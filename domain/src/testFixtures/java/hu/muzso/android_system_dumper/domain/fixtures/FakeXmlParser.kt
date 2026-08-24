package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.platform.XmlParser
import java.io.InputStream

class FakeXmlParser(
    private var noticeEntries: List<String> = emptyList(),
    private var permissionsEntries: List<String> = emptyList()
) : XmlParser {

    fun setEntries(newEntries: List<String>) {
        noticeEntries = newEntries
    }

    fun setPermissionsEntries(newEntries: List<String>) {
        permissionsEntries = newEntries
    }

    override suspend fun parseNoticeXml(inputStream: InputStream, onEntry: suspend (String) -> Unit) {
        noticeEntries.forEach { onEntry(it) }
    }

    override suspend fun parsePermissionsXml(inputStream: InputStream, onEntry: suspend (String) -> Unit) {
        permissionsEntries.forEach { onEntry(it) }
    }
}
