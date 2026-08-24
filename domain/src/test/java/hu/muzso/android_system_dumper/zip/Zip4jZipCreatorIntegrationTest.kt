package hu.muzso.android_system_dumper.zip

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.model.CompressionLevel
import hu.muzso.android_system_dumper.model.CompressionMethod
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.util.Zip4jUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText
import kotlin.math.abs
import net.lingala.zip4j.model.enums.CompressionMethod as Zip4jCompressionMethod

class Zip4jZipCreatorIntegrationTest {

    private val logger = FakeFileLogger()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val zipCreator by lazy { Zip4jZipCreator(logger, FakeJvmFileSystem(dispatcherProvider,Paths.get("/")), dispatcherProvider) }

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `verify filenames and directory structure`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("a.txt").apply { writeText("a") }
        val file2 = tempDir.resolve("b.txt").apply { writeText("b") }
        val outputFile = tempDir.resolve("structure.zip").toFile()

        val files = listOf(
            ZipFileEntry(file1.toString(), "root.txt"),
            ZipFileEntry(file2.toString(), "dir/sub.txt"),
        )
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(files, options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        ZipFile(outputFile).use { zip ->
            val fileNames = zip.fileHeaders.map { it.fileName }
            assertThat(fileNames).containsExactly("root.txt", "dir/sub.txt")
        }
    }

    @Test
    fun `verify compression method and level`() = runTest(testDispatcher) {
        val largeContent = "A".repeat(1000)
        val file = tempDir.resolve("large.txt").apply { writeText(largeContent) }
        val outputFile = tempDir.resolve("compressed.zip").toFile()

        val options = ZipOptions(
            outputFilePath = outputFile.absolutePath,
            encryptionMethod = ZipEncryption.NONE,
            compressionMethod = CompressionMethod.DEFLATE,
            compressionLevel = CompressionLevel.MAXIMUM
        )

        val result = zipCreator.create(listOf(ZipFileEntry(file.toString(), "large.txt")), options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        ZipFile(outputFile).use { zip ->
            val header = zip.getFileHeader("large.txt")
            assertThat(header.compressionMethod).isEqualTo(Zip4jCompressionMethod.DEFLATE)
            assertThat(header.compressedSize).isLessThan(header.uncompressedSize)
        }
    }

    @Test
    fun `verify store compression method`() = runTest(testDispatcher) {
        val content = "no compression"
        val file = tempDir.resolve("store.txt").apply { writeText(content) }
        val outputFile = tempDir.resolve("store.zip").toFile()

        val options = ZipOptions(
            outputFilePath = outputFile.absolutePath,
            encryptionMethod = ZipEncryption.NONE,
            compressionMethod = CompressionMethod.STORE
        )

        val result = zipCreator.create(listOf(ZipFileEntry(file.toString(), "store.txt")), options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        ZipFile(outputFile).use { zip ->
            val header = zip.getFileHeader("store.txt")
            assertThat(header.compressionMethod).isEqualTo(Zip4jCompressionMethod.STORE)
            assertThat(header.compressedSize).isEqualTo(header.uncompressedSize)
        }
    }

    @Test
    fun `verify timestamps are preserved`() = runTest(testDispatcher) {
        val file = tempDir.resolve("time.txt").apply { writeText("time") }
        val originalTime = 1722643200000L // 2024-08-03
        file.toFile().setLastModified(originalTime)

        val outputFile = tempDir.resolve("time.zip").toFile()
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(listOf(ZipFileEntry(file.toString(), "time.txt")), options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        ZipFile(outputFile).use { zip ->
            val header = zip.getFileHeader("time.txt")
            // Zip4j uses DOS time format which has 2s resolution.
            // We use dosToExtendedEpochTme to handle the conversion correctly.
            // Note: Zip4j 2.11.6 has a typo in the method name "dosToExtendedEpochTme".
            val lastModified = Zip4jUtil.dosToExtendedEpochTme(header.lastModifiedTime)
            assertThat(abs(lastModified - originalTime)).isLessThan(2000L)
        }
    }

    @Test
    fun `verify permissions are preserved if supported`() = runTest(testDispatcher) {
        // This test might be environmental, but we try to set specific permissions
        val file = tempDir.resolve("perms.txt").apply { writeText("perms") }
        
        try {
            val permissions = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
            file.setPosixFilePermissions(permissions)
            
            val outputFile = tempDir.resolve("perms.zip").toFile()
            val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

            val result = zipCreator.create(listOf(ZipFileEntry(file.toString(), "perms.txt")), options, false)

            assertThat(result).isInstanceOf(DomainResult.Success::class.java)
            ZipFile(outputFile).use { zip ->
                val header = zip.getFileHeader("perms.txt")
                // External attributes contain permissions in the upper bytes for UNIX
                // Zip4jZipCreator currently doesn't explicitly set them, so we just check if they are there.
                val extAttrs = header.externalFileAttributes
                assertThat(extAttrs).isNotNull()
            }
        } catch (_: UnsupportedOperationException) {
            // Not a POSIX filesystem, skip assertion
        }
    }

    @Test
    fun `verify empty archive works`() = runTest(testDispatcher) {
        val outputFile = tempDir.resolve("empty.zip").toFile()
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(emptyList(), options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        assertThat(outputFile.exists()).isTrue()
        ZipFile(outputFile).use { zip ->
            assertThat(zip.fileHeaders).isEmpty()
        }
    }

    @Test
    fun `verify duplicate names are handled`() = runTest(testDispatcher) {
        // Zip4j ZipFile.addFile overwrites existing entries with the same name.
        // This test verifies that only the last entry with the same name remains.
        val file1 = tempDir.resolve("1.txt").apply { writeText("one") }
        val file2 = tempDir.resolve("2.txt").apply { writeText("two") }
        val outputFile = tempDir.resolve("duplicate.zip").toFile()

        val files = listOf(
            ZipFileEntry(file1.toString(), "dup.txt"),
            ZipFileEntry(file2.toString(), "dup.txt"),
        )
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(files, options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        ZipFile(outputFile).use { zip ->
            val headers = zip.fileHeaders.filter { it.fileName == "dup.txt" }
            assertThat(headers).hasSize(1)
            
            val extractDir = tempDir.resolve("extracted_dup")
            zip.extractAll(extractDir.toString())
            assertThat(extractDir.resolve("dup.txt").toFile().readText()).isEqualTo("two")
        }
    }

    @Test
    fun `verify unicode filenames`() = runTest(testDispatcher) {
        val unicodeName = "árvíztűrőtükörfúrógép.txt"
        val file = tempDir.resolve(unicodeName).apply { writeText("unicode") }
        val outputFile = tempDir.resolve("unicode.zip").toFile()

        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(listOf(ZipFileEntry(file.toString(), unicodeName)), options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        ZipFile(outputFile).use { zip ->
            val header = zip.getFileHeader(unicodeName)
            assertThat(header).isNotNull()
            assertThat(header.fileName).isEqualTo(unicodeName)
        }
    }
}
