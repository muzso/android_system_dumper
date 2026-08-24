package hu.muzso.android_system_dumper.domain.repository.zip

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.zip.ZipCreator
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

abstract class ZipCreatorContract {
    abstract fun createZipCreator(filesystem: FileSystem, dispatcherProvider: FakeDispatcherProvider): ZipCreator

    protected lateinit var testDispatcher: TestDispatcher
    protected lateinit var dispatcherProvider: FakeDispatcherProvider
    protected lateinit var fileSystem: FakeJvmFileSystem

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        dispatcherProvider = FakeDispatcherProvider(testDispatcher)
        fileSystem = FakeJvmFileSystem(dispatcherProvider)
    }

    @After
    fun cleanup() {
        if (::fileSystem.isInitialized) {
            fileSystem.close()
        }
    }

    @Test
    fun create_produces_a_zip_file() = runTest(testDispatcher) {
        val zipCreator = createZipCreator(fileSystem, dispatcherProvider)
        val file1 = fileSystem.addFileWithText("file1.txt", "content1")
        
        val outputFile = "test.zip"
        val files = listOf(ZipFileEntry(file1, "file1.txt"))
        val options = ZipOptions(outputFilePath = outputFile, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(files, options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        assertThat(fileSystem.exists(outputFile)).isTrue()
        fileSystem.close()
    }

    @Test
    fun create_encrypted_zip_file() = runTest(testDispatcher) {
        val zipCreator = createZipCreator(fileSystem, dispatcherProvider)
        val file1 = fileSystem.addFileWithText("file2.txt", "sensitive")

        val outputFile = "encrypted.zip"
        val files = listOf(ZipFileEntry(file1, "file2.txt"))
        val options = ZipOptions(
            outputFilePath = outputFile,
            encryptionMethod = ZipEncryption.AES,
            password = "password".toCharArray()
        )

        val result = zipCreator.create(files, options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        assertThat(fileSystem.exists(outputFile)).isTrue()
    }

    @Test
    fun create_with_missing_input_file_returns_success() = runTest(testDispatcher) {
        val zipCreator = createZipCreator(fileSystem, dispatcherProvider)
        val missingFile = "missing.txt"
        
        val outputFile = "test.zip"
        val files = listOf(ZipFileEntry(missingFile, "missing.txt"))
        val options = ZipOptions(outputFilePath = outputFile, encryptionMethod = ZipEncryption.NONE)

        val result = zipCreator.create(files, options, false)

        assertThat(result).isInstanceOf(DomainResult.Success::class.java)
    }
}
