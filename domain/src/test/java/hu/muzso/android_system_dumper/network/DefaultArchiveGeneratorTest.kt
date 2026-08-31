package hu.muzso.android_system_dumper.network

import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DefaultArchiveGeneratorTest {
    private val fileSystem = mockk<FileSystem>()
    private val clock = mockk<Clock>()
    private val logger = mockk<FileLogger>(relaxed = true)
    private val systemInfo = mockk<SystemInfo>()
    private val batchFilesUseCase = mockk<BatchFilesUseCase>()
    private val createArchiveUseCase = mockk<CreateArchiveUseCase>()
    private val cleanupUseCase = mockk<CleanupUseCase>()

    private lateinit var generator: DefaultArchiveGenerator

    @Before
    fun setup() {
        generator = DefaultArchiveGenerator(
            fileSystem, clock, logger, systemInfo, batchFilesUseCase, createArchiveUseCase, cleanupUseCase
        )
        every { clock.now() } returns Instant.parse("2026-08-25T12:00:00Z")
        every { createArchiveUseCase.generatePassphrase(any()) } returns "passphrase123"
    }

    @Test
    fun `prepare sets up batches and passphrase`() {
        val params = mockk<UploadParameters>(relaxed = true)
        every { params.shouldUploadZips } returns true
        every { params.zipEncryption } returns ZipEncryption.STANDARD
        every { params.customBatchSizeMb } returns "200"
        every { params.maxBatches } returns 0
        every { createArchiveUseCase.generatePassphrase(any()) } returns "passphrase123"
        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns listOf(listOf("file1"), listOf("file2"))
        
        val scanResult = ScanResult(readableFiles = emptyList())
        
        generator.prepare(params, scanResult)
        
        Truth.assertThat(generator.getBatchCount()).isEqualTo(2)
        Truth.assertThat(generator.getEncryptionPassphrase()).isEqualTo("passphrase123")
    }

    @Test
    fun `shouldGenerateMisc returns true when misc flags are set`() {
        val params = mockk<UploadParameters>(relaxed = true)
        every { params.shouldUploadFileLists } returns true
        val scanResult = ScanResult(readableFiles = emptyList())
        
        generator.prepare(params, scanResult)
        
        Truth.assertThat(generator.shouldGenerateMisc()).isTrue()
    }

    @Test
    fun `generateMisc writes all five file lists when shouldUploadFileLists is true`() = runTest {
        val params = mockk<UploadParameters>(relaxed = true)
        every { params.shouldUploadFileLists } returns true
        every { params.zipEncryption } returns ZipEncryption.NONE
        
        val scanResult = ScanResult(
            readableFiles = listOf(FileEntry("/r1", 10, "s")),
            unreadableFiles = listOf("/u1"),
            excludedFiles = listOf("/e1"),
            missingFiles = listOf("/m1"),
            symlinks = mapOf("/s1" to "/target")
        )

        coEvery { fileSystem.getCacheDir() } returns "/cache"
        coEvery { fileSystem.join(any(), any()) } answers { "${it.invocation.args[0]}/${it.invocation.args[1]}" }
        coEvery { fileSystem.writeText(any(), any()) } returns Unit
        coEvery { fileSystem.getCanonicalPath(any()) } answers { it.invocation.args[0] as String }
        coEvery { fileSystem.getFileName(any()) } answers { (it.invocation.args[0] as String).substringAfterLast('/') }
        coEvery { fileSystem.exists(any()) } returns true
        coEvery { fileSystem.size(any()) } returns 10L
        coEvery { cleanupUseCase.execute(any()) } returns Unit
        
        every { createArchiveUseCase.generateMiscZipFilename(any()) } returns "misc.zip"
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("misc.zip")

        generator.prepare(params, scanResult)
        generator.generateMisc()

        coVerify { fileSystem.writeText("/cache/readable_list.txt", any()) }
        coVerify { fileSystem.writeText("/cache/unreadable_list.txt", any()) }
        coVerify { fileSystem.writeText("/cache/excluded_list.txt", any()) }
        coVerify { fileSystem.writeText("/cache/missing_list.txt", any()) }
        coVerify { fileSystem.writeText("/cache/symlink_list.txt", any()) }

        coVerify {
            createArchiveUseCase.execute(
                match { list ->
                    list.size == 5 &&
                            list.any { it.zipPath == "readable_list.txt" } &&
                            list.any { it.zipPath == "unreadable_list.txt" } &&
                            list.any { it.zipPath == "excluded_list.txt" } &&
                            list.any { it.zipPath == "missing_list.txt" } &&
                            list.any { it.zipPath == "symlink_list.txt" }
                },
                any(),
                true
            )
        }
    }

    @Test
    fun `generateMisc writes none of the five file lists when shouldUploadFileLists is false`() = runTest {
        val params = mockk<UploadParameters>(relaxed = true)
        every { params.shouldUploadFileLists } returns false
        every { params.zipEncryption } returns ZipEncryption.NONE
        
        val scanResult = ScanResult(
            readableFiles = listOf(FileEntry("/r1", 10, "s")),
            unreadableFiles = listOf("/u1"),
            excludedFiles = listOf("/e1"),
            missingFiles = listOf("/m1"),
            symlinks = mapOf("/s1" to "/target")
        )

        coEvery { fileSystem.getCacheDir() } returns "/cache"
        coEvery { fileSystem.join(any(), any()) } answers { "${it.invocation.args[0]}/${it.invocation.args[1]}" }
        coEvery { fileSystem.writeText(any(), any()) } returns Unit
        coEvery { fileSystem.getCanonicalPath(any()) } answers { it.invocation.args[0] as String }
        coEvery { fileSystem.getFileName(any()) } answers { (it.invocation.args[0] as String).substringAfterLast('/') }
        coEvery { cleanupUseCase.execute(any()) } returns Unit
        
        every { createArchiveUseCase.generateMiscZipFilename(any()) } returns "misc.zip"
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("misc.zip")

        generator.prepare(params, scanResult)
        generator.generateMisc()

        coVerify(exactly = 0) { fileSystem.writeText("/cache/readable_list.txt", any()) }
        coVerify(exactly = 0) { fileSystem.writeText("/cache/unreadable_list.txt", any()) }
        coVerify(exactly = 0) { fileSystem.writeText("/cache/excluded_list.txt", any()) }
        coVerify(exactly = 0) { fileSystem.writeText("/cache/missing_list.txt", any()) }
        coVerify(exactly = 0) { fileSystem.writeText("/cache/symlink_list.txt", any()) }

        coVerify {
            createArchiveUseCase.execute(
                match { it.isEmpty() },
                any(),
                true
            )
        }
    }
}
