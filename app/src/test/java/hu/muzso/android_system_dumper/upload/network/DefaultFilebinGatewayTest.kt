package hu.muzso.android_system_dumper.upload.network

import android.content.Context
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.DefaultNetworkUtils
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.upload.network.gateway.GatewayResult
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
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFilebinGatewayTest {

    private lateinit var server: MockWebServer
    private lateinit var gateway: DefaultFilebinGateway
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

        gateway = DefaultFilebinGateway(
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
    fun `upload success returns Success result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(201))

        val file = fileSystem.addFileWithText("test.txt", "test content")

        val results = gateway.upload("bin123", "test.txt", file).toList()

        assertThat(results.any { it is GatewayResult.Progress }).isTrue()
        assertThat(results.last()).isInstanceOf(GatewayResult.Success::class.java)

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/bin123/test.txt")
        assertThat(recordedRequest.method).isEqualTo("POST")
    }

    @Test
    fun `upload error returns Error result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        every { context.getString(any(), 500, any()) } returns "HTTP Error 500"

        val file = fileSystem.addFileWithText("test.txt", "content")

        val results = gateway.upload("bin123", "test.txt", file).toList()

        assertThat(results.last()).isInstanceOf(GatewayResult.Error::class.java)
        logger.assertErrorLogExists("DefaultFilebinGateway", "HTTP Error 500")
    }

    @Test
    fun `upload with 429 returns Error result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(429))
        every { context.getString(any(), 429, any()) } returns "HTTP Error 429"

        val file = fileSystem.addFileWithText("test.txt", "content")

        val results = gateway.upload("bin123", "test.txt", file).toList()

        assertThat(results.last()).isInstanceOf(GatewayResult.Error::class.java)
        logger.assertErrorLogExists("DefaultFilebinGateway", "HTTP Error 429")
    }

    @Test
    fun `upload timeout returns Error result`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        every { context.getString(any(), any()) } returns "Timeout Error"

        val file = fileSystem.addFileWithText("test.txt", "content")

        val results = gateway.upload("bin123", "test.txt", file).toList()

        assertThat(results.last()).isInstanceOf(GatewayResult.Error::class.java)
        logger.assertErrorLogExists("DefaultFilebinGateway", "Timeout Error")
    }

    @Test
    fun `upload emits multiple progress updates`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(201))

        val content = "A".repeat(1024 * 1024)
        val file = fileSystem.addFileWithText("large.txt", content)

        val results = mutableListOf<GatewayResult<*>>()
        
        val gatewayWithTick = DefaultFilebinGateway(
            context, Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create()),
            httpClientProvider, clock, logger, object : FileSystem by fileSystem {
                override suspend fun openInputStream(path: String): InputStream {
                    val original = fileSystem.openInputStream(path)
                    return object : InputStream() {
                        override fun read(): Int = original.read()
                        override fun read(b: ByteArray, off: Int, len: Int): Int {
                            val r = original.read(b, off, len)
                            if (r > 0) clock.tick(100)
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

        gatewayWithTick.upload("bin123", "large.txt", file).collect {
            results.add(it)
        }

        val progressUpdates = results.filterIsInstance<GatewayResult.Progress>()
        assertThat(progressUpdates.size).isAtLeast(2)
        val lastProgress = progressUpdates.last()
        assertThat(lastProgress.bytesWritten).isEqualTo(lastProgress.totalBytes)
        assertThat(lastProgress.bytesWritten).isEqualTo(fileSystem.size(file))
    }

    @Test
    fun `upload cancellation stops process and throws CancellationException`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(201).setBodyDelay(1, TimeUnit.SECONDS))

        val file = fileSystem.addFileWithText("cancel.txt", "content")

        val results = mutableListOf<GatewayResult<*>>()
        var caughtCancellation = false
        val job = launch {
            try {
                gateway.upload("bin123", "cancel.txt", file).collect {
                    results.add(it)
                }
            } catch (_: CancellationException) {
                caughtCancellation = true
            }
        }
        
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

    @Test
    fun `logConfiguration logs proxy information`() = runTest {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("1.2.3.4", 8080))
        val clientWithProxy = OkHttpClient.Builder()
            .proxy(proxy)
            .build()
        every { httpClientProvider.getClient() } returns clientWithProxy

        gateway.logConfiguration()

        logger.assertLogExists("I", "DefaultFilebinGateway", "OkHttpClient Configuration")
        logger.assertLogExists("I", "DefaultFilebinGateway", "Host: 1.2.3.4")
        logger.assertLogExists("I", "DefaultFilebinGateway", "Port: 8080")
    }
}
