package hu.muzso.android_system_dumper.upload.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.logging.FileLogger
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: FileLogger,
    private val retrofitBuilder: Retrofit.Builder,
    private val httpClientProvider: HttpClientProvider
) {
    companion object {
        private const val TAG = "TorChecker"
        private const val DEFAULT_TOR_CHECK_URL = "https://check.torproject.org/api/ip"
    }

    private var torCheckUrl = DEFAULT_TOR_CHECK_URL
    private var cachedApi: TorCheckerApi? = null
    private var lastUsedClient: OkHttpClient? = null

    fun setTorCheckUrl(url: String) {
        torCheckUrl = url
    }

    /**
     * Checks if the traffic is routed through the Tor network.
     * 
     * This method uses Retrofit to call a Tor check service (by default check.torproject.org).
     * It handles network errors and parses the JSON response to determine the Tor status.
     *
     * @return True if the check confirms traffic is through Tor, false otherwise.
     * @throws IOException If the network request fails or the response cannot be parsed.
     */
    suspend fun check(): Boolean {
        val client = httpClientProvider.getClient()
        
        val api = if (client === lastUsedClient && cachedApi != null) {
            cachedApi!!
        } else {
            val newApi = retrofitBuilder
                .baseUrl("http://127.0.0.1:12345/") // Dummy base, @Url overrides it
                .client(client)
                .build()
                .create(TorCheckerApi::class.java)
            cachedApi = newApi
            lastUsedClient = client
            newApi
        }

        return try {
            val response = api.checkTor(torCheckUrl)
            val isTor = response.isTor
            logger.i(TAG, "Tor check: ${if (isTor) "success" else "failure"}")
            isTor
        } catch (e: Exception) {
            val msg = context.getString(R.string.error_processing_response, e.message ?: "Network error")
            logger.e(TAG, "Error during Tor check: ${e.message}", e)
            throw IOException(msg, e)
        }
    }
}
