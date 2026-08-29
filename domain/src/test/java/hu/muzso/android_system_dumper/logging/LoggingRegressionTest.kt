package hu.muzso.android_system_dumper.logging

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.network.DefaultArchiveGenerator
import hu.muzso.android_system_dumper.network.upload.UploadProgressTracker
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import hu.muzso.android_system_dumper.usecase.UploadArchiveUseCase
import hu.muzso.android_system_dumper.usecase.UploadBatchUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingRegressionTest {

    private val fileSystem = FakeMemoryFileSystem()
    private val clock = mockk<Clock>(relaxed = true)
    private val logger = FakeFileLogger()
    private val systemInfo = mockk<SystemInfo>(relaxed = true)
    private val batchFilesUseCase = mockk<BatchFilesUseCase>()
    private val createArchiveUseCase = mockk<CreateArchiveUseCase>(relaxed = true)
    private val uploadBatchUseCase = mockk<UploadBatchUseCase>(relaxed = true)
    private val cleanupUseCase = mockk<CleanupUseCase>(relaxed = true)
    private val resourceProvider = mockk<ResourceProvider>(relaxed = true)
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
        every { clock.now() } returns Instant.now()
        every { clock.monotonicTime() } returns 0L
    }

    @Test
    fun `successful upload logs summary`() = runTest(testDispatcher) {
        val parameters = createParameters().copy(shouldUploadZips = true)
        val scanResult = ScanResult(readableFiles = listOf(FileEntry("/f1", 100L, "source")))

        every {
            batchFilesUseCase.execute(
                any(),
                any(),
                any(),
                any()
            )
        } returns listOf(listOf("/f1"))
        coEvery {
            createArchiveUseCase.execute(
                any(),
                any(),
                any()
            )
        } returns DomainResult.Success("zip")
        coEvery {
            uploadBatchUseCase.execute(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns DomainResult.Success("url")
        coEvery { uploadRepository.getUrlListUrl() } returns "http://test.com"

        useCase.execute(parameters, scanResult).test {
            while (awaitItem() !is UploadWorkflowStatus.Success) {
                // consume items
            }
            awaitComplete()
        }

        logger.assertLogExists("I", "UploadArchiveUseCase", "succeededUploads: 1, totalUploads: 1")
    }

    @Test
    fun `upload crash logs error`() = runTest(testDispatcher) {
        val parameters = createParameters()
        val scanResult = ScanResult()

        // Trigger an unexpected exception
        coEvery { progressTracker.reset() } throws RuntimeException("Unexpected boom")

        useCase.execute(parameters, scanResult).test {
            assertThat(awaitItem()).isEqualTo(UploadWorkflowStatus.Preparing)
            awaitItem() // Error
            awaitComplete()
        }

        logger.assertErrorLogExists("UploadArchiveUseCase", "Upload process crashed")
    }

    private fun createParameters() = UploadParameters(
        customBatchSizeMb = "10",
        proxySpecification = "",
        shouldUseTor = false,
        shouldUploadZips = false,
        shouldUploadReadableList = false,
        shouldUploadUnreadableList = false,
        shouldUploadExcludedList = false,
        shouldUploadMissingList = false,
        shouldUploadSymlinkList = false,
        shouldUploadGetprop = false,
        shouldUploadAppLogs = false,
        zipEncryption = ZipEncryption.NONE,
        selectedService = uploadRepository,
        maxBatches = 0,
        useDoubleZipping = false
    )
}