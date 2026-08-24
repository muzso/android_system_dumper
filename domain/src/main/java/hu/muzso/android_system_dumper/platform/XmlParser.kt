package hu.muzso.android_system_dumper.platform

import java.io.InputStream

interface XmlParser {
    suspend fun parseNoticeXml(inputStream: InputStream, onEntry: suspend (String) -> Unit)
    suspend fun parsePermissionsXml(inputStream: InputStream, onEntry: suspend (String) -> Unit)
}
