package hu.muzso.android_system_dumper.common

import java.net.Proxy

interface NetworkUtils {
    fun getProxyFromSpecification(specification: String): Proxy?

    fun httpErrorMessage(code: Int): String
}
