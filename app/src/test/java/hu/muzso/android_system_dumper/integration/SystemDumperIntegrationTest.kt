package hu.muzso.android_system_dumper.integration

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.DefaultPlatformUtils
import hu.muzso.android_system_dumper.common.RandomProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeGofileGateway
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeScanRepository
import hu.muzso.android_system_dumper.domain.fixtures.FakeTorServiceController
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.network.BatchingLogic
import hu.muzso.android_system_dumper.network.DefaultArchiveGenerator
import hu.muzso.android_system_dumper.network.upload.DefaultUploadExecutor
import hu.muzso.android_system_dumper.network.upload.DefaultUploadProgressTracker
import hu.muzso.android_system_dumper.network.upload.DefaultUploadRetryPolicy
import hu.muzso.android_system_dumper.network.upload.DefaultUploadSelector
import hu.muzso.android_system_dumper.network.upload.GofileUploadRepository
import hu.muzso.android_system_dumper.network.upload.TorChecker
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import hu.muzso.android_system_dumper.network.upload.gateway.GofileUploadDomainModel
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.repository.SettingsRepository
import hu.muzso.android_system_dumper.scan.DefaultArchiveRepository
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import hu.muzso.android_system_dumper.usecase.StartScanUseCase
import hu.muzso.android_system_dumper.usecase.UploadArchiveUseCase
import hu.muzso.android_system_dumper.usecase.UploadBatchUseCase
import hu.muzso.android_system_dumper.zip.Zip4jZipCreator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class SystemDumperIntegrationTest {

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

    private val torChecker = mockk<TorChecker>(relaxed = true)
    private val uploadBatchUseCase = UploadBatchUseCase(torService, torChecker, logger, executor, retryPolicy)
    private val cleanupUseCase = CleanupUseCase(fileSystem)

    private val scanRepository = FakeScanRepository()
    private val startScanUseCase = StartScanUseCase(scanRepository)
    
    private val platformUtils = DefaultPlatformUtils(randomProvider)
    private val batchingLogic = BatchingLogic(logger)
    private val batchFilesUseCase = BatchFilesUseCase(batchingLogic)
    private val zipCreator = Zip4jZipCreator(logger, fileSystem, dispatcherProvider)
    private val archiveRepository = DefaultArchiveRepository(zipCreator, fileSystem)
    private val createArchiveUseCase = CreateArchiveUseCase(archiveRepository, platformUtils)

    private lateinit var uploadArchiveUseCase: UploadArchiveUseCase

    @Before
    fun setup() {
        val archiveGenerator = DefaultArchiveGenerator(
            fileSystem = fileSystem,
            clock = clock,
            logger = logger,
            systemInfo = systemInfo,
            batchFilesUseCase = batchFilesUseCase,
            createArchiveUseCase = createArchiveUseCase,
            cleanupUseCase = cleanupUseCase
        )
        uploadArchiveUseCase = UploadArchiveUseCase(
            clock = clock,
            logger = logger,
            uploadBatchUseCase = uploadBatchUseCase,
            cleanupUseCase = cleanupUseCase,
            resourceProvider = resourceProvider,
            progressTracker = progressTracker,
            dispatcherProvider = dispatcherProvider,
            archiveGenerator = archiveGenerator
        )

        every { randomProvider.getRandom() } returns Random(42)
        every { resourceProvider.getMaxUploadRetries() } returns 3
        every { settingsRepository.getSelectedUploadServiceId() } returns gofileRepository.id
    }

    @Test
    fun `full end-to-end simulation flow`() = runTest(testDispatcher) {
        // 1. Simulate Scan
        scanRepository.setStatuses(listOf(ScanStatus.RUNNING, ScanStatus.FINISHED))
        val scanResult = ScanResult(
            readableFiles = listOf(FileEntry("/data/log.txt", 100, "scan"))
        )
        scanRepository.updateResult(scanResult)
        fileSystem.addFileOfSize("/data/log.txt", 100)

        startScanUseCase.execute(false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        // 2. Simulate Upload
        gateway.result = GatewayResult.Success(
            GofileUploadDomainModel("https://gofile.io/d/e2e", "token", "folder")
        )

        val parameters = UploadParameters(
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
            maxBatches = 0,
            useDoubleZipping = false
        )

        uploadArchiveUseCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.TotalPlannedUploads(1))
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.ArchivingBatch(1, 1))
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.UploadingBatch::class.java)
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Progress::class.java)
            assertThat(awaitItem()).isInstanceOf(UploadWorkflowStatus.Progress::class.java)
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.SuccessfulUploads(1))
            
            val finalStatus = awaitItem() as UploadWorkflowStatus.Success
            assertThat(finalStatus.downloadUrl).isEqualTo("https://gofile.io/d/e2e")
            awaitComplete()
        }
    }
}
