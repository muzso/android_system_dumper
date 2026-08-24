package hu.muzso.android_system_dumper.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.muzso.android_system_dumper.BuildConfig
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.presentation.state.AppState
import hu.muzso.android_system_dumper.presentation.state.SettingsResult
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.reduce
import hu.muzso.android_system_dumper.repository.IpInfoRepository
import hu.muzso.android_system_dumper.upload.network.HttpClientProvider
import hu.muzso.android_system_dumper.upload.network.UploadRepository
import hu.muzso.android_system_dumper.upload.network.UploadRepositoryManager
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import hu.muzso.android_system_dumper.usecase.StartTorUseCase
import hu.muzso.android_system_dumper.usecase.StopTorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.torproject.jni.TorService
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock,
    private val logger: FileLogger,
    private val networkUtils: NetworkUtils,
    private val httpClientProvider: HttpClientProvider,
    private val uploadRepositoryManager: UploadRepositoryManager,
    private val startTorUseCase: StartTorUseCase,
    private val stopTorUseCase: StopTorUseCase,
    private val loadExcludeListUseCase: LoadExcludeListUseCase,
    private val getSeedPathsUseCase: GetSeedPathsUseCase,
    private val ipInfoRepository: IpInfoRepository
) : ViewModel() {

    sealed class Intent {
        data class NavigateToQrCode(val qrcodeText: String) : Intent()
        object NavigateToMain : Intent()
        object NavigateToHelp : Intent()
        object NavigateToIpInfo : Intent()
        data class SetCustomBatchSizeMb(val size: String) : Intent()
        data class SetProxySpecification(val spec: String) : Intent()
        data class SetShouldUseTor(val value: Boolean) : Intent()
        data class SetShouldUploadZips(val value: Boolean) : Intent()
        data class SetShouldUploadReadableList(val value: Boolean) : Intent()
        data class SetShouldUploadUnreadableList(val value: Boolean) : Intent()
        data class SetShouldUploadExcludedList(val value: Boolean) : Intent()
        data class SetShouldUploadMissingList(val value: Boolean) : Intent()
        data class SetShouldUploadSymlinkList(val value: Boolean) : Intent()
        data class SetShouldUploadGetprop(val value: Boolean) : Intent()
        data class SetShouldUploadAppLogs(val value: Boolean) : Intent()
        data class SetZipEncryption(val value: ZipEncryption) : Intent()
        data class SetIgnoreExcludeList(val value: Boolean) : Intent()
        data class SelectService(val service: UploadRepository) : Intent()
        data class SetSelectedIpSource(val source: String) : Intent()
        data class SetFatalError(val error: String?) : Intent()
    }

    private val _appState = MutableStateFlow<AppState>(AppState.MainScreen)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _ignoreExcludeList = savedStateHandle.getStateFlow("ignoreExcludeList", false)
    private val _customBatchSizeMb = savedStateHandle.getStateFlow("customBatchSizeMb", BuildConfig.DEFAULT_BATCH_SIZE_MB.toString())
    private val _proxySpecification = savedStateHandle.getStateFlow("proxySpecification", "")
    private val _shouldUseTor = savedStateHandle.getStateFlow("shouldUseTor", true)
    private val _shouldUploadZips = savedStateHandle.getStateFlow("shouldUploadZips", true)
    private val _shouldUploadReadableList = savedStateHandle.getStateFlow("shouldUploadReadableList", true)
    private val _shouldUploadUnreadableList = savedStateHandle.getStateFlow("shouldUploadUnreadableList", true)
    private val _shouldUploadExcludedList = savedStateHandle.getStateFlow("shouldUploadExcludedList", true)
    private val _shouldUploadMissingList = savedStateHandle.getStateFlow("shouldUploadMissingList", true)
    private val _shouldUploadSymlinkList = savedStateHandle.getStateFlow("shouldUploadSymlinkList", true)
    private val _shouldUploadGetprop = savedStateHandle.getStateFlow("shouldUploadGetprop", false)
    private val _shouldUploadAppLogs = savedStateHandle.getStateFlow("shouldUploadAppLogs", true)
    private val _zipEncryption = savedStateHandle.getStateFlow("zipEncryption", ZipEncryption.STANDARD)
    private val _selectedIpSource = savedStateHandle.getStateFlow("selectedIpSource", ipInfoRepository.getAvailableSources().first())
    private val _fatalError = MutableStateFlow<String?>(null)

    val services = uploadRepositoryManager.getRepositories().sortedBy { it.name }

    private val _selectedService = MutableStateFlow(uploadRepositoryManager.getSelectedRepository())

    val uiState: StateFlow<SettingsUiState> = combine(
        _customBatchSizeMb, _proxySpecification, _shouldUseTor, _shouldUploadZips,
        _shouldUploadReadableList, _shouldUploadUnreadableList, _shouldUploadExcludedList,
        _shouldUploadMissingList, _shouldUploadSymlinkList, _shouldUploadGetprop,
        _shouldUploadAppLogs, _zipEncryption, _ignoreExcludeList, _selectedService,
        _selectedIpSource, _fatalError
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            customBatchSizeMb = args[0] as String,
            proxySpecification = args[1] as String,
            shouldUseTor = args[2] as Boolean,
            shouldUploadZips = args[3] as Boolean,
            shouldUploadReadableList = args[4] as Boolean,
            shouldUploadUnreadableList = args[5] as Boolean,
            shouldUploadExcludedList = args[6] as Boolean,
            shouldUploadMissingList = args[7] as Boolean,
            shouldUploadSymlinkList = args[8] as Boolean,
            shouldUploadGetprop = args[9] as Boolean,
            shouldUploadAppLogs = args[10] as Boolean,
            zipEncryption = args[11] as ZipEncryption,
            ignoreExcludeList = args[12] as Boolean,
            selectedService = args[13] as UploadRepository,
            services = services,
            selectedIpSource = args[14] as String,
            availableIpSources = ipInfoRepository.getAvailableSources(),
            fatalError = args[15] as String?,
            exclusionList = loadExcludeListUseCase.execute(),
            discoveryRoots = getSeedPathsUseCase.execute()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState(
            selectedService = uploadRepositoryManager.getSelectedRepository(),
            services = services,
            availableIpSources = ipInfoRepository.getAvailableSources(),
            exclusionList = loadExcludeListUseCase.execute(),
            discoveryRoots = getSeedPathsUseCase.execute()
        )
    )

    init {
        if (_shouldUseTor.value) {
            setProxySpecification("9050")
            startTorService()
        }
    }

    /**
     * Processes incoming UI intents to update settings and manage app state.
     * 
     * @param intent The intent to process.
     */
    fun processIntent(intent: Intent) {
        when (intent) {
            is Intent.NavigateToQrCode -> updateAppState(SettingsResult.AppStateChanged(AppState.QrCodeScreen(intent.qrcodeText)))
            Intent.NavigateToMain -> updateAppState(SettingsResult.AppStateChanged(AppState.MainScreen))
            Intent.NavigateToHelp -> updateAppState(SettingsResult.AppStateChanged(AppState.HelpScreen))
            Intent.NavigateToIpInfo -> updateAppState(SettingsResult.AppStateChanged(AppState.IpInfoScreen))
            is Intent.SetCustomBatchSizeMb -> savedStateHandle["customBatchSizeMb"] = intent.size
            is Intent.SetProxySpecification -> setProxySpecification(intent.spec)
            is Intent.SetShouldUseTor -> setShouldUseTor(intent.value)
            is Intent.SetShouldUploadZips -> savedStateHandle["shouldUploadZips"] = intent.value
            is Intent.SetShouldUploadReadableList -> savedStateHandle["shouldUploadReadableList"] = intent.value
            is Intent.SetShouldUploadUnreadableList -> savedStateHandle["shouldUploadUnreadableList"] = intent.value
            is Intent.SetShouldUploadExcludedList -> savedStateHandle["shouldUploadExcludedList"] = intent.value
            is Intent.SetShouldUploadMissingList -> savedStateHandle["shouldUploadMissingList"] = intent.value
            is Intent.SetShouldUploadSymlinkList -> savedStateHandle["shouldUploadSymlinkList"] = intent.value
            is Intent.SetShouldUploadGetprop -> savedStateHandle["shouldUploadGetprop"] = intent.value
            is Intent.SetShouldUploadAppLogs -> savedStateHandle["shouldUploadAppLogs"] = intent.value
            is Intent.SetZipEncryption -> savedStateHandle["zipEncryption"] = intent.value
            is Intent.SetIgnoreExcludeList -> savedStateHandle["ignoreExcludeList"] = intent.value
            is Intent.SelectService -> {
                _selectedService.value = intent.service
                uploadRepositoryManager.selectRepository(intent.service.id)
            }
            is Intent.SetSelectedIpSource -> savedStateHandle["selectedIpSource"] = intent.source
            is Intent.SetFatalError -> _fatalError.value = intent.error
        }
    }

    private fun updateAppState(result: SettingsResult) {
        _appState.update { reduce(result) }
    }

    private fun setProxySpecification(spec: String) {
        savedStateHandle["proxySpecification"] = spec
        val proxy = networkUtils.getProxyFromSpecification(spec)
        httpClientProvider.setProxy(proxy)
    }

    private var lastTorToggleTime: Instant = clock.now()

    /**
     * Updates the flag indicating whether Tor should be used for uploads.
     * 
     * If the flag changes, it also triggers the starting or stopping of the Tor service.
     * A debounce mechanism is implemented to prevent rapid toggling.
     *
     * @param value The new value for the use-Tor flag.
     */
    private fun setShouldUseTor(value: Boolean) {
        val now = clock.now()
        val diff = Duration.between(lastTorToggleTime, now).toMillis()
        if (diff < 1000L) {
            return
        }
        lastTorToggleTime = now

        savedStateHandle["shouldUseTor"] = value
        if (value) {
            setProxySpecification("9050")
            startTorService()
        } else {
            setProxySpecification("")
            stopTorService()
        }
    }

    private fun startTorService() {
        try {
            startTorUseCase.execute(TorService.ACTION_START)
        } catch (e: Exception) {
            logger.e("SettingsViewModel", "Failed to start CustomTorService", e)
        }
    }

    private fun stopTorService() {
        try {
            stopTorUseCase.execute()
        } catch (e: Exception) {
            logger.e("SettingsViewModel", "Failed to stop CustomTorService", e)
        }
    }
}
