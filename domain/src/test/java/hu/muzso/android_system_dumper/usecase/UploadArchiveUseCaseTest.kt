package hu.muzso.android_system_dumper.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.network.DefaultArchiveGenerator
import hu.muzso.android_system_dumper.network.upload.UploadProgressTracker
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.SystemInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class UploadArchiveUseCaseTest {

    private val fileSystem = FakeMemoryFileSystem()
    private val clock = mockk<Clock>(relaxed = true)
    private val logger = spyk(FakeFileLogger())
    private val batchFilesUseCase = mockk<BatchFilesUseCase>()
    private val createArchiveUseCase = mockk<CreateArchiveUseCase>(relaxed = true)
    private val uploadBatchUseCase = mockk<UploadBatchUseCase>(relaxed = true)
    private val cleanupUseCase = mockk<CleanupUseCase>(relaxed = true)
    private val resourceProvider = mockk<ResourceProvider>(relaxed = true)
    private val systemInfo = mockk<SystemInfo>(relaxed = true)
    private val uploadRepository = mockk<UploadRepository>(relaxed = true)
    private val progressTracker = mockk<UploadProgressTracker>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)

    private lateinit var useCase: UploadArchiveUseCase

    @Before
    fun setup() {
        val archiveGenerator = DefaultArchiveGenerator(
            fileSystem, clock, logger, systemInfo, batchFilesUseCase, createArchiveUseCase, cleanupUseCase
        )
        useCase = UploadArchiveUseCase(
            clock, logger, uploadBatchUseCase, cleanupUseCase, resourceProvider,
            progressTracker, dispatcherProvider, archiveGenerator
        )
        every { progressTracker.totalUploadedBytes } returns MutableStateFlow(0L)
        coEvery { progressTracker.reset() } just runs
        every { clock.now() } returns Instant.now()
        every { clock.monotonicTime() } returnsMany listOf(1_000_000_000L, 2_000_000_000L) // 1 second apart
    }

    @Test
    fun `execute resets progress tracker`() = runTest(testDispatcher) {
        val parameters = createParameters()
        val scanResult = ScanResult()
        coEvery { uploadRepository.getUrlListUrl() } returns "" // Force Error to end quickly

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(0))
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Error::class.java)
            awaitComplete()
        }

        coVerify { progressTracker.reset() }
    }

    @Test
    fun `execute flushes logs when shouldUploadAppLogs is true`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadAppLogs = true)
        val scanResult = ScanResult()
        
        every { logger.getLogFilePath() } returns "/log.txt"
        fileSystem.addFileOfSize("/log.txt", 100)
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("misc.zip")
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returns DomainResult.Success("url")
        coEvery { uploadRepository.getUrlListUrl() } returns "url"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Success::class.java)
            awaitComplete()
        }

        verify { logger.flush() }
    }

    @Test
    fun `execute calculates runtimeInSeconds correctly`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadAppLogs = true)
        val scanResult = ScanResult()
        
        every { logger.getLogFilePath() } returns null
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("misc.zip")
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returns DomainResult.Success("url")
        coEvery { uploadRepository.getUrlListUrl() } returns "http://test.com"

        // Mock monotonic time to return 0 then 5 seconds (5 * 10^9 ns)
        every { clock.monotonicTime() } returnsMany listOf(0L, 5_000_000_000L)

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))
            val success = awaitItem() as UploadWorkflowStatus.Success
            assertThat(success.runtimeSeconds).isEqualTo(5L)
            awaitComplete()
        }
    }

    @Test
    fun `execute emits correct batch index in ArchivingBatch`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/f1", 10L, "source"), FileEntry("/f2", 10L, "source")))
        
        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns listOf(listOf("/f1"), listOf("/f2"))
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("zip")
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returns DomainResult.Success("url")
        coEvery { uploadRepository.getUrlListUrl() } returns "url"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(2))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 2))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(2, 2))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `execute emits Preparing status first`() = runTest(testDispatcher) {
        val parameters = createParameters()
        val scanResult = ScanResult()

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `execute logs error when ZIP creation fails`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source")))

        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns listOf(listOf("/test"))
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Error(ZipError.IOException("ZIP Error"))

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))
            val error = awaitItem() as UploadWorkflowStatus.Error
            assertThat((error.error as UploadError.Unknown).message).contains("Failed to create ZIP")
            awaitComplete()
        }

        logger.assertErrorLogExists("UploadArchiveUseCase", "Failed to create ZIP: IOException(message=ZIP Error, cause=null)")
    }

    @Test
    fun `execute logs error when upload fails`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source")))

        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns listOf(listOf("/test"))
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("test.zip")
        every { createArchiveUseCase.generateBatchFilename(any(), any(), any()) } returns "batch1.zip"
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returns DomainResult.Error(UploadError.Unknown("Upload Error"))
        coEvery { uploadRepository.getUrlListUrl() } returns "http://test.com"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Error::class.java)
            awaitComplete()
        }

        logger.assertErrorLogExists("UploadArchiveUseCase", "Failed to upload batch1.zip: Unknown(message=Upload Error, cause=null)")
    }

    @Test
    fun `execute emits incremental SuccessfulUploads`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(
            shouldUploadZips = true,
            shouldUploadAppLogs = true
        )
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source")))

        val batches = List(5) { listOf("/test") }
        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns batches
        
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("test.zip")
        every { createArchiveUseCase.generateBatchFilename(any(), any(), any()) } returns "batch.zip"
        every { createArchiveUseCase.generateMiscZipFilename(any()) } returns "misc.zip"
        
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returns DomainResult.Success("http://success.com")
        coEvery { uploadRepository.getUrlListUrl() } returns "http://test.com"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(6))
            for (i in 1..5) {
                assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(i, 5))
                assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(i))
            }
            
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(6))
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Success::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `execute emits PartialSuccess when some batches fail`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/f1", 10L, "source"), FileEntry("/f2", 10L, "source")))

        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns listOf(listOf("/f1"), listOf("/f2"))
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("zip")
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returnsMany listOf(
            DomainResult.Success("url1"),
            DomainResult.Error(UploadError.Unknown("Fail"))
        )
        coEvery { uploadRepository.getUrlListUrl() } returns "url"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(2))
            
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 2))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))
            
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(2, 2))
            
            val result = awaitItem() as UploadWorkflowStatus.PartialSuccess
            assertThat(result.uploadedZips).isEqualTo(1)
            assertThat(result.totalZips).isEqualTo(2)
            awaitComplete()
        }
    }

    @Test
    fun `execute emits status for all misc files`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(
            shouldUploadFileLists = true,
            shouldUploadGetprop = true,
            shouldUploadAppLogs = true
        )
        val scanResult = ScanResult(
            readableFiles = listOf(FileEntry("/f1", 10L, "source")),
            unreadableFiles = listOf("/u1"),
            excludedFiles = listOf("/e1"),
            missingFiles = listOf("/m1"),
            symlinks = mapOf("/s1" to "/f1")
        )

        every { logger.getLogFilePath() } returns "/log.txt"
        fileSystem.addFileOfSize("/log.txt", 100)
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("misc.zip")
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returns DomainResult.Success("url")
        coEvery { uploadRepository.getUrlListUrl() } returns "url"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Success::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `execute uses default batch size when invalid`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(customBatchSizeMb = "invalid", shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source")))

        every { batchFilesUseCase.execute(any(), any(), eq(10L * 1024 * 1024), any()) } returns listOf(listOf("/test"))
        coEvery { uploadRepository.getUrlListUrl() } returns "url"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `execute skips ZIPs when readableFiles is empty`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = emptyList())

        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns emptyList()
        coEvery { uploadRepository.getUrlListUrl() } returns "url"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(0))
            
            val error = awaitItem() as UploadWorkflowStatus.Error
            assertThat(error.error).isInstanceOf(UploadError.ZeroSuccessfulUploads::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `execute emits error when insufficient space during batch creation`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source")))

        every { batchFilesUseCase.execute(any(), any(), any(), any()) } returns listOf(listOf("/test"))
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Error(ZipError.InsufficientSpace(1024L))

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))
            
            val error = awaitItem() as UploadWorkflowStatus.Error
            assertThat(error.error).isInstanceOf(UploadError.InsufficientStorage::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `execute emits error when getUrlListUrl is empty`() = runTest(testDispatcher) {
        val parameters = createParameters()
        val scanResult = ScanResult()
        coEvery { uploadRepository.getUrlListUrl() } returns ""

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(0))
            val error = awaitItem() as UploadWorkflowStatus.Error
            assertThat(error.error).isInstanceOf(UploadError.MissingDownloadURL::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `execute emits error when zero successful uploads`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadAppLogs = true)
        val scanResult = ScanResult()
        
        coEvery { createArchiveUseCase.execute(any(), any(), any()) } returns DomainResult.Success("misc.zip")
        coEvery { uploadBatchUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()) } returns DomainResult.Error(UploadError.Unknown("Fail"))
        coEvery { uploadRepository.getUrlListUrl() } returns "http://test.com"

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            val error = awaitItem() as UploadWorkflowStatus.Error
            assertThat(error.error).isInstanceOf(UploadError.ZeroSuccessfulUploads::class.java)
            awaitComplete()
        }
    }

    private fun createParameters() = UploadParameters(
        customBatchSizeMb = "10",
        proxySpecification = "",
        shouldUseTor = false,
        shouldUploadZips = false,
        shouldUploadFileLists = false,
        shouldUploadGetprop = false,
        shouldUploadAppLogs = false,
        zipEncryption = ZipEncryption.NONE,
        selectedService = uploadRepository,
        maxBatches = 0,
        useDoubleZipping = false
    )
}
