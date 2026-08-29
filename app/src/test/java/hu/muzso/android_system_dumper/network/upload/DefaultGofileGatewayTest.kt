package hu.muzso.android_system_dumper.network.upload

import android.content.Context
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.DefaultNetworkUtils
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.InputStream
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGofileGatewayTest {

    private lateinit var server: MockWebServer
    private lateinit var gateway: DefaultGofileGateway
    private lateinit var context: Context
    private val logger = FakeFileLogger()
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var dispatcherProvider: FakeDispatcherProvider
    private lateinit var fileSystem: FakeJvmFileSystem
    private val torChecker = mockk<TorChecker>(relaxed = true)

    private lateinit var clock: FakeClock
    private lateinit var httpClientProvider: HttpClientProvider
    private val networkUtils = DefaultNetworkUtils()

    @Before
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        dispatcherProvider = FakeDispatcherProvider(testDispatcher)
        fileSystem = FakeJvmFileSystem(dispatcherProvider)
        server = MockWebServer()
        context = mockk<Context>(relaxed = true)
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())

        val testClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        httpClientProvider = mockk<HttpClientProvider>(relaxed = true)
        every { httpClientProvider.getClient() } returns testClient

        clock = FakeClock()

        gateway = DefaultGofileGateway(
            context,
            retrofitBuilder,
            httpClientProvider,
            clock,
            logger,
            fileSystem,
            torChecker,
            networkUtils
        )
        gateway.setBaseUrl(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `upload success returns Success result with mapped data`() = runTest(testDispatcher) {
        val jsonResponse = """
            {
                "status": "ok",
                "data": {
                    "downloadPage": "https://gofile.io/d/test",
                    "guestToken": "gt123",
                    "parentFolder": "pf456"
                }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(jsonResponse))

        val file = fileSystem.addFileWithText("test.txt", "test content")

        val results = gateway.upload(file, "test.txt", null, null).toList()

        assertThat(results.any { it is GatewayResult.Progress }).isTrue()
        val lastResult = results.last() as GatewayResult.Success
        assertThat(lastResult.data.downloadPage).isEqualTo("https://gofile.io/d/test")
        assertThat(lastResult.data.guestToken).isEqualTo("gt123")
        assertThat(lastResult.data.parentFolder).isEqualTo("pf456")

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/uploadfile")
        assertThat(recordedRequest.method).isEqualTo("POST")
    }

    @Test
    fun `upload error returns Error result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        every { context.getString(any(), 500, any()) } returns "HTTP Error 500"

        val file = fileSystem.addFileWithText("test.txt", "content")

        val results = gateway.upload(file, "test.txt", null, null).toList()

        assertThat(results.last()).isInstanceOf(GatewayResult.Error::class.java)
        logger.assertErrorLogExists("DefaultGofileGateway", "HTTP Error 500")
    }

    @Test
    fun `upload with 429 returns Error result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(429))
        every { context.getString(any(), 429, any()) } returns "HTTP Error 429"

        val file = fileSystem.addFileWithText("test.txt", "content")

        val results = gateway.upload(file, "test.txt", null, null).toList()

        assertThat(results.last()).isInstanceOf(GatewayResult.Error::class.java)
        logger.assertErrorLogExists("DefaultGofileGateway", "HTTP Error 429")
    }

    @Test
    fun `upload with malformed JSON returns Error result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setBody("invalid json"))
        every { context.getString(any(), any()) } returns "JSON Error"

        val file = fileSystem.addFileWithText("test.txt", "content")

        val results = gateway.upload(file, "test.txt", null, null).toList()

        assertThat(results.last()).isInstanceOf(GatewayResult.Error::class.java)
        logger.assertErrorLogExists("DefaultGofileGateway", "JSON Error")
    }

    @Test
    fun `upload timeout returns Error result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        every { context.getString(any(), any()) } returns "Timeout Error"

        val file = fileSystem.addFileWithText("test.txt", "content")

        val results = gateway.upload(file, "test.txt", null, null).toList()

        assertThat(results.last()).isInstanceOf(GatewayResult.Error::class.java)
        logger.assertErrorLogExists("DefaultGofileGateway", "Timeout Error")
    }

    @Test
    fun `upload emits multiple progress updates`() = runTest(testDispatcher) {
        val jsonResponse = """{"status": "ok", "data": {"downloadPage": "..."}}"""
        server.enqueue(MockResponse().setBody(jsonResponse))

        // Create a file large enough to trigger multiple updates
        val content = "A".repeat(1024 * 1024)
        val file = fileSystem.addFileWithText("large.txt", content)

        val results = mutableListOf<GatewayResult<*>>()
        
        // We use a custom stream that ticks the clock to ensure progress updates are triggered
        // during the fast in-memory upload in the test.
        val gatewayWithTick = DefaultGofileGateway(
            context, Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create()),
            httpClientProvider, clock, logger, object : FileSystem by fileSystem {
                override suspend fun openInputStream(path: String): InputStream {
                    val original = fileSystem.openInputStream(path)
                    return object : InputStream() {
                        override fun read(): Int = original.read()
                        override fun read(b: ByteArray, off: Int, len: Int): Int {
                            val r = original.read(b, off, len)
                            if (r > 0) clock.tick(100) // Advance clock as we read
                            return r
                        }

                        override fun close() = original.close()
                    }
                }
            },
            torChecker,
            networkUtils
        )
        gatewayWithTick.setBaseUrl(server.url("/").toString())

        gatewayWithTick.upload(file, "large.txt", null, null).collect {
            results.add(it)
        }

        val progressUpdates = results.filterIsInstance<GatewayResult.Progress>()
        assertThat(progressUpdates.size).isAtLeast(2)
        val lastProgress = progressUpdates.last()
        assertThat(lastProgress.bytesWritten).isEqualTo(lastProgress.totalBytes)
    }

    @Test
    fun `upload cancellation stops process and throws CancellationException`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val file = fileSystem.addFileWithText("cancel.txt", "content")

        val results = mutableListOf<GatewayResult<*>>()
        var caughtCancellation = false
        val job = launch {
            try {
                gateway.upload(file, "cancel.txt", null, null).collect {
                    results.add(it)
                }
            } catch (_: CancellationException) {
                caughtCancellation = true
            }
        }
        
        // Wait for it to start and emit progress
        while (results.isEmpty()) {
            yield()
        }
        
        job.cancelAndJoin()
        
        assertThat(caughtCancellation).isTrue()
        assertThat(results.last()).isNotInstanceOf(GatewayResult.Success::class.java)
    }

    @Test
    fun `torCheck delegates to TorChecker`() = runTest(testDispatcher) {
        coEvery { torChecker.check() } returns true
        
        val result = gateway.torCheck()
        
        assertThat(result).isTrue()
        coVerify { torChecker.check() }
    }
}
