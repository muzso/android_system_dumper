package hu.muzso.android_system_dumper.zip

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.model.CompressionLevel
import hu.muzso.android_system_dumper.model.CompressionMethod
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.lingala.zip4j.ZipFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

class Zip4jZipCreatorTest {

    private val logger = FakeFileLogger()
    private val fsRoot = Paths.get("/")
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val fakeFs = spyk(FakeJvmFileSystem(dispatcherProvider,fsRoot))
    private val zipCreator by lazy { Zip4jZipCreator(logger, fakeFs, dispatcherProvider) }

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `create creates a valid zip file`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("content1")
        val file2 = tempDir.resolve("file2.txt")
        file2.writeText("content2")

        val outputFile = tempDir.resolve("test.zip").toFile()
        val files = listOf(
            ZipFileEntry(file1.toString(), "inner/file1.txt"),
            ZipFileEntry(file2.toString(), "file2.txt")
        )
        val options = ZipOptions(
            outputFilePath = outputFile.absolutePath,
            encryptionMethod = ZipEncryption.NONE,
            compressionMethod = CompressionMethod.DEFLATE,
            compressionLevel = CompressionLevel.NORMAL
        )

        zipCreator.create(files, options, false)

        assertThat(outputFile.exists()).isTrue()
        
        ZipFile(outputFile).use { zip ->
            assertThat(zip.fileHeaders.map { it.fileName }).containsExactly("inner/file1.txt", "file2.txt")
        }
    }

    @Test
    fun `create creates an encrypted zip file`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("sensitive content")
        val password = "password123".toCharArray()

        val outputFile = tempDir.resolve("encrypted.zip").toFile()
        val files = listOf(ZipFileEntry(file1.toString(), "file1.txt"))
        val options = ZipOptions(
            outputFilePath = outputFile.absolutePath,
            encryptionMethod = ZipEncryption.AES,
            password = password
        )

        zipCreator.create(files, options, false)

        assertThat(outputFile.exists()).isTrue()

        val zipFile = ZipFile(outputFile)
        assertThat(zipFile.isEncrypted).isTrue()
        
        zipFile.setPassword(password)
        val extractedFile = tempDir.resolve("extracted.txt").toFile()
        zipFile.extractFile("file1.txt", tempDir.toString(), "extracted.txt")
        
        assertThat(extractedFile.readText()).isEqualTo("sensitive content")
    }

    @Test
    fun `create with readIntoMemory true works`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("content1")

        val outputFile = tempDir.resolve("test_memory.zip").toFile()
        val files = listOf(ZipFileEntry(file1.toString(), "file1.txt"))
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        zipCreator.create(files, options, true)

        assertThat(outputFile.exists()).isTrue()
    }

    @Test
    fun `create skips non-existent files`() = runTest(testDispatcher) {
        val outputFile = tempDir.resolve("skipped.zip").toFile()
        val files = listOf(ZipFileEntry("/non/existent/path", "missing.txt"))
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        zipCreator.create(files, options, false)

        assertThat(outputFile.exists()).isTrue()
        ZipFile(outputFile).use { zip ->
            assertThat(zip.fileHeaders).isEmpty()
        }
    }

    @Test
    fun `create with zero length file reads into memory`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("zero.txt")
        file1.toFile().createNewFile() // 0 bytes

        val outputFile = tempDir.resolve("zero_test.zip").toFile()
        val files = listOf(ZipFileEntry(file1.toString(), "zero.txt"))
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        zipCreator.create(files, options, false)

        assertThat(outputFile.exists()).isTrue()
    }

    @Test
    fun `mapping standard encryption works`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("content")
        val outputFile = tempDir.resolve("standard_enc.zip").toFile()
        val options = ZipOptions(
            outputFilePath = outputFile.absolutePath,
            encryptionMethod = ZipEncryption.STANDARD,
            password = "pass".toCharArray()
        )

        zipCreator.create(listOf(ZipFileEntry(file1.toString(), "f.txt")), options, false)
        assertThat(outputFile.exists()).isTrue()
    }

    @Test
    fun `mapping all compression levels and methods`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("content")
        
        val combinations = listOf(
            CompressionMethod.STORE to CompressionLevel.NO_COMPRESSION,
            CompressionMethod.DEFLATE to CompressionLevel.FASTEST,
            CompressionMethod.DEFLATE to CompressionLevel.FAST,
            CompressionMethod.DEFLATE to CompressionLevel.NORMAL,
            CompressionMethod.DEFLATE to CompressionLevel.MAXIMUM,
            CompressionMethod.DEFLATE to CompressionLevel.ULTRA
        )

        for ((m, l) in combinations) {
            val outputFile = tempDir.resolve("zip_${m}_${l}.zip").toFile()
            val options = ZipOptions(
                outputFilePath = outputFile.absolutePath,
                encryptionMethod = ZipEncryption.NONE,
                compressionMethod = m,
                compressionLevel = l
            )
            zipCreator.create(listOf(ZipFileEntry(file1.toString(), "f.txt")), options, false)
            assertThat(outputFile.exists()).isTrue()
        }
    }

    @Test
    fun `create logs error when ZIP creation fails`() = runTest(testDispatcher) {
        val outputFile = File("/non_existent_directory/error.zip")
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(emptyList(), options, false)

        assertThat(result).isInstanceOf(DomainResult.Error::class.java)
        logger.assertErrorLogExists("Zip4jZipCreator", "Failed to create ZIP")
    }

    @Test
    fun `simulate ENOSPC error returns correct DomainResult`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("content")
        val outputFile = tempDir.resolve("nospace.zip").toFile()
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        // Force openOutputStream to fail with ENOSPC message
        coEvery { fakeFs.openOutputStream(any(), any()) } throws IOException("No space left on device")

        val result = zipCreator.create(listOf(ZipFileEntry(file1.toString(), "f.txt")), options, false)

        assertThat(result).isInstanceOf(DomainResult.Error::class.java)
        val error = (result as DomainResult.Error).error
        assertThat(error).isInstanceOf(ZipError.IOException::class.java)
        assertThat((error as ZipError.IOException).message).isEqualTo("No space left on device")
    }

    @Test
    fun `Zip4j generic failure returns Zip4jError`() = runTest(testDispatcher) {
        val file1 = tempDir.resolve("file1.txt")
        file1.writeText("content")
        val outputFile = tempDir.resolve("fail.zip").toFile()
        val options = ZipOptions(outputFilePath = outputFile.absolutePath, encryptionMethod = ZipEncryption.NONE)

        // Mock openOutputStream to throw generic exception
        coEvery { fakeFs.openOutputStream(any(), any()) } throws RuntimeException("Random Zip Fail")

        val result = zipCreator.create(listOf(ZipFileEntry(file1.toString(), "f.txt")), options, false)

        assertThat(result).isInstanceOf(DomainResult.Error::class.java)
        assertThat(((result as DomainResult.Error).error as ZipError.Zip4jError).message).isEqualTo("Random Zip Fail")
    }
}
