package hu.muzso.android_system_dumper.repository

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import hu.muzso.android_system_dumper.common.DefaultNetworkUtils
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.network.upload.HttpClientProvider
import hu.muzso.android_system_dumper.network.upload.TorChecker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultIpInfoRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultIpInfoRepository
    private val moshi = Moshi.Builder().build()
    private val torChecker = mockk<TorChecker>()
    private val httpClientProvider = mockk<HttpClientProvider>()
    private val okHttpClient = OkHttpClient()
    private val networkUtils = DefaultNetworkUtils()
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var dispatcherProvider: FakeDispatcherProvider

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        dispatcherProvider = FakeDispatcherProvider(testDispatcher)
        server = MockWebServer()
        server.start()
        
        every { httpClientProvider.getClient() } returns okHttpClient
        coEvery { torChecker.check() } returns false
        
        repository = DefaultIpInfoRepository(httpClientProvider, moshi, torChecker, networkUtils, dispatcherProvider)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchIpInfo returns structured data with human readable keys`() = runTest(testDispatcher) {
        val jsonResponse = """
            {
                "ip": "1.2.3.4",
                "success": true,
                "city": "Budapest",
                "connection": {
                    "asn": 12345,
                    "isp": "Test ISP"
                }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(jsonResponse))
        
        // We need to inject the test URL into the repository. 
        // Since it's hardcoded in the impl, we'll use reflection or just assume it works 
        // if we could point it to localhost.
        // Actually, DefaultIpInfoRepository has a private urls list.
        // Let's modify the repository to allow setting base URLs for testing or use a different approach.
        // For now, I'll just use reflection to swap the URLs for the test.
        
        val urlsField = repository.javaClass.getDeclaredField("availableSources")
        urlsField.isAccessible = true
        urlsField.set(repository, listOf(server.url("/").toString()))

        val result = repository.fetchIpInfo()
        
        assertThat(result.isSuccess).isTrue()
        val ipInfo = result.getOrThrow()
        
        assertThat(ipInfo.sourceUrl).isEqualTo(server.url("/").toString())
        val data = ipInfo.data
        
        assertThat(data["Ip"]).isEqualTo("1.2.3.4")
        assertThat(data["City"]).isEqualTo("Budapest")
        assertThat(data["Is Tor Node"]).isEqualTo(false)
        
        val connection = data["Connection"] as Map<*, *>
        assertThat(connection["Asn"]).isEqualTo(12345L) // Verified integer fix
        assertThat(connection["Isp"]).isEqualTo("Test ISP")
    }

    @Test
    fun `fetchIpInfo handles failures and tries next source`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setBody("""{"ip":"8.8.8.8","success":true}"""))

        val urlsField = repository.javaClass.getDeclaredField("availableSources")
        urlsField.isAccessible = true
        val testUrl = server.url("/").toString()
        urlsField.set(repository, listOf(testUrl, testUrl))

        val result = repository.fetchIpInfo()
        
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().data["Ip"]).isEqualTo("8.8.8.8")
    }

    @Test
    fun `getAvailableSources returns the list of sources`() {
        val sources = repository.getAvailableSources()
        assertThat(sources).isNotEmpty()
        assertThat(sources).contains("https://ipwho.is/")
    }
}
