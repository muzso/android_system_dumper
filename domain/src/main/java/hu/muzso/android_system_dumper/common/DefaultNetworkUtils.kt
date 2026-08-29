package hu.muzso.android_system_dumper.common

import org.apache.commons.validator.routines.DomainValidator
import org.apache.commons.validator.routines.InetAddressValidator
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNetworkUtils @Inject constructor() : NetworkUtils {

    /**
     * Parses a proxy specification string and returns a [Proxy] object.
     *
     * The specification should be in the format `[[type://]host:]port`.
     * `host` can be a hostname or an IP address, the default value being `127.0.0.1`.
     * Proxy type can be `http` or `socks`, the latter being the default.
     * If the specification is invalid or empty, this method returns null.
     *
     * @param specification The proxy specification string.
     * @return A [Proxy] object, or null if the specification is invalid.
     */
    override fun getProxyFromSpecification(specification: String): Proxy? {
        if (specification.isEmpty()) return null

        var proxy = specification.trim()

        val proxyType = when {
            proxy.startsWith("http://") -> {
                proxy = proxy.substring(7)
                Proxy.Type.HTTP
            }
            proxy.startsWith("socks://") -> {
                proxy = proxy.substring(8)
                Proxy.Type.SOCKS
            }
            else -> Proxy.Type.SOCKS
        }

        var proxyPort = proxy.toIntOrNull()
        var proxyHost = "127.0.0.1"

        if (proxyPort == null) {
            val proxyParts = proxy.split(":")
            if (proxyParts.isEmpty() || proxyParts.size > 2) {
                return null
            } else if (proxyParts.size == 2) {
                proxyHost = proxyParts[0]
                proxyPort = proxyParts[1].toIntOrNull()
            } else {
                proxyHost = proxy
            }
        }

        if (proxyPort == null) {
            return null
        }

        if (!InetAddressValidator.getInstance().isValid(proxyHost) && !DomainValidator.getInstance(true).isValid(proxyHost)) {
            return null
        }

        return Proxy(proxyType, InetSocketAddress.createUnresolved(proxyHost, proxyPort))
    }

    override fun httpErrorMessage(code: Int): String =
        when (code) {
            400 -> "Bad Request"
            401 -> "Unauthorized"
            402 -> "Payment Required"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            406 -> "Not Acceptable"
            407 -> "Proxy Authentication Required"
            408 -> "Request Timeout"
            409 -> "Conflict"
            410 -> "Gone"
            411 -> "Length Required"
            412 -> "Precondition Failed"
            413 -> "Content Too Large"
            414 -> "URI Too Long"
            415 -> "Unsupported Media Type"
            416 -> "Range Not Satisfiable"
            417 -> "Expectation Failed"
            418 -> "I'm a teapot"
            421 -> "Misdirected Request"
            422 -> "Unprocessable Content"
            423 -> "Locked"
            424 -> "Failed Dependency"
            425 -> "Too Early"
            426 -> "Upgrade Required"
            428 -> "Precondition Required"
            429 -> "Too Many Requests"
            431 -> "Request Header Fields Too Large"
            451 -> "Unavailable For Legal Reasons"
            500 -> "Internal Server Error"
            501 -> "Not Implemented"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"
            505 -> "HTTP Version Not Supported"
            506 -> "Variant Also Negotiates"
            507 -> "Insufficient Storage"
            508 -> "Loop Detected"
            510 -> "Not Extended"
            511 -> "Network Authentication Required"
            else -> "Unknown"
        }

    override fun getLocalIPv4Addresses(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        ips.add(address.hostAddress ?: "")
                    }
                }
            }
        } catch (e: Exception) {
            // Log or handle error if needed, for now return what we have
        }
        return ips.filter { it.isNotEmpty() }.distinct().sorted()
    }
}
