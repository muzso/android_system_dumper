package hu.muzso.android_system_dumper.network.upload

import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Generates a human-readable summary of the [OkHttpClient]'s current configuration.
 * 
 * This includes timeouts, proxy settings (including detailed address info), 
 * redirect behavior, and supported protocols. Useful for diagnostics and logging.
 *
 * @return A formatted string containing the client configuration.
 */
fun OkHttpClient.configurationToString(): String {
    // Format proxy information safely
    val proxyLog = when (val p = this.proxy) {
        null -> "None set (using default ProxySelector: ${this.proxySelector})"
        Proxy.NO_PROXY -> "Explicitly set to NO_PROXY"
        else -> {
            val address = p.address()
            if (address is InetSocketAddress) {
                "Type: ${p.type()}, Host: ${address.hostName}, Port: ${address.port}"
            } else {
                "Type: ${p.type()}, Address: $address"
            }
        }
    }

    val configLog = """
    === OkHttpClient Configuration ===
    Connect Timeout:       ${this.connectTimeoutMillis} ms
    Read Timeout:          ${this.readTimeoutMillis} ms
    Write Timeout:         ${this.writeTimeoutMillis} ms
    Call Timeout:          ${this.callTimeoutMillis} ms
    Proxy SettingsRepository:        $proxyLog
    Follow Redirects:      ${this.followRedirects}
    Follow SSL Redirects:  ${this.followSslRedirects}
    Retry on Failure:      ${this.retryOnConnectionFailure}
    Supported Protocols:   ${this.protocols.joinToString(", ")}
    ==================================
""".trimIndent()

    return configLog
}
