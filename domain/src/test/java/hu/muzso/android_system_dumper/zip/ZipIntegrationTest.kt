package hu.muzso.android_system_dumper.zip

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.CompressionLevel
import hu.muzso.android_system_dumper.model.CompressionMethod
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.lingala.zip4j.ZipFile
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ZipIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val logger = mockk<FileLogger>(relaxed = true)
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var dispatcherProvider: FakeDispatcherProvider
    lateinit var fileSystem: FakeJvmFileSystem
    private lateinit var zipCreator: Zip4jZipCreator

    @Before
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        dispatcherProvider = FakeDispatcherProvider(testDispatcher)
        fileSystem = FakeJvmFileSystem(dispatcherProvider,tempFolder.root.toPath())
        zipCreator = Zip4jZipCreator(logger, fileSystem, dispatcherProvider)
    }

    @After
    fun cleanup() {
        if (::fileSystem.isInitialized) {
            fileSystem.close()
        }
    }

    @Test
    fun create_createsEncryptedArchiveWithCorrectFiles() = runTest(testDispatcher) {
        // Arrange
        val root = tempFolder.root
        val file1 = File(root, "test1.txt")
        file1.writeText("content 1")
        val file2 = File(root, "test2.txt")
        file2.writeText("content 2")

        val outputZip = File(root, "output.zip")
        val passphrase = "test-passphrase"
        val options = ZipOptions(
            outputFilePath = outputZip.absolutePath,
            encryptionMethod = ZipEncryption.STANDARD,
            passphrase = passphrase.toCharArray(),
            compressionMethod = CompressionMethod.DEFLATE,
            compressionLevel = CompressionLevel.NORMAL
        )

        val files = listOf(
            ZipFileEntry(file1.absolutePath, "test1.txt"),
            ZipFileEntry(file2.absolutePath, "sub/test2.txt")
        )

        // Act
        val result = zipCreator.create(files, options, false)

        // Assert
        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        val zipFileResultPath = (result as DomainResult.Success).data
        val zipFileResult = File(zipFileResultPath)
        assertThat(zipFileResult.exists()).isTrue()
        assertThat(zipFileResult.absolutePath).isEqualTo(outputZip.absolutePath)

        val zipFile = ZipFile(zipFileResult, passphrase.toCharArray())
        assertThat(zipFile.isValidZipFile).isTrue()
        assertThat(zipFile.isEncrypted).isTrue()

        val fileHeaders = zipFile.fileHeaders
        assertThat(fileHeaders).hasSize(2)

        val filenames = fileHeaders.map { it.fileName }
        assertThat(filenames).containsExactly("test1.txt", "sub/test2.txt")

        // Verify content by extracting
        val extractDir = File(root, "extracted")
        extractDir.mkdirs()
        zipFile.extractAll(extractDir.absolutePath)

        assertThat(File(extractDir, "test1.txt").readText()).isEqualTo("content 1")
        assertThat(File(extractDir, "sub/test2.txt").readText()).isEqualTo("content 2")
    }

    @Test
    fun create_createsUnencryptedArchiveWhenSpecified() = runTest(testDispatcher) {
        // Arrange
        val root = tempFolder.root
        val file1 = File(root, "test_unencrypted.txt")
        file1.writeText("some content")

        val outputZip = File(root, "unencrypted.zip")
        val options = ZipOptions(
            outputFilePath = outputZip.absolutePath,
            encryptionMethod = ZipEncryption.NONE,
            passphrase = null,
            compressionMethod = CompressionMethod.DEFLATE,
            compressionLevel = CompressionLevel.FASTEST
        )

        val files = listOf(ZipFileEntry(file1.absolutePath, "test.txt"))

        // Act
        val result = zipCreator.create(files, options, false)

        // Assert
        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        val zipFileResultPath = (result as DomainResult.Success).data
        val zipFileResult = File(zipFileResultPath)
        assertThat(zipFileResult.exists()).isTrue()
        val zipFile = ZipFile(zipFileResult)
        assertThat(zipFile.isEncrypted).isFalse()

        val extractDir = File(root, "extracted_unencrypted")
        extractDir.mkdirs()
        zipFile.extractAll(extractDir.absolutePath)
        assertThat(File(extractDir, "test.txt").readText()).isEqualTo("some content")
    }
}