package hu.muzso.android_system_dumper.network

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeScanRepository
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.UiMessenger
import hu.muzso.android_system_dumper.presentation.UploadViewModel
import hu.muzso.android_system_dumper.usecase.GenerateQrUseCase
import hu.muzso.android_system_dumper.usecase.UploadArchiveUseCase
import hu.muzso.android_system_dumper.usecase.ValidateUploadUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadCancellationTest {

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

        every { resourceProvider.getString(R.string.aborted) } returns "Aborted"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stopUploading sets state to aborted even if flow is active`() = runTest {
        viewModel = UploadViewModel(
            resourceProvider, uiMessenger, platformUtils,
            SavedStateHandle(), repository, logger,
            validateUploadUseCase, uploadArchiveUseCase, generateQrUseCase
        )

        val uploadRepo = mockk<UploadRepository>(relaxed = true)
        every { uploadRepo.totalUploadedBytes } returns MutableStateFlow(0L)
        repository.updateResult(
            ScanResult(
                readableFiles = listOf(
                    FileEntry(
                        "/test",
                        100L,
                        "source"
                    )
                )
            )
        )
        every { validateUploadUseCase.execute(any()) } returns ValidateUploadUseCase.ValidationResult.Success

        val statusFlow = MutableSharedFlow<UploadWorkflowStatus>()
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
            useDoubleZipping = false,
            selectedService = uploadRepo,
            onFatalError = {}
        )

        viewModel.processIntent(UploadViewModel.Intent.ToggleUploading(settings))
        testDispatcher.scheduler.runCurrent()
        assertThat(viewModel.uiState.value.isUploading).isTrue()

        viewModel.processIntent(UploadViewModel.Intent.StopUploading)
        testDispatcher.scheduler.runCurrent()

        assertThat(viewModel.uiState.value.isUploading).isFalse()
        assertThat(viewModel.uiState.value.uploadStatusText).isEqualTo("Aborted")
    }
}