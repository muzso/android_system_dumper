package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.platform.XmlParser
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class DefaultMetadataCollectorTest {

    private val fileSystem = FakeMemoryFileSystem()
    private val xmlParser = mockk<XmlParser>()
    private val logger = mockk<FileLogger>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val selinuxAnalyzer = SelinuxContextAnalyzer()
    private val getSeedPathsUseCase = GetSeedPathsUseCase()
    private lateinit var collector: DefaultMetadataCollector

    @Before
    fun setup() {
        collector = DefaultMetadataCollector(
            fileSystem, xmlParser, logger, dispatcherProvider, selinuxAnalyzer, getSeedPathsUseCase
        )
    }

    @Test
    fun `isMetadataFile recognizes notice files`() {
        assertTrue(collector.isMetadataFile("/etc/notice.xml"))
        assertTrue(collector.isMetadataFile("/etc/notice.xml.gz"))
        assertTrue(collector.isMetadataFile("/etc/selinux/plat_file_contexts"))
        assertTrue(collector.isMetadataFile("/vendor/etc/selinux/vendor_file_contexts"))
        assertTrue(collector.isMetadataFile("/init.rc"))
        assertTrue(collector.isMetadataFile("/vendor/etc/init/hw/init.common.rc"))
        assertTrue(collector.isMetadataFile("/system/etc/public.libraries.txt"))
        assertTrue(collector.isMetadataFile("public.libraries.android.txt"))
        assertTrue(collector.isMetadataFile("/system/etc/linker.config.pb"))
        assertTrue(collector.isMetadataFile("/system/etc/classpaths/bootclasspath.pb"))
        assertTrue(collector.isMetadataFile("systemserverclasspath.pb"))
        assertTrue(collector.isMetadataFile("/system/etc/permissions/privapp-permissions-platform.xml"))
        assertTrue(collector.isMetadataFile("/vendor/etc/sysconfig/component-override.xml"))
        assertTrue(collector.isMetadataFile("/vendor/lib/modules/modules.dep"))
        assertTrue(collector.isMetadataFile("/vendor/lib/modules/modules.load"))
        assertTrue(!collector.isMetadataFile("/etc/other.txt"))
        assertTrue(!collector.isMetadataFile("/system/etc/other_dir/file.xml"))
    }

    @Test
    fun `processMetadata ignores non-notice files`() = runTest(testDispatcher) {
        val path = "/etc/not_a_notice.txt"
        fileSystem.writeText(path, "some content")

        collector.processMetadata(path) { _, _ -> }

        coVerify(exactly = 0) { xmlParser.parseNoticeXml(any(), any()) }
    }

    @Test
    fun `processMetadata parses notice xml`() = runTest(testDispatcher) {
        val path = "/etc/notice.xml"
        fileSystem.writeText(path, "<notice></notice>")
        coEvery { xmlParser.parseNoticeXml(any(), any()) } coAnswers {
            val onEntry = secondArg<suspend (String) -> Unit>()
            onEntry("/path/to/file")
        }

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        assertEquals(listOf("/path/to/file"), foundPaths)
        assertEquals(listOf("notice.xml analysis of $path"), sources)
        coVerify { xmlParser.parseNoticeXml(any(), any()) }
    }

    @Test
    fun `processMetadata parses gzipped notice xml`() = runTest(testDispatcher) {
        val path = "/etc/notice.xml.gz"
        val content = "<notice></notice>".toByteArray()
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(content) }
        val gzippedBytes = bos.toByteArray()
        
        // Mocking openInputStream because FakeMemoryFileSystem might not handle binary data well
        val mockFileSystem = mockk<FileSystem>()
        val collectorWithMock =
            DefaultMetadataCollector(
                mockFileSystem,
                xmlParser,
                logger,
                dispatcherProvider,
                selinuxAnalyzer,
                getSeedPathsUseCase
            )
        
        coEvery { mockFileSystem.openInputStream(path) } returns ByteArrayInputStream(gzippedBytes)

        coEvery { xmlParser.parseNoticeXml(any(), any()) } coAnswers {
            val onEntry = secondArg<suspend (String) -> Unit>()
            onEntry("/path/to/gz/file")
        }

        val foundPaths = mutableListOf<String>()
        collectorWithMock.processMetadata(path) { innerPath, _ -> foundPaths.add(innerPath) }

        assertEquals(listOf("/path/to/gz/file"), foundPaths)
    }

    @Test
    fun `processMetadata logs error on exception`() = runTest(testDispatcher) {
        val path = "/etc/notice.xml"
        fileSystem.writeText(path, "invalid content")
        coEvery { xmlParser.parseNoticeXml(any(), any()) } throws RuntimeException("Parsing failed")

        collector.processMetadata(path) { _, _ -> }

        coVerify { logger.d("DefaultMetadataCollector", match { it.contains("An exception occurred during parsing") }) }
    }

    @Test
    fun `processMetadata parses selinux file contexts`() = runTest(testDispatcher) {
        val path = "/etc/selinux/plat_file_contexts"
        val content = """
            # comment
            /system/bin/sh  u:object_r:shell_exec:s0
            /vendor/lib(64)? u:object_r:vendor_file:s0
        """.trimIndent()
        fileSystem.writeText(path, content)

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        val expectedPaths = listOf("/system/bin/sh", "/vendor/lib", "/vendor/lib64")
        assertEquals(expectedPaths.sorted(), foundPaths.sorted())
        assertEquals(expectedPaths.size, sources.filter { it == "selinux file context analysis of $path" }.size)
    }

    @Test
    fun `processMetadata parses rc file`() = runTest(testDispatcher) {
        val path = "/init.rc"
        val content = """
            import /init.environ.rc
            
            on early-init
                mkdir /dev/socket 0755 system system
                
            service console /system/bin/sh
                class core
                user shell
                group shell log readproc
                
            # Random characters to filter: ! @ % ^ * ( )
            on post-fs-data
                exec /system/bin/my_tool --param=value!
                write /proc/sys/kernel/panic 1
        """.trimIndent()
        fileSystem.writeText(path, content)

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        // Expected tokens:
        // /init.environ.rc (starts with /)
        // /dev/socket (starts with /)
        // /system/bin/sh (starts with /system)
        // /system/bin/my_tool (starts with /system, '!' filtered)
        // /proc/sys/kernel/panic (starts with /proc)
        
        val expectedPaths = listOf(
            "/init.environ.rc",
            "/dev/socket",
            "/system/bin/sh",
            "/system/bin/my_tool",
            "/proc/sys/kernel/panic"
        )
        assertEquals(expectedPaths.sorted(), foundPaths.sorted())
        assertTrue(sources.all { it == "RC analysis of $path" })
        
        coVerify { logger.d("DefaultMetadataCollector", match { it.contains("RC analysis candidates in \"$path\": $expectedPaths") }) }
    }

    @Test
    fun `isMetadataFile recognizes fstab files`() {
        assertTrue(collector.isMetadataFile("/vendor/etc/fstab.qcom"))
        assertTrue(collector.isMetadataFile("/fstab.postinstall"))
        assertTrue(collector.isMetadataFile("fstab.something"))
        assertTrue(collector.isMetadataFile("/etc/recovery.fstab"))
        assertTrue(collector.isMetadataFile("my.fstab"))
        assertTrue(!collector.isMetadataFile("/etc/fstab_not_really"))
    }

    @Test
    fun `processMetadata parses fstab file`() = runTest(testDispatcher) {
        val path = "/vendor/etc/fstab.qcom"
        val content = """
            # comment
            /dev/block/bootdevice/by-name/system /system ext4 ro,barrier=1 wait
            none /dev/cpuctl cgroup cpu
            /dev/block/bootdevice/by-name/userdata /data f2fs noatime,nosuid,nodev,discard,inline_xattr,inline_data,reserve_root=32768,resgid=1065,latemount,wait,check,quota,formattable,reservedsize=128M,checkpoint=fs
            /devices/platform/soc/8804000.sdhci/mmc_host* auto auto defaults voldmanaged=sdcard1:auto
        """.trimIndent()
        fileSystem.writeText(path, content)

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        val expectedPaths = listOf("/system", "/dev/cpuctl", "/data")
        assertEquals(expectedPaths, foundPaths)
        assertTrue(sources.all { it == "fstab analysis of $path" })

        coVerify { logger.d("DefaultMetadataCollector", match { it.contains("fstab analysis candidates in \"$path\": $expectedPaths") }) }
    }

    @Test
    fun `processMetadata parses public libraries file`() = runTest(testDispatcher) {
        val path = "/system/etc/public.libraries.txt"
        val content = """
            # This is a comment
              # Another comment with spaces
            libart.so
            libbase.so 
              libnativehelper.so
            libart.so
            
            # Empty lines above and below
        """.trimIndent()
        fileSystem.writeText(path, content)

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        val expectedLibs = listOf("libart.so", "libbase.so", "libnativehelper.so")
        val prefixes = listOf("/system/lib/", "/system/lib64/", "/vendor/lib/", "/vendor/lib64/")
        val expectedPaths = expectedLibs.flatMap { lib -> prefixes.map { prefix -> prefix + lib } }

        assertEquals(expectedPaths.sorted(), foundPaths.sorted())
        assertTrue(sources.all { it == "public.libraries.txt analysis of $path" })

        coVerify {
            logger.d(
                "DefaultMetadataCollector",
                match { it.contains("public.libraries.txt analysis candidates in \"$path\": $expectedPaths") }
            )
        }
    }

    @Test
    fun `processMetadata parses protobuf file`() = runTest(testDispatcher) {
        val path = "/system/etc/linker.config.pb"
        // Binary data with some paths: /system/bin/app_process and libart.so
        // Delimiters (non-printable or space) around them.
        val binaryData = byteArrayOf(
            0x0A, 0x16, // Random protobuf tags
            '/'.code.toByte(), 's'.code.toByte(), 'y'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(), 'e'.code.toByte(), 'm'.code.toByte(),
            '/'.code.toByte(), 'b'.code.toByte(), 'i'.code.toByte(), 'n'.code.toByte(), '/'.code.toByte(), 'a'.code.toByte(), 'p'.code.toByte(), 'p'.code.toByte(),
            '_'.code.toByte(), 'p'.code.toByte(), 'r'.code.toByte(), 'o'.code.toByte(), 'c'.code.toByte(), 'e'.code.toByte(), 's'.code.toByte(), 's'.code.toByte(),
            0x00, 0x20, // NUL and SPACE (delimiters)
            'l'.code.toByte(), 'i'.code.toByte(), 'b'.code.toByte(), 'a'.code.toByte(), 'r'.code.toByte(), 't'.code.toByte(), '.'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(),
            0x01, // Another delimiter
            '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(), // Starts with digit, should be ignored
            0x00,
            '/'.code.toByte(), 'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(), // Another absolute path
        )
        
        val mockFileSystem = mockk<FileSystem>()
        val collectorWithMock = DefaultMetadataCollector(
            mockFileSystem,
            xmlParser,
            logger,
            dispatcherProvider,
            selinuxAnalyzer,
            getSeedPathsUseCase
        )
        
        coEvery { mockFileSystem.openInputStream(path) } returns ByteArrayInputStream(binaryData)

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collectorWithMock.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        // Expected:
        // /system/bin/app_process -> as is
        // libart.so -> expanded to 4 paths
        // /data -> as is
        // "123" -> ignored (doesn't start with / and doesn't start with letter)

        val expectedPaths = mutableListOf("/system/bin/app_process", "/data")
        val prefixes = listOf("/system/lib/", "/system/lib64/", "/vendor/lib/", "/vendor/lib64/")
        expectedPaths.addAll(prefixes.map { it + "libart.so" })

        assertEquals(expectedPaths.sorted(), foundPaths.sorted())
        assertTrue(sources.all { it == "classpath analysis of $path" })

        coVerify {
            logger.d(
                "DefaultMetadataCollector",
                match { it.contains("classpath analysis candidates in \"$path\":") }
            )
        }
    }

    @Test
    fun `processMetadata parses permissions xml`() = runTest(testDispatcher) {
        val path = "/system/etc/permissions/privapp-permissions-platform.xml"
        fileSystem.writeText(path, "<permissions></permissions>")
        coEvery { xmlParser.parsePermissionsXml(any(), any()) } coAnswers {
            val onEntry = secondArg<suspend (String) -> Unit>()
            onEntry("/system/framework/oem-services.jar")
        }

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        assertEquals(listOf("/system/framework/oem-services.jar"), foundPaths)
        assertEquals(listOf("permissions xml analysis of $path"), sources)
        coVerify { xmlParser.parsePermissionsXml(any(), any()) }
        coVerify { logger.d("DefaultMetadataCollector", match { it.contains("permissions xml analysis candidates in \"$path\":") }) }
    }

    @Test
    fun `processMetadata parses modules load file`() = runTest(testDispatcher) {
        val path = "/vendor/lib/modules/modules.load"
        val content = """
            # comment
            kernel_module_1.ko
            kernel_module_2.ko
        """.trimIndent()
        fileSystem.writeText(path, content)

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        val expectedPaths = listOf(
            "/vendor/lib/modules/kernel_module_1.ko",
            "/vendor/lib/modules/kernel_module_2.ko"
        )
        assertEquals(expectedPaths, foundPaths)
        assertTrue(sources.all { it == "module analysis of $path" })

        coVerify {
            logger.d(
                "DefaultMetadataCollector",
                match { it.contains("module analysis candidates in \"$path\": $expectedPaths") }
            )
        }
    }

    @Test
    fun `processMetadata parses modules dep file`() = runTest(testDispatcher) {
        val path = "/vendor/lib/modules/modules.dep"
        val content = """
            kernel_module_1.ko: kernel_module_base.ko
            kernel_module_2.ko: kernel_module_1.ko kernel_module_base.ko
            kernel_module_standalone.ko:
        """.trimIndent()
        fileSystem.writeText(path, content)

        val foundPaths = mutableListOf<String>()
        val sources = mutableListOf<String>()
        collector.processMetadata(path) { innerPath, source ->
            foundPaths.add(innerPath)
            sources.add(source)
        }

        val expectedPaths = listOf(
            "kernel_module_1.ko",
            "kernel_module_base.ko",
            "kernel_module_2.ko",
            "kernel_module_standalone.ko"
        )
        // Order might vary due to distinct() if we weren't careful, but here it follows the appearance.
        // Let's use sorted comparison if order doesn't strictly matter, but here I'll check if they are all present.
        assertEquals(expectedPaths.distinct().sorted(), foundPaths.sorted())
        assertTrue(sources.all { it == "module analysis of $path" })

        coVerify {
            logger.d(
                "DefaultMetadataCollector",
                match { it.contains("module analysis candidates in \"$path\":") }
            )
        }
    }
}
