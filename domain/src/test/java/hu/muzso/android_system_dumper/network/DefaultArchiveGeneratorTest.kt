package hu.muzso.android_system_dumper.network

import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import io.mockk.every
import io.mockk.mockk
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
        every { params.shouldUploadAppLogs } returns true
        val scanResult = ScanResult(readableFiles = emptyList())
        
        generator.prepare(params, scanResult)
        
        Truth.assertThat(generator.shouldGenerateMisc()).isTrue()
    }
}
