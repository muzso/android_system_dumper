package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A centralized provider for [OkHttpClient] instances, ensuring consistent configuration
 * across the application.
 *
 * This provider manages the global [Proxy] settings and applies them to the returned clients.
 */
@Singleton
class HttpClientProvider @Inject constructor(
    appConfig: AppConfig
) {
    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(appConfig.networkTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(appConfig.networkTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(appConfig.networkTimeoutMs, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private var currentProxy: Proxy? = null
    
    private val _clientFlow = MutableStateFlow(baseClient)
    val clientFlow: StateFlow<OkHttpClient> = _clientFlow.asStateFlow()

    /**
     * Updates the global proxy configuration and rebuilds the shared client if necessary.
     *
     * @param proxy The new proxy to use, or null to disable proxy.
     */
    @Synchronized
    fun setProxy(proxy: Proxy?) {
        if (currentProxy == proxy) return
        
        currentProxy = proxy
        val newClient = if (proxy != null) {
            baseClient.newBuilder()
                .proxy(proxy)
                .build()
        } else {
            baseClient
        }
        _clientFlow.value = newClient
    }

    /**
     * Returns the shared [OkHttpClient] instance configured with the current global proxy.
     *
     * @return The shared [OkHttpClient].
     */
    fun getClient(): OkHttpClient = _clientFlow.value
}
