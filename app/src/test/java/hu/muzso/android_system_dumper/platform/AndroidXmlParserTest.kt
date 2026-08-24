package hu.muzso.android_system_dumper.platform

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidXmlParserTest {

    private val parser = AndroidXmlParser()

    @Test
    fun `parseNoticeXml extracts file names correctly`() = runTest {
        val xml = """
            <notice>
                <file-name>/etc/test1.txt</file-name>
                <file-name>  /etc/test2.txt  </file-name>
                <file-name>/etc//test3.txt</file-name>
                <not-file-name>/etc/test4.txt</not-file-name>
                <file-name>not/absolute/path</file-name>
            </notice>
        """.trimIndent()
        
        val foundPaths = mutableListOf<String>()
        parser.parseNoticeXml(ByteArrayInputStream(xml.toByteArray())) {
            foundPaths.add(it)
        }

        assertEquals(
            listOf("/etc/test1.txt", "/etc/test2.txt", "/etc/test3.txt"),
            foundPaths
        )
    }

    @Test
    fun `parseNoticeXml handles empty input`() = runTest {
        val xml = "<notice></notice>"
        val foundPaths = mutableListOf<String>()
        parser.parseNoticeXml(ByteArrayInputStream(xml.toByteArray())) {
            foundPaths.add(it)
        }
        assertEquals(0, foundPaths.size)
    }

    @Test
    fun `parsePermissionsXml extracts library files correctly`() = runTest {
        val xml = """
            <permissions>
                <library name="android.test.runner" file="/system/framework/android.test.runner.jar" />
                <library name="android.test.mock" file="  /system/framework/android.test.mock.jar  " />
                <library name="no.file.attr" />
                <not-library name="test" file="/system/framework/test.jar" />
                <other-tag>
                    <library name="nested" file="/system/framework/nested.jar" />
                </other-tag>
            </permissions>
        """.trimIndent()

        val foundPaths = mutableListOf<String>()
        parser.parsePermissionsXml(ByteArrayInputStream(xml.toByteArray())) {
            foundPaths.add(it)
        }

        assertEquals(
            listOf(
                "/system/framework/android.test.runner.jar",
                "/system/framework/android.test.mock.jar",
                "/system/framework/nested.jar"
            ),
            foundPaths
        )
    }

    @Test
    fun `parsePermissionsXml ignores library outside permissions tag`() = runTest {
        val xml = """
            <other>
                <library name="test" file="/system/framework/test.jar" />
            </other>
        """.trimIndent()

        val foundPaths = mutableListOf<String>()
        parser.parsePermissionsXml(ByteArrayInputStream(xml.toByteArray())) {
            foundPaths.add(it)
        }

        assertEquals(0, foundPaths.size)
    }
}
