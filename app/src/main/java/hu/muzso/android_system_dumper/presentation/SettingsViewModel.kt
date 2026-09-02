package hu.muzso.android_system_dumper.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.network.upload.HttpClientProvider
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.network.upload.UploadRepositoryManager
import hu.muzso.android_system_dumper.presentation.state.AppState
import hu.muzso.android_system_dumper.presentation.state.FatalError
import hu.muzso.android_system_dumper.presentation.state.SettingsResult
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.reduce
import hu.muzso.android_system_dumper.repository.IpInfoRepository
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
        data class NavigateTo(val state: AppState) : Intent()
        object NavigateToMain : Intent()
        object NavigateToHelp : Intent()
        object NavigateToIpInfo : Intent()
        object NavigateToDownload : Intent()
        data class SetCustomBatchSizeMb(val size: String) : Intent()
        data class SetProxySpecification(val spec: String) : Intent()
        data class SetShouldUseTor(val value: Boolean) : Intent()
        data class SetShouldUploadZips(val value: Boolean) : Intent()
        data class SetShouldUploadFileLists(val value: Boolean) : Intent()
        data class SetShouldUploadGetprop(val value: Boolean) : Intent()
        data class SetShouldUploadAppLogs(val value: Boolean) : Intent()
        data class SetMaxUploadRetries(val value: String) : Intent()
        data class SetZipEncryption(val value: ZipEncryption) : Intent()
        data class SetUseDoubleZipping(val value: Boolean) : Intent()
        data class SetIgnoreExcludeList(val value: Boolean) : Intent()
        data class SelectService(val service: UploadRepository) : Intent()
        data class SetSelectedIpSource(val source: String) : Intent()
        data class SetFatalError(val error: FatalError?) : Intent()
    }

    private val _appState = MutableStateFlow<AppState>(AppState.MainScreen)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _ignoreExcludeList = savedStateHandle.getStateFlow("ignoreExcludeList", SettingsUiState.DEFAULT_IGNORE_EXCLUDE_LIST)
    private val _customBatchSizeMb = savedStateHandle.getStateFlow("customBatchSizeMb", SettingsUiState.DEFAULT_CUSTOM_BATCH_SIZE_MB)
    private val _proxySpecification = savedStateHandle.getStateFlow("proxySpecification", SettingsUiState.DEFAULT_PROXY_SPECIFICATION)
    private val _shouldUseTor = savedStateHandle.getStateFlow("shouldUseTor", SettingsUiState.DEFAULT_SHOULD_USE_TOR)
    private val _shouldUploadZips = savedStateHandle.getStateFlow("shouldUploadZips", SettingsUiState.DEFAULT_SHOULD_UPLOAD_ZIPS)
    private val _shouldUploadFileLists = savedStateHandle.getStateFlow("shouldUploadFileLists", SettingsUiState.DEFAULT_SHOULD_UPLOAD_FILE_LISTS)
    private val _shouldUploadGetprop = savedStateHandle.getStateFlow("shouldUploadGetprop", SettingsUiState.DEFAULT_SHOULD_UPLOAD_GETPROP)
    private val _shouldUploadAppLogs = savedStateHandle.getStateFlow("shouldUploadAppLogs", SettingsUiState.DEFAULT_SHOULD_UPLOAD_APP_LOGS)
    private val _zipEncryption = savedStateHandle.getStateFlow("zipEncryption", SettingsUiState.DEFAULT_ZIP_ENCRYPTION)
    private val _useDoubleZipping = savedStateHandle.getStateFlow("useDoubleZipping", SettingsUiState.DEFAULT_USE_DOUBLE_ZIPPING)
    private val _maxUploadRetries = savedStateHandle.getStateFlow("maxUploadRetries", SettingsUiState.DEFAULT_MAX_UPLOAD_RETRIES)
    private val _selectedIpSource = savedStateHandle.getStateFlow("selectedIpSource", ipInfoRepository.getAvailableSources().first())
    private val _fatalError = MutableStateFlow<FatalError?>(null)

    val services = uploadRepositoryManager.getRepositories().sortedBy { it.name }

    private val _selectedService = MutableStateFlow(uploadRepositoryManager.getSelectedRepository())

    val uiState: StateFlow<SettingsUiState> = combine(
        _customBatchSizeMb, _proxySpecification, _shouldUseTor, _shouldUploadZips,
        _shouldUploadFileLists, _shouldUploadGetprop,
        _shouldUploadAppLogs, _zipEncryption, _useDoubleZipping, _ignoreExcludeList,
        _maxUploadRetries, _selectedService, _selectedIpSource, _fatalError
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            customBatchSizeMb = args[0] as String,
            proxySpecification = args[1] as String,
            shouldUseTor = args[2] as Boolean,
            shouldUploadZips = args[3] as Boolean,
            shouldUploadFileLists = args[4] as Boolean,
            shouldUploadGetprop = args[5] as Boolean,
            shouldUploadAppLogs = args[6] as Boolean,
            zipEncryption = args[7] as ZipEncryption,
            useDoubleZipping = args[8] as Boolean,
            ignoreExcludeList = args[9] as Boolean,
            maxUploadRetries = args[10] as String,
            selectedService = args[11] as UploadRepository,
            services = services,
            selectedIpSource = args[12] as String,
            availableIpSources = ipInfoRepository.getAvailableSources(),
            fatalError = args[13] as FatalError?,
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
            is Intent.NavigateToQrCode -> updateAppState(SettingsResult.AppStateChanged(AppState.QrCodeScreen(intent.qrcodeText, _appState.value)))
            is Intent.NavigateTo -> updateAppState(SettingsResult.AppStateChanged(intent.state))
            Intent.NavigateToMain -> updateAppState(SettingsResult.AppStateChanged(AppState.MainScreen))
            Intent.NavigateToHelp -> updateAppState(SettingsResult.AppStateChanged(AppState.HelpScreen))
            Intent.NavigateToIpInfo -> updateAppState(SettingsResult.AppStateChanged(AppState.IpInfoScreen))
            Intent.NavigateToDownload -> updateAppState(SettingsResult.AppStateChanged(AppState.DownloadScreen))
            is Intent.SetCustomBatchSizeMb -> savedStateHandle["customBatchSizeMb"] = intent.size
            is Intent.SetProxySpecification -> setProxySpecification(intent.spec)
            is Intent.SetShouldUseTor -> setShouldUseTor(intent.value)
            is Intent.SetShouldUploadZips -> savedStateHandle["shouldUploadZips"] = intent.value
            is Intent.SetShouldUploadFileLists -> savedStateHandle["shouldUploadFileLists"] = intent.value
            is Intent.SetShouldUploadGetprop -> savedStateHandle["shouldUploadGetprop"] = intent.value
            is Intent.SetShouldUploadAppLogs -> savedStateHandle["shouldUploadAppLogs"] = intent.value
            is Intent.SetMaxUploadRetries -> savedStateHandle["maxUploadRetries"] = intent.value
            is Intent.SetZipEncryption -> {
                savedStateHandle["zipEncryption"] = intent.value
                if (intent.value == ZipEncryption.NONE) {
                    savedStateHandle["useDoubleZipping"] = false
                }
            }
            is Intent.SetUseDoubleZipping -> {
                if (_zipEncryption.value != ZipEncryption.NONE) {
                    savedStateHandle["useDoubleZipping"] = intent.value
                } else {
                    savedStateHandle["useDoubleZipping"] = false
                }
            }
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
