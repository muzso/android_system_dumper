package hu.muzso.android_system_dumper.platform

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidXmlParser @Inject constructor() : XmlParser {
    /**
     * Parses a notice.xml file from stream and reports file path entries found within.
     * 
     * This method uses [XmlPullParser] to find all `<file-name>` tags,
     * normalizes the paths by removing duplicate slashes, and invokes the
     * [onEntry] callback for each valid path found.
     *
     * @param inputStream The input stream containing the notice XML.
     * @param onEntry A suspending callback invoked for each file path found.
     */
    override suspend fun parseNoticeXml(inputStream: InputStream, onEntry: suspend (String) -> Unit) {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        val forwardSlashRegex = Regex("/+")
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "file-name") {
                val innerPath = parser.nextText()?.trim()
                if (!innerPath.isNullOrEmpty() && innerPath[0] == '/') {
                    val processedPath = innerPath.replace(forwardSlashRegex, "/")
                    onEntry(processedPath)
                }
            }
            eventType = parser.next()
        }
    }

    /**
     * Parses a permissions XML file from stream and reports file path entries from <library> tags.
     *
     * This method looks for <permissions> tags, and then for <library> tags directly under them.
     * It extracts the "file" attribute from each <library> tag and invokes the [onEntry] callback.
     *
     * @param inputStream The input stream containing the permissions XML.
     * @param onEntry A suspending callback invoked for each file path found.
     */
    override suspend fun parsePermissionsXml(inputStream: InputStream, onEntry: suspend (String) -> Unit) {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        var inPermissions = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "permissions" -> inPermissions = true
                        "library" -> {
                            if (inPermissions) {
                                val filePath = parser.getAttributeValue(null, "file")?.trim()
                                if (!filePath.isNullOrBlank()) {
                                    onEntry(filePath)
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "permissions") {
                        inPermissions = false
                    }
                }
            }
            eventType = parser.next()
        }
    }
}
