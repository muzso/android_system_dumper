package hu.muzso.android_system_dumper.upload.network

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TorCheckerTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private val logger = FakeFileLogger()
    private lateinit var httpClientProvider: HttpClientProvider
    private lateinit var retrofitBuilder: Retrofit.Builder

    @Before
    fun setup() {
        server = MockWebServer()
        context = mockk<Context>(relaxed = true)
        httpClientProvider = HttpClientProvider()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        retrofitBuilder = Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))

        // Mocking the string resource for error messages
        every { context.getString(R.string.http_error_from, any(), any()) } returns "HTTP Error"
        every { context.getString(R.string.error_processing_response, any()) } returns "Processing Error"
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `torCheck returns true when IsTor is true`() = runTest {
        val torChecker = TorChecker(context, logger, retrofitBuilder, httpClientProvider)
        torChecker.setTorCheckUrl(server.url("/api/ip").toString())
        server.enqueue(MockResponse().setBody("""{"IsTor": true}"""))
        
        val result = torChecker.check()
        
        assertThat(result).isTrue()
        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/api/ip")
    }

    @Test
    fun `torCheck returns false when IsTor is false`() = runTest {
        val torChecker = TorChecker(context, logger, retrofitBuilder, httpClientProvider)
        torChecker.setTorCheckUrl(server.url("/api/ip").toString())
        server.enqueue(MockResponse().setBody("""{"IsTor": false}"""))
        
        val result = torChecker.check()
        
        assertThat(result).isFalse()
    }

    @Test
    fun `torCheck throws IOException when HTTP error occurs`() = runTest {
        val torChecker = TorChecker(context, logger, retrofitBuilder, httpClientProvider)
        torChecker.setTorCheckUrl(server.url("/api/ip").toString())
        server.enqueue(MockResponse().setResponseCode(500))
        
        try {
            torChecker.check()
            Assert.fail("Should have thrown IOException")
        } catch (_: IOException) {
            // Success
        }
    }

    @Test
    fun `torCheck throws IOException when JSON is malformed`() = runTest {
        val torChecker = TorChecker(context, logger, retrofitBuilder, httpClientProvider)
        torChecker.setTorCheckUrl(server.url("/api/ip").toString())
        server.enqueue(MockResponse().setBody("""{"invalid": "json"}"""))
        
        try {
            torChecker.check()
            Assert.fail("Should have thrown IOException")
        } catch (_: IOException) {
            // Success
        }
    }

    @Test
    fun `torCheck throws IOException on network failure`() = runTest {
        val torChecker = TorChecker(context, logger, retrofitBuilder, httpClientProvider)
        torChecker.setTorCheckUrl(server.url("/api/ip").toString())
        server.shutdown() // Simulate network failure
        
        try {
            torChecker.check()
            Assert.fail("Should have thrown IOException")
        } catch (_: IOException) {
            // Success
        }
    }
}
