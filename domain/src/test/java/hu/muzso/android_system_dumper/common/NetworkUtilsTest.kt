package hu.muzso.android_system_dumper.common

import com.google.common.truth.Truth
import org.junit.Test
import java.net.Proxy

class NetworkUtilsTest {

    private val networkUtils = DefaultNetworkUtils()

    @Test
    fun `getProxyFromSpecification returns null for empty string`() {
        Truth.assertThat(networkUtils.getProxyFromSpecification("")).isNull()
    }

    @Test
    fun `getProxyFromSpecification parses http proxy`() {
        val proxy = networkUtils.getProxyFromSpecification("http://1.2.3.4:8080")
        Truth.assertThat(proxy?.type()).isEqualTo(Proxy.Type.HTTP)
        Truth.assertThat(proxy?.address().toString()).contains("1.2.3.4")
        Truth.assertThat(proxy?.address().toString()).contains("8080")
    }

    @Test
    fun `getProxyFromSpecification parses socks proxy`() {
        val proxy = networkUtils.getProxyFromSpecification("socks://myproxy.com:1080")
        Truth.assertThat(proxy?.type()).isEqualTo(Proxy.Type.SOCKS)
        Truth.assertThat(proxy?.address().toString()).contains("myproxy.com")
        Truth.assertThat(proxy?.address().toString()).contains("1080")
    }

    @Test
    fun `getProxyFromSpecification defaults to socks`() {
        val proxy = networkUtils.getProxyFromSpecification("1.2.3.4:1080")
        Truth.assertThat(proxy?.type()).isEqualTo(Proxy.Type.SOCKS)
    }

    @Test
    fun `getProxyFromSpecification returns null for invalid format`() {
        Truth.assertThat(networkUtils.getProxyFromSpecification("invalid-proxy")).isNull()
        Truth.assertThat(networkUtils.getProxyFromSpecification("host:invalid")).isNull()
        Truth.assertThat(networkUtils.getProxyFromSpecification("http://host:port:extra")).isNull()
    }

    @Test
    fun `httpErrorMessage returns correct message for all categories`() {
        val testCodes = listOf(
            400 to "Bad Request",
            401 to "Unauthorized",
            403 to "Forbidden",
            404 to "Not Found",
            418 to "I'm a teapot",
            429 to "Too Many Requests",
            500 to "Internal Server Error",
            502 to "Bad Gateway",
            503 to "Service Unavailable",
            504 to "Gateway Timeout",
            507 to "Insufficient Storage",
            999 to "Unknown"
        )
        for ((code, message) in testCodes) {
            Truth.assertThat(networkUtils.httpErrorMessage(code)).isEqualTo(message)
        }
    }
}