package hu.muzso.android_system_dumper.common

import java.net.Proxy

interface NetworkUtils {
    fun getProxyFromSpecification(specification: String): Proxy?

    fun httpErrorMessage(code: Int): String

    /**
     * Returns a list of all local IPv4 addresses assigned to the device's network interfaces,
     * excluding loopback (127.0.0.0/8) addresses.
     */
    fun getLocalIPv4Addresses(): List<String>
}
