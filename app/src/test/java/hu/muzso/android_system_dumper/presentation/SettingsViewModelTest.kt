package hu.muzso.android_system_dumper.presentation

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.config.AppConfig
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.network.upload.HttpClientProvider
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.network.upload.UploadRepositoryManager
import hu.muzso.android_system_dumper.presentation.state.AppState
import hu.muzso.android_system_dumper.repository.IpInfoRepository
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import hu.muzso.android_system_dumper.usecase.StartTorUseCase
import hu.muzso.android_system_dumper.usecase.StopTorUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val clock = FakeClock()
    private val logger = FakeFileLogger()
    private val appConfig = mockk<AppConfig>()
    private val networkUtils = mockk<NetworkUtils>(relaxed = true)
    private lateinit var httpClientProvider: HttpClientProvider
    private val uploadRepositoryManager = mockk<UploadRepositoryManager>(relaxed = true)
    private val startTorUseCase = mockk<StartTorUseCase>(relaxed = true)
    private val stopTorUseCase = mockk<StopTorUseCase>(relaxed = true)
    private val loadExcludeListUseCase = mockk<LoadExcludeListUseCase>(relaxed = true)
    private val getSeedPathsUseCase = mockk<GetSeedPathsUseCase>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private val ipInfoRepository = mockk<IpInfoRepository>()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { appConfig.networkTimeoutMs } returns 30000L
        httpClientProvider = HttpClientProvider(appConfig)
        
        every { loadExcludeListUseCase.execute() } returns listOf("/excluded")
        every { getSeedPathsUseCase.execute() } returns listOf("/seed")
        
        val mockRepo = mockk<UploadRepository>(relaxed = true)
        every { ipInfoRepository.getAvailableSources() } returns listOf("https://test.source")
        every { mockRepo.name } returns "MockRepo"
        every { mockRepo.id } returns "mock_id"
        every { uploadRepositoryManager.getRepositories() } returns listOf(mockRepo)
        every { uploadRepositoryManager.getSelectedRepository() } returns mockRepo
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) {
        viewModel = SettingsViewModel(
            savedStateHandle,
            clock,
            logger,
            networkUtils,
            httpClientProvider,
            uploadRepositoryManager,
            startTorUseCase,
            stopTorUseCase,
            loadExcludeListUseCase,
            getSeedPathsUseCase,
            ipInfoRepository
        )
    }

    @Test
    fun `initial state is correct`() = runTest {
        createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.customBatchSizeMb).isEqualTo("200")
        assertThat(state.shouldUseTor).isTrue()
        assertThat(state.zipEncryption).isEqualTo(ZipEncryption.STANDARD)
        assertThat(state.maxUploadRetries).isEqualTo("5")
        assertThat(state.exclusionList).containsExactly("/excluded")
        assertThat(state.discoveryRoots).containsExactly("/seed")
        
        // Tor service should be started by default if shouldUseTor is true
        verify { startTorUseCase.execute(any()) }
        collectJob.cancel()
    }

    @Test
    fun `SavedStateHandle restores state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(
            "customBatchSizeMb" to "500",
            "shouldUseTor" to false,
            "zipEncryption" to ZipEncryption.AES,
            "maxUploadRetries" to "10"
        ))
        createViewModel(savedStateHandle)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.customBatchSizeMb).isEqualTo("500")
        assertThat(state.shouldUseTor).isFalse()
        assertThat(state.zipEncryption).isEqualTo(ZipEncryption.AES)
        assertThat(state.maxUploadRetries).isEqualTo("10")
        
        // Tor service should NOT be started if shouldUseTor is false in SavedStateHandle
        verify(exactly = 0) { startTorUseCase.execute(any()) }
        collectJob.cancel()
    }

    @Test
    fun `SetCustomBatchSizeMb updates state`() = runTest {
        createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.processIntent(SettingsViewModel.Intent.SetCustomBatchSizeMb("100"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.customBatchSizeMb).isEqualTo("100")
        collectJob.cancel()
    }

    @Test
    fun `SetMaxUploadRetries updates state`() = runTest {
        createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.processIntent(SettingsViewModel.Intent.SetMaxUploadRetries("3"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.maxUploadRetries).isEqualTo("3")
        collectJob.cancel()
    }

    @Test
    fun `SetShouldUseTor toggles Tor service and updates proxy`() = runTest {
        createViewModel(SavedStateHandle(mapOf("shouldUseTor" to false)))
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        
        // Advance clock to allow toggle (init sets lastTorToggleTime)
        clock.tick(2000)

        // Toggle ON
        viewModel.processIntent(SettingsViewModel.Intent.SetShouldUseTor(true))
        advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.shouldUseTor).isTrue()
        assertThat(viewModel.uiState.value.proxySpecification).isEqualTo("9050")
        verify { startTorUseCase.execute(any()) }

        // Advance clock to allow another toggle (throttle is 1000ms)
        clock.tick(2000)

        // Toggle OFF
        viewModel.processIntent(SettingsViewModel.Intent.SetShouldUseTor(false))
        advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.shouldUseTor).isFalse()
        assertThat(viewModel.uiState.value.proxySpecification).isEqualTo("")
        verify { stopTorUseCase.execute() }
        collectJob.cancel()
    }

    @Test
    fun `SetShouldUseTor is throttled`() = runTest {
        createViewModel(SavedStateHandle(mapOf("shouldUseTor" to false)))
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        
        // Advance clock to allow FIRST toggle
        clock.tick(2000)

        viewModel.processIntent(SettingsViewModel.Intent.SetShouldUseTor(true))
        advanceUntilIdle()
        verify(exactly = 1) { startTorUseCase.execute(any()) }

        // Toggle again immediately - should be ignored (clock not ticked)
        viewModel.processIntent(SettingsViewModel.Intent.SetShouldUseTor(false))
        advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.shouldUseTor).isTrue() // Still true
        verify(exactly = 0) { stopTorUseCase.execute() }
        collectJob.cancel()
    }

    @Test
    fun `Navigation intents update appState`() = runTest {
        createViewModel()
        
        viewModel.processIntent(SettingsViewModel.Intent.NavigateToHelp)
        assertThat(viewModel.appState.value).isEqualTo(AppState.HelpScreen)

        viewModel.processIntent(SettingsViewModel.Intent.NavigateToQrCode("test_qr"))
        assertThat(viewModel.appState.value).isEqualTo(AppState.QrCodeScreen("test_qr", AppState.HelpScreen))

        viewModel.processIntent(SettingsViewModel.Intent.NavigateTo(AppState.HelpScreen))
        assertThat(viewModel.appState.value).isEqualTo(AppState.HelpScreen)

        viewModel.processIntent(SettingsViewModel.Intent.NavigateToMain)
        assertThat(viewModel.appState.value).isEqualTo(AppState.MainScreen)
    }

    @Test
    fun `SetFatalError updates state`() = runTest {
        createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.processIntent(SettingsViewModel.Intent.SetFatalError("Something went wrong"))
        advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.fatalError).isEqualTo("Something went wrong")
        collectJob.cancel()
    }

    @Test
    fun `SelectService updates state and repository manager`() = runTest {
        createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val mockRepo = mockk<UploadRepository>(relaxed = true)
        every { mockRepo.id } returns "new_id"
        every { mockRepo.name } returns "NewRepo"

        viewModel.processIntent(SettingsViewModel.Intent.SelectService(mockRepo))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedService).isEqualTo(mockRepo)
        verify { uploadRepositoryManager.selectRepository("new_id") }
        collectJob.cancel()
    }

    @Test
    fun `Tor service start failure is logged`() = runTest {
        every { startTorUseCase.execute(any()) } throws RuntimeException("Tor failed")
        
        createViewModel() // init calls startTorService if shouldUseTor is default (true)
        advanceUntilIdle()
        
        logger.assertLogExists("E", "SettingsViewModel", "Failed to start CustomTorService")
    }

    @Test
    fun `setting zipEncryption to NONE disables useDoubleZipping`() = runTest {
        createViewModel(SavedStateHandle(mapOf("useDoubleZipping" to true, "zipEncryption" to ZipEncryption.AES)))
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.zipEncryption).isEqualTo(ZipEncryption.AES)
        assertThat(viewModel.uiState.value.useDoubleZipping).isTrue()

        viewModel.processIntent(SettingsViewModel.Intent.SetZipEncryption(ZipEncryption.NONE))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.zipEncryption).isEqualTo(ZipEncryption.NONE)
        assertThat(viewModel.uiState.value.useDoubleZipping).isFalse()
        collectJob.cancel()
    }

    @Test
    fun `SetUseDoubleZipping is ignored when zipEncryption is NONE`() = runTest {
        createViewModel(SavedStateHandle(mapOf("zipEncryption" to ZipEncryption.NONE, "useDoubleZipping" to false)))
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.processIntent(SettingsViewModel.Intent.SetUseDoubleZipping(true))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.useDoubleZipping).isFalse()
        collectJob.cancel()
    }

    @Test
    fun `SetUseDoubleZipping works when zipEncryption is STANDARD`() = runTest {
        createViewModel(SavedStateHandle(mapOf("zipEncryption" to ZipEncryption.STANDARD, "useDoubleZipping" to false)))
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.processIntent(SettingsViewModel.Intent.SetUseDoubleZipping(true))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.useDoubleZipping).isTrue()
        collectJob.cancel()
    }
}
