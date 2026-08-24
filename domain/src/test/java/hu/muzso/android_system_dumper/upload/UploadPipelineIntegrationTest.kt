package hu.muzso.android_system_dumper.upload

import app.cash.turbine.test
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.common.DefaultPlatformUtils
import hu.muzso.android_system_dumper.common.RandomProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeGofileGateway
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeTorServiceController
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.repository.SettingsRepository
import hu.muzso.android_system_dumper.scan.DefaultArchiveRepository
import hu.muzso.android_system_dumper.upload.network.DefaultUploadExecutor
import hu.muzso.android_system_dumper.upload.network.DefaultUploadProgressTracker
import hu.muzso.android_system_dumper.upload.network.DefaultUploadRetryPolicy
import hu.muzso.android_system_dumper.upload.network.DefaultUploadSelector
import hu.muzso.android_system_dumper.upload.network.GofileUploadRepository
import hu.muzso.android_system_dumper.upload.network.gateway.GatewayResult
import hu.muzso.android_system_dumper.upload.network.gateway.GofileGateway
import hu.muzso.android_system_dumper.upload.network.gateway.GofileUploadDomainModel
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import hu.muzso.android_system_dumper.usecase.UploadArchiveUseCase
import hu.muzso.android_system_dumper.usecase.UploadBatchUseCase
import hu.muzso.android_system_dumper.zip.Zip4jZipCreator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class UploadPipelineIntegrationTest {

    private val fileSystem = FakeMemoryFileSystem()
    private val logger = FakeFileLogger()
    private val clock = FakeClock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val torService = FakeTorServiceController()

    private val randomProvider = mockk<RandomProvider>()
    private val resourceProvider = mockk<ResourceProvider>()
    private val systemInfo = mockk<SystemInfo>()
    private val settingsRepository = mockk<SettingsRepository>()

    private val gateway = FakeGofileGateway()
    private val gofileRepository = GofileUploadRepository(gateway)

    private val uploadSelector = DefaultUploadSelector(
        repositories = mapOf(gofileRepository.id to gofileRepository),
        settingsRepository = settingsRepository
    )

    private val progressTracker = DefaultUploadProgressTracker(uploadSelector)
    private val executor = DefaultUploadExecutor(progressTracker, logger)
    private val retryPolicy = DefaultUploadRetryPolicy(logger)

    private val platformUtils = DefaultPlatformUtils(randomProvider)

    private val batchingLogic = BatchingLogic(logger)
    private val batchFilesUseCase = BatchFilesUseCase(batchingLogic)

    private val zipCreator = Zip4jZipCreator(logger, fileSystem, dispatcherProvider)
    private val archiveRepository = DefaultArchiveRepository(zipCreator, fileSystem)
    private val createArchiveUseCase = CreateArchiveUseCase(archiveRepository, platformUtils)

    private val uploadBatchUseCase = UploadBatchUseCase(torService, logger, executor, retryPolicy)
    private val cleanupUseCase = CleanupUseCase(fileSystem)

    private lateinit var uploadArchiveUseCase: UploadArchiveUseCase

    @Before
    fun setup() {
        uploadArchiveUseCase = UploadArchiveUseCase(
            fileSystem = fileSystem,
            clock = clock,
            logger = logger,
            systemInfo = systemInfo,
            batchFilesUseCase = batchFilesUseCase,
            createArchiveUseCase = createArchiveUseCase,
            uploadBatchUseCase = uploadBatchUseCase,
            cleanupUseCase = cleanupUseCase,
            resourceProvider = resourceProvider,
            progressTracker = progressTracker,
            dispatcherProvider = dispatcherProvider
        )

        every { randomProvider.getRandom() } returns Random(42)
        every { resourceProvider.getMaxUploadRetries() } returns 3
        every { settingsRepository.getSelectedUploadServiceId() } returns gofileRepository.id
    }

    @Test
    fun `successful upload pipeline flow`() = runTest(testDispatcher) {
        // Prepare files in fake file system
        fileSystem.addFileOfSize("/data/file1.txt", 1024)
        fileSystem.addFileOfSize("/data/file2.txt", 1024)

        val scanResult = ScanResult(
            readableFiles = listOf(
                FileEntry("/data/file1.txt", 1024, "source"),
                FileEntry("/data/file2.txt", 1024, "source")
            )
        )

        val parameters = UploadParameters(
            customBatchSizeMb = "1", // 1MB batch size, so both files fit in one batch
            proxySpecification = "",
            shouldUseTor = false,
            shouldUploadZips = true,
            shouldUploadReadableList = false,
            shouldUploadUnreadableList = false,
            shouldUploadExcludedList = false,
            shouldUploadMissingList = false,
            shouldUploadSymlinkList = false,
            shouldUploadGetprop = false,
            shouldUploadAppLogs = false,
            zipEncryption = ZipEncryption.NONE,
            selectedService = gofileRepository,
            maxBatches = 0
        )

        gateway.result = GatewayResult.Success(
            GofileUploadDomainModel(
                "https://gofile.io/d/test",
                "token",
                "folder"
            )
        )

        uploadArchiveUseCase.execute(parameters, scanResult).test {
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))

            // UploadBatchUseCase calls retryPolicy which calls onStatusUpdate
            val status = awaitItem()
            Truth.assertThat(status).isInstanceOf(UploadWorkflowStatus.UploadingBatch::class.java)
            val uploadingBatch = status as UploadWorkflowStatus.UploadingBatch
            Truth.assertThat(uploadingBatch.attempt).isEqualTo(1)

            // Progress updates from gateway
            Truth.assertThat(awaitItem())
                .isInstanceOf(UploadWorkflowStatus.Progress::class.java) // 0%
            Truth.assertThat(awaitItem())
                .isInstanceOf(UploadWorkflowStatus.Progress::class.java) // 100%

            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))

            val success = awaitItem() as UploadWorkflowStatus.Success
            Truth.assertThat(success.downloadUrl).isEqualTo("https://gofile.io/d/test")
            Truth.assertThat(success.uploadedZips).isEqualTo(1)

            awaitComplete()
        }

        // Regression test for logging
        logger.assertLogExists("I", "UploadArchiveUseCase", "succeededUploads: 1, totalUploads: 1")
    }

    @Test
    fun `pipeline retries on transient error and succeeds`() = runTest(testDispatcher) {
        fileSystem.addFileOfSize("/data/file1.txt", 1024)
        val scanResult =
            ScanResult(readableFiles = listOf(FileEntry("/data/file1.txt", 1024, "source")))
        val parameters = createSimpleParameters()

        // Mock gateway to fail once and then succeed
        val failFlow = flowOf(
            GatewayResult.Progress(0, 1024),
            GatewayResult.Error("Network Error")
        )
        val successFlow = flowOf(
            GatewayResult.Progress(0, 1024),
            GatewayResult.Progress(1024, 1024),
            GatewayResult.Success(
                GofileUploadDomainModel(
                    "https://gofile.io/d/test",
                    "token",
                    "folder"
                )
            )
        )

        val mockGateway = mockk<GofileGateway>(relaxed = true)
        every { mockGateway.upload(any(), any(), any(), any()) } returnsMany listOf(
            failFlow,
            successFlow
        )

        // Re-setup with mock gateway
        val customRepo = GofileUploadRepository(mockGateway)
        val customUploadSelector = DefaultUploadSelector(
            repositories = mapOf(customRepo.id to customRepo),
            settingsRepository = settingsRepository
        )
        val customProgressTracker = DefaultUploadProgressTracker(customUploadSelector)
        val customExecutor = DefaultUploadExecutor(customProgressTracker, logger)
        val customUploadBatchUseCase =
            UploadBatchUseCase(torService, logger, customExecutor, retryPolicy)

        val customUseCase = UploadArchiveUseCase(
            fileSystem = fileSystem,
            clock = clock,
            logger = logger,
            systemInfo = systemInfo,
            batchFilesUseCase = batchFilesUseCase,
            createArchiveUseCase = createArchiveUseCase,
            uploadBatchUseCase = customUploadBatchUseCase,
            cleanupUseCase = cleanupUseCase,
            resourceProvider = resourceProvider,
            progressTracker = customProgressTracker,
            dispatcherProvider = dispatcherProvider
        )

        customUseCase.execute(parameters.copy(selectedService = customRepo), scanResult).test {
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))

            // Attempt 1
            val status1 = awaitItem() as UploadWorkflowStatus.UploadingBatch
            Truth.assertThat(status1.attempt).isEqualTo(1)
            Truth.assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Progress::class.java)
            // Error logged but not emitted as terminal yet because of retry policy

            // Attempt 2
            val status2 = awaitItem() as UploadWorkflowStatus.UploadingBatch
            Truth.assertThat(status2.attempt).isEqualTo(2)
            Truth.assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Progress::class.java)
            Truth.assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Progress::class.java)

            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))
            Truth.assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Success::class.java)
            awaitComplete()
        }

        // Regression test for logging retries
        logger.assertLogExists("D", "UploadRetryPolicy", "withRetry: attempt 1")
        logger.assertLogExists("E", "UploadRetryPolicy", "Attempt 1 of 3 failed")
        logger.assertLogExists("D", "UploadRetryPolicy", "withRetry: attempt 2")
    }

    @Test
    fun `pipeline fails after exhausting retries`() = runTest(testDispatcher) {
        fileSystem.addFileOfSize("/data/file1.txt", 1024)
        val scanResult =
            ScanResult(readableFiles = listOf(FileEntry("/data/file1.txt", 1024, "source")))
        val parameters = createSimpleParameters()

        val failFlow = flowOf(GatewayResult.Error("Persistent Error"))
        val mockGateway = mockk<GofileGateway>(relaxed = true)
        every { mockGateway.upload(any(), any(), any(), any()) } returns failFlow

        // Re-setup (similar to above, could refactor but keeping it simple for now)
        val customRepo = GofileUploadRepository(mockGateway)
        val customUploadSelector =
            DefaultUploadSelector(mapOf(customRepo.id to customRepo), settingsRepository)
        val customProgressTracker = DefaultUploadProgressTracker(customUploadSelector)
        val customExecutor = DefaultUploadExecutor(customProgressTracker, logger)
        val customUploadBatchUseCase =
            UploadBatchUseCase(torService, logger, customExecutor, retryPolicy)

        val customUseCase = UploadArchiveUseCase(
            fileSystem, clock, logger, systemInfo, batchFilesUseCase,
            createArchiveUseCase, customUploadBatchUseCase, cleanupUseCase, resourceProvider,
            customProgressTracker, dispatcherProvider
        )

        customUseCase.execute(parameters.copy(selectedService = customRepo), scanResult).test {
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))

            // Retries: 1, 2, 3
            repeat(3) {
                Truth.assertThat(awaitItem())
                    .isInstanceOf(UploadWorkflowStatus.UploadingBatch::class.java)
            }

            val error = awaitItem() as UploadWorkflowStatus.Error
            Truth.assertThat((error.error as UploadError.MissingDownloadURL).message)
                .isEqualTo("Empty URL")
            awaitComplete()
        }
    }

    @Test
    fun `progress tracker accumulates bytes across batches`() = runTest(testDispatcher) {
        // Two files, each in its own batch
        fileSystem.addFileOfSize("/data/file1.txt", 1024)
        fileSystem.addFileOfSize("/data/file2.txt", 1024)

        val scanResult = ScanResult(
            readableFiles = listOf(
                FileEntry("/data/file1.txt", 1024, "source"),
                FileEntry("/data/file2.txt", 1024, "source")
            )
        )

        // Force partitioning into 2 batches
        every { resourceProvider.getMinBatchSizeMb() } returns 1
        val parameters = createSimpleParameters().copy(
            customBatchSizeMb = "0" // This might be tricky depending on BatchingLogic, let's assume we can force it
        )

        // Mocking BatchFilesUseCase directly to be sure
        val mockBatchFilesUseCase = mockk<BatchFilesUseCase>()
        every {
            mockBatchFilesUseCase.execute(
                any(),
                any(),
                any(),
                any()
            )
        } returns listOf(listOf("/data/file1.txt"), listOf("/data/file2.txt"))

        val customUseCase = UploadArchiveUseCase(
            fileSystem, clock, logger, systemInfo, mockBatchFilesUseCase,
            createArchiveUseCase, uploadBatchUseCase, cleanupUseCase, resourceProvider,
            progressTracker, dispatcherProvider
        )

        gateway.result = GatewayResult.Success(
            GofileUploadDomainModel(
                "https://gofile.io/d/test",
                "token",
                "folder"
            )
        )

        customUseCase.execute(parameters, scanResult).test {
            // Consume until Success
            while (awaitItem() !is UploadWorkflowStatus.Success) { /* keep going */
            }
            awaitComplete()
        }

        // Verify total bytes tracked.
        // Note: ZIP overhead might change the size, but Zip4jZipCreator uses the source file size in ZipParameters entrySize.
        // DefaultUploadExecutor increments progressTracker based on result.bytesWritten from gateway.
        // FakeGofileGateway emits Progress(0, 100) and Progress(100, 100).
        // Wait, FakeGofileGateway hardcodes 100L as total.

        // Let's check how many bytes were tracked.
        // 2 batches * 100 bytes (from FakeGofileGateway) = 200 bytes.
        Truth.assertThat(progressTracker.totalUploadedBytes.value).isEqualTo(200L)
    }

    @Test
    fun `pipeline handles encryption correctly`() = runTest(testDispatcher) {
        fileSystem.addFileOfSize("/data/file1.txt", 1024)
        val scanResult =
            ScanResult(readableFiles = listOf(FileEntry("/data/file1.txt", 1024, "source")))
        val parameters = createSimpleParameters().copy(
            zipEncryption = ZipEncryption.STANDARD
        )

        gateway.result = GatewayResult.Success(
            GofileUploadDomainModel(
                "https://gofile.io/d/test",
                "token",
                "folder"
            )
        )

        uploadArchiveUseCase.execute(parameters, scanResult).test {
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.PartitioningBatches)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))

            Truth.assertThat(awaitItem())
                .isInstanceOf(UploadWorkflowStatus.UploadingBatch::class.java)
            Truth.assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Progress::class.java)
            Truth.assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Progress::class.java)
            Truth.assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))

            val success = awaitItem() as UploadWorkflowStatus.Success
            Truth.assertThat(success.password).isNotNull()
            Truth.assertThat(success.password?.length).isEqualTo(16)
            awaitComplete()
        }
    }

    private fun createSimpleParameters() = UploadParameters(
        customBatchSizeMb = "1",
        proxySpecification = "",
        shouldUseTor = false,
        shouldUploadZips = true,
        shouldUploadReadableList = false,
        shouldUploadUnreadableList = false,
        shouldUploadExcludedList = false,
        shouldUploadMissingList = false,
        shouldUploadSymlinkList = false,
        shouldUploadGetprop = false,
        shouldUploadAppLogs = false,
        zipEncryption = ZipEncryption.NONE,
        selectedService = gofileRepository,
        maxBatches = 0
    )
}