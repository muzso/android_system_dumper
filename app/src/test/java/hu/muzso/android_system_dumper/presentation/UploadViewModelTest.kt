package hu.muzso.android_system_dumper.presentation

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeScanRepository
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.UiMessenger
import hu.muzso.android_system_dumper.upload.network.UploadRepository
import hu.muzso.android_system_dumper.usecase.GenerateQrUseCase
import hu.muzso.android_system_dumper.usecase.UploadArchiveUseCase
import hu.muzso.android_system_dumper.usecase.ValidateUploadUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {

    private val resourceProvider = mockk<ResourceProvider>(relaxed = true)
    private val uiMessenger = mockk<UiMessenger>(relaxed = true)
    private val platformUtils = mockk<PlatformUtils>(relaxed = true)
    private val repository = FakeScanRepository()
    private val dispatcherProvider = mockk<DispatcherProvider>()
    private val logger = FakeFileLogger()
    private val validateUploadUseCase = mockk<ValidateUploadUseCase>()
    private val uploadArchiveUseCase = mockk<UploadArchiveUseCase>()
    private val generateQrUseCase = mockk<GenerateQrUseCase>()

    private lateinit var viewModel: UploadViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { dispatcherProvider.io() } returns testDispatcher
        every { dispatcherProvider.default() } returns testDispatcher
        
        every { resourceProvider.getMinBatchSizeMb() } returns 1
        every { resourceProvider.getMaxBatchSizeMb() } returns 4000
        
        every { resourceProvider.getString(any<Int>()) } returns "mock_string"
        every { resourceProvider.getString(any<Int>(), *anyVararg()) } answers {
            val args = it.invocation.args[1] as Array<*>
            "mock_string: ${args.joinToString()}"
        }
    }

    @Test
    fun `ResetResults intent resets the UI state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(
            "downloadUrl" to "https://test.com",
            "generatedPassword" to "secret"
        ))
        createViewModel(savedStateHandle)
        assertThat(viewModel.uiState.value.downloadUrl).isEqualTo("https://test.com")

        viewModel.processIntent(UploadViewModel.Intent.ResetResults)
        testScheduler.runCurrent()

        assertThat(viewModel.uiState.value.downloadUrl).isNull()
        assertThat(viewModel.uiState.value.generatedPassword).isNull()
    }

    @Test
    fun `GenerateQr and ClearQr intents update state`() = runTest {
        createViewModel()
        val mockBitmap = mockk<Bitmap>()
        every { generateQrUseCase.execute("test-text", any()) } returns mockBitmap

        viewModel.processIntent(UploadViewModel.Intent.GenerateQr("test-text"))
        testScheduler.runCurrent()
        assertThat(viewModel.uiState.value.qrBitmap).isEqualTo(mockBitmap)

        viewModel.processIntent(UploadViewModel.Intent.ClearQr)
        testScheduler.runCurrent()
        assertThat(viewModel.uiState.value.qrBitmap).isNull()
    }

    @Test
    fun `formatBytes delegates to platformUtils`() = runTest {
        createViewModel()
        every { platformUtils.formatBytes(1024L) } returns "1 KB"
        
        val result = viewModel.formatBytes(1024L)
        
        assertThat(result).isEqualTo("1 KB")
    }

    @Test
    fun `startUploading triggers onFatalError on validation failure`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        val validationError = ValidateUploadUseCase.ValidationResult.Error.InvalidBatchSize(1, 100)
        every { validateUploadUseCase.execute(any()) } returns validationError

        var fatalErrorReceived: String? = null
        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "500",
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
            selectedService = uploadRepo,
            onFatalError = { fatalErrorReceived = it }
        )

        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.runCurrent()

        assertThat(fatalErrorReceived).isNotNull()
        assertThat(viewModel.uiState.value.isUploading).isFalse()
    }

    @Test
    fun `startUploading shows toast if scan result is empty`() = runTest {
        createViewModel()
        repository.updateResult(ScanResult(readableFiles = emptyList()))
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success

        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = mockk(relaxed = true),
            onFatalError = {}
        )

        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.runCurrent()

        verify { uiMessenger.showShortToast(any()) }
        assertThat(viewModel.uiState.value.isUploading).isFalse()
    }

    @Test
    fun `PartialSuccess status updates state correctly`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)
        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        
        val statusFlow = flowOf(UploadWorkflowStatus.PartialSuccess(
            downloadUrl = "https://download.com",
            uploadedZips = 1,
            totalZips = 2,
            failedZips = 1,
            totalBytes = 50L,
            runtimeSeconds = 30L,
            password = "pwd"
        ))
        every { uploadArchiveUseCase.execute(any(), any()) } returns statusFlow

        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = uploadRepo,
            onFatalError = {}
        )
        
        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertThat(state.uploadedZips).isEqualTo(1)
        assertThat(state.totalZips).isEqualTo(2)
        assertThat(state.downloadUrl).isEqualTo("https://download.com")
    }

    @Test
    fun `Aborted status updates state correctly`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)
        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        
        val statusFlow = flowOf(UploadWorkflowStatus.Aborted)
        every { uploadArchiveUseCase.execute(any(), any()) } returns statusFlow

        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = uploadRepo,
            onFatalError = {}
        )
        
        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.isUploading).isFalse()
        assertThat(viewModel.uiState.value.uploadStatusText).contains("mock_string")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) {
        viewModel = UploadViewModel(
            resourceProvider, uiMessenger, platformUtils,
            savedStateHandle, repository, logger,
            validateUploadUseCase, uploadArchiveUseCase, generateQrUseCase
        )
    }

    @Test
    fun `initial state is correct`() = runTest {
        createViewModel()
        val state = viewModel.uiState.value
        assertThat(state.isUploading).isFalse()
        assertThat(state.uploadedZips).isEqualTo(0)
        assertThat(state.totalZips).isEqualTo(0)
        assertThat(state.uploadStatusText).isEqualTo("")
    }

    @Test
    fun `SavedStateHandle restores state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(
            "downloadUrl" to "https://test.com",
            "generatedPassword" to "secret",
            "uploadStatusText" to "Restored"
        ))
        createViewModel(savedStateHandle)
        
        val state = viewModel.uiState.value
        assertThat(state.downloadUrl).isEqualTo("https://test.com")
        assertThat(state.generatedPassword).isEqualTo("secret")
        assertThat(state.uploadStatusText).isEqualTo("Restored")
    }

    @Test
    fun `stopUploading cancels the upload job`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)
        
        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))
        
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        every { uploadArchiveUseCase.execute(any(), any()) } returns flowOf(UploadWorkflowStatus.Preparing)

        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = uploadRepo,
            onFatalError = {}
        )
        
        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.runCurrent()
        
        assertThat(viewModel.uiState.value.isUploading).isTrue()
        
        viewModel.processIntent(UploadViewModel.Intent.StopUploading)
        testScheduler.runCurrent()
        
        assertThat(viewModel.uiState.value.isUploading).isFalse()
    }

    @Test
    fun `toggleUploading logs start and service name`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.name } returns "Gofile"
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)

        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))

        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        every { uploadArchiveUseCase.execute(any(), any()) } returns flowOf(UploadWorkflowStatus.Preparing)

        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = uploadRepo,
            onFatalError = {}
        )

        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.runCurrent()

        logger.assertLogExists("I", "UploadViewModel", "toggleUploading: isUploading=false")
        logger.assertLogExists("I", "UploadViewModel", "startUploading: parameters=")
    }

    @Test
    fun `uiState updates totalZips and uploadedZips correctly from status emissions`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)

        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))
        
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        
        val statusFlow = flow {
            emit(UploadWorkflowStatus.Preparing)
            emit(UploadWorkflowStatus.TotalPlannedUploads(5))
            emit(UploadWorkflowStatus.SuccessfulUploads(1))
            emit(UploadWorkflowStatus.ArchivingBatch(2, 3)) // i.e. 2nd batch of 3 (ZIP-only count)
            emit(UploadWorkflowStatus.SuccessfulUploads(2))
        }
        every { uploadArchiveUseCase.execute(any(), any()) } returns statusFlow

        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = uploadRepo,
            onFatalError = {}
        )
        
        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.advanceUntilIdle()
        
        // Final state should have totalZips = 5 and uploadedZips = 2
        assertThat(viewModel.uiState.value.totalZips).isEqualTo(5)
        assertThat(viewModel.uiState.value.uploadedZips).isEqualTo(2)
    }

    @Test
    fun `successful upload updates state with results`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)
        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        
        val statusFlow = flowOf(UploadWorkflowStatus.Success(
            downloadUrl = "https://download.com",
            uploadedZips = 1,
            totalZips = 1,
            totalBytes = 100L,
            runtimeSeconds = 60L,
            password = "pwd"
        ))
        every { uploadArchiveUseCase.execute(any(), any()) } returns statusFlow

        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = uploadRepo,
            onFatalError = {}
        )
        
        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertThat(state.isUploading).isFalse()
        assertThat(state.downloadUrl).isEqualTo("https://download.com")
        assertThat(state.generatedPassword).isEqualTo("pwd")
        assertThat(state.uploadedZips).isEqualTo(1)
    }

    @Test
    fun `upload error updates state and triggers callback`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)
        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        
        val error = UploadError.NetworkError("No connection")
        val statusFlow = flowOf(UploadWorkflowStatus.Error(error, 0L, 0L))
        every { uploadArchiveUseCase.execute(any(), any()) } returns statusFlow

        var fatalErrorReceived: String? = null
        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
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
            selectedService = uploadRepo,
            onFatalError = { fatalErrorReceived = it }
        )
        
        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.isUploading).isFalse()
        assertThat(fatalErrorReceived).contains("No connection")
    }

    @Test
    fun `startUploading aborts when Tor check fails`() = runTest {
        createViewModel()
        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        coEvery { uploadRepo.torCheck() } returns false
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)
        repository.updateResult(ScanResult(readableFiles = listOf(FileEntry("/test", 100L, "source"))))
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success
        every { resourceProvider.getString(R.string.traffic_doesnt_go_through_tor_error) } returns "Tor check failed"

        var fatalErrorReceived: String? = null
        val settings = UploadViewModel.UploadSettings(
            customBatchSizeMb = "200",
            proxySpecification = "9050",
            shouldUseTor = true,
            shouldUploadZips = true,
            shouldUploadReadableList = false,
            shouldUploadUnreadableList = false,
            shouldUploadExcludedList = false,
            shouldUploadMissingList = false,
            shouldUploadSymlinkList = false,
            shouldUploadGetprop = false,
            shouldUploadAppLogs = false,
            zipEncryption = ZipEncryption.NONE,
            selectedService = uploadRepo,
            onFatalError = { fatalErrorReceived = it }
        )
        
        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testScheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.isUploading).isFalse()
        assertThat(fatalErrorReceived).isEqualTo("Tor check failed")
        verify(exactly = 0) { uploadArchiveUseCase.execute(any(), any()).run { } }
    }
}
