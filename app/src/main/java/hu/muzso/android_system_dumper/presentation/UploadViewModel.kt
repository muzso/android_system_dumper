package hu.muzso.android_system_dumper.presentation

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.config.AppConfig
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.UiMessenger
import hu.muzso.android_system_dumper.presentation.state.UploadResult
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import hu.muzso.android_system_dumper.presentation.state.reduce
import hu.muzso.android_system_dumper.scan.ScanRepository
import hu.muzso.android_system_dumper.usecase.GenerateQrUseCase
import hu.muzso.android_system_dumper.usecase.UploadArchiveUseCase
import hu.muzso.android_system_dumper.usecase.ValidateUploadUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val uiMessenger: UiMessenger,
    private val platformUtils: PlatformUtils,
    private val savedStateHandle: SavedStateHandle,
    private val repository: ScanRepository,
    private val logger: FileLogger,
    private val validateUploadUseCase: ValidateUploadUseCase,
    private val uploadArchiveUseCase: UploadArchiveUseCase,
    private val generateQrUseCase: GenerateQrUseCase,
    private val appConfig: AppConfig
) : ViewModel() {

    companion object {
        private const val TAG = "UploadViewModel"
    }

    sealed class Intent {
        data class ToggleUploading(val settings: UploadSettings) : Intent()
        object StopUploading : Intent()
        object ResetResults : Intent()
        data class GenerateQr(val text: String) : Intent()
        object ClearQr : Intent()
    }

    private var uploadJob: Job? = null

    private val _uiState = MutableStateFlow(
        UploadUiState(
            downloadUrl = savedStateHandle.get<String>("downloadUrl"),
            generatedPassphrase = savedStateHandle.get<String>("generatedPassphrase"),
            uploadStatusText = savedStateHandle.get<String>("uploadStatusText") ?: ""
        )
    )
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    /**
     * Updates the UI state by applying the provided transformation.
     * 
     * This method also persists key state values to [SavedStateHandle] for process death recovery.
     *
     * @param transform A function that takes the current state and returns a new state.
     */
    private fun updateState(transform: (UploadUiState) -> UploadUiState) {
        _uiState.update { 
            val newState = transform(it)
            savedStateHandle["downloadUrl"] = newState.downloadUrl
            savedStateHandle["generatedPassphrase"] = newState.generatedPassphrase
            savedStateHandle["uploadStatusText"] = newState.uploadStatusText
            newState
        }
    }

    /**
     * Processes incoming UI intents and maps them to ViewModel actions.
     * 
     * @param intent The intent to process.
     */
    fun processIntent(intent: Intent) {
        when (intent) {
            is Intent.ToggleUploading -> toggleUploading(intent.settings)
            Intent.StopUploading -> stopUploading()
            Intent.ResetResults -> resetResults()
            is Intent.GenerateQr -> generateQr(intent.text)
            Intent.ClearQr -> updateState { reduce(it, UploadResult.QrGenerated(null)) }
        }
    }

    /**
     * Toggles the upload process state.
     * 
     * If an upload is already in progress, it initiates a stop. Otherwise, it 
     * starts a new upload session with the provided [settings].
     *
     * @param settings The configuration for the upload session.
     */
    private fun toggleUploading(settings: UploadSettings) {
        logger.i(TAG, "toggleUploading: isUploading=${_uiState.value.isUploading}")
        if (_uiState.value.isUploading) {
            stopUploading()
        } else {
            startUploading(settings)
        }
    }

    /**
     * Starts the upload process with the given settings.
     * 
     * This method validates the parameters, starts the background upload flow,
     * and updates the UI state based on the progress and result of the upload workflow.
     *
     * @param settings The settings for the upload process.
     */
    private fun startUploading(settings: UploadSettings) {
        val parameters = UploadParameters(
            customBatchSizeMb = settings.customBatchSizeMb,
            proxySpecification = settings.proxySpecification,
            shouldUseTor = settings.shouldUseTor,
            shouldUploadZips = settings.shouldUploadZips,
            shouldUploadReadableList = settings.shouldUploadReadableList,
            shouldUploadUnreadableList = settings.shouldUploadUnreadableList,
            shouldUploadExcludedList = settings.shouldUploadExcludedList,
            shouldUploadMissingList = settings.shouldUploadMissingList,
            shouldUploadSymlinkList = settings.shouldUploadSymlinkList,
            shouldUploadGetprop = settings.shouldUploadGetprop,
            shouldUploadAppLogs = settings.shouldUploadAppLogs,
            zipEncryption = settings.zipEncryption,
            useDoubleZipping = settings.useDoubleZipping,
            selectedService = settings.selectedService,
            maxBatches = appConfig.batchLimit
        )

        logger.i(TAG, "startUploading: parameters=$parameters")

        val validation = validateUploadUseCase.execute(parameters)
        if (validation is ValidateUploadUseCase.ValidationResult.Error) {
            val errorText = when (validation) {
                is ValidateUploadUseCase.ValidationResult.Error.InvalidBatchSize ->
                    resourceProvider.getString(R.string.custom_batch_size_error, validation.min, validation.max)
                is ValidateUploadUseCase.ValidationResult.Error.InvalidProxy ->
                    resourceProvider.getString(R.string.invalid_proxy_error, validation.spec)
                ValidateUploadUseCase.ValidationResult.Error.NoUploadSelected ->
                    resourceProvider.getString(R.string.pre_flight_check_failed)
            }
            settings.onFatalError(errorText)
            return
        }

        updateState { reduce(it, UploadResult.PreparationStarted).copy(
            uploadStatusText = resourceProvider.getString(R.string.preparing)
        ) }

        val scanResult = repository.scanResult.value
        if (scanResult.readableFiles.isEmpty()) {
            uiMessenger.showShortToast(resourceProvider.getString(R.string.file_list_is_empty))
            updateState { reduce(it, UploadResult.UploadError).copy(uploadStatusText = "") }
            return
        }

        uploadJob = viewModelScope.launch {
            if (parameters.shouldUseTor) {
                try {
                    val isTor = settings.selectedService.torCheck()
                    if (!isTor) {
                        settings.onFatalError(resourceProvider.getString(R.string.traffic_doesnt_go_through_tor_error))
                        updateState { reduce(it, UploadResult.UploadError).copy(uploadStatusText = "") }
                        return@launch
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Tor check failed", e)
                    settings.onFatalError(resourceProvider.getString(R.string.upload_crashed, e.message ?: "Tor check error"))
                    updateState { reduce(it, UploadResult.UploadError).copy(uploadStatusText = "") }
                    return@launch
                }
            }
            uploadArchiveUseCase.execute(parameters, scanResult).collect { status ->
                when (status) {
                    UploadWorkflowStatus.Preparing -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.preparing))) }
                    }
                    UploadWorkflowStatus.PartitioningBatches -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.partitioning_files_into_batches))) }
                    }
                    is UploadWorkflowStatus.ArchivingBatch -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.archiving_batch_of, status.current, status.total))) }
                    }
                    is UploadWorkflowStatus.UploadingBatch -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.uploading_attempt_of, status.label, status.attempt, status.totalRetries))) }
                    }
                    UploadWorkflowStatus.CreatingReadableList -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.creating_readable_file_list))) }
                    }
                    UploadWorkflowStatus.CreatingUnreadableList -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.creating_unreadable_file_list))) }
                    }
                    UploadWorkflowStatus.CreatingExcludedList -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.creating_excluded_file_list))) }
                    }
                    UploadWorkflowStatus.CreatingMissingList -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.creating_missing_list))) }
                    }
                    UploadWorkflowStatus.CreatingSymlinkList -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.creating_symlink_list))) }
                    }
                    is UploadWorkflowStatus.ExecutingCommand -> {
                        updateState { reduce(it, UploadResult.StatusTextChanged(resourceProvider.getString(
                            R.string.executing_command, status.command))) }
                    }
                    is UploadWorkflowStatus.TotalPlannedUploads -> {
                        updateState { reduce(it, UploadResult.TotalPlannedUploads(status.count)) }
                    }
                    is UploadWorkflowStatus.SuccessfulUploads -> {
                        updateState { reduce(it, UploadResult.SuccessfulUploads(status.count)) }
                    }
                    is UploadWorkflowStatus.Progress -> {
                        updateState { reduce(it, UploadResult.ProgressUpdated(status.currentZipBytes, status.totalZipBytes)) }
                    }
                    is UploadWorkflowStatus.Success -> {
                        updateState { reduce(it, UploadResult.UploadFinished(
                            downloadUrl = status.downloadUrl,
                            uploadedZips = status.uploadedZips,
                            passphrase = status.passphrase,
                            statusText = resourceProvider.getString(R.string.upload_success, status.uploadedZips.toLong(), platformUtils.formatBytes(status.totalBytes), status.runtimeSeconds / 60.0)
                        )) }
                    }
                    is UploadWorkflowStatus.PartialSuccess -> {
                        updateState { reduce(it, UploadResult.UploadFinished(
                            downloadUrl = status.downloadUrl,
                            uploadedZips = status.uploadedZips,
                            passphrase = status.passphrase,
                            statusText = resourceProvider.getString(
                                R.string.upload_partial_success,
                                status.uploadedZips,
                                status.totalZips,
                                platformUtils.formatBytes(status.totalBytes),
                                status.runtimeSeconds / 60.0,
                                status.failedZips
                            )
                        )).copy(totalZips = status.totalZips) }
                    }
                    is UploadWorkflowStatus.Error -> {
                        updateState { reduce(it, UploadResult.UploadError) }
                        val errorMessage = when (val error = status.error) {
                            is UploadError.NetworkError -> error.message
                            is UploadError.ServerError -> error.message
                            is UploadError.AuthenticationError -> error.message
                            is UploadError.FileNotFoundError -> "File not found: ${error.path}"
                            is UploadError.Cancelled -> error.message
                            is UploadError.ZeroSuccessfulUploads -> resourceProvider.getString(R.string.upload_zero_success, status.runtimeSeconds / 60.0)
                            is UploadError.MissingDownloadURL -> resourceProvider.getString(R.string.upload_error_empty_url, platformUtils.formatBytes(status.totalBytes), status.runtimeSeconds / 60.0)
                            is UploadError.InsufficientStorage -> resourceProvider.getString(R.string.insufficient_cache_space_required, platformUtils.formatBytes(error.requiredBytes))
                            is UploadError.TorVerificationFailed -> resourceProvider.getString(R.string.traffic_doesnt_go_through_tor_error)
                            is UploadError.Unknown -> error.message
                        }
                        settings.onFatalError(resourceProvider.getString(R.string.upload_crashed, errorMessage))
                    }
                    UploadWorkflowStatus.Aborted -> {
                        updateState { reduce(it, UploadResult.UploadAborted).copy(
                            uploadStatusText = resourceProvider.getString(R.string.aborted)
                        ) }
                    }
                }
            }
        }
    }

    /**
     * Stops any active upload job.
     * 
     * This method cancels the [uploadJob] and waits for its completion before 
     * updating the UI state to reflect that the process was aborted.
     */
    private fun stopUploading() {
        viewModelScope.launch {
            uploadJob?.cancelAndJoin()
            updateState { it.copy(
                isUploading = false,
                uploadStatusText = resourceProvider.getString(R.string.aborted)
            ) }
        }
    }

    fun formatBytes(bytes: Long): String = platformUtils.formatBytes(bytes)

    private fun generateQr(text: String) {
        val bitmap = generateQrUseCase.execute(text, 1024) as? Bitmap
        updateState { reduce(it, UploadResult.QrGenerated(bitmap)) }
    }

    private fun resetResults() {
        updateState { reduce(it, UploadResult.Reset) }
    }

    data class UploadSettings(
        val customBatchSizeMb: String,
        val proxySpecification: String,
        val shouldUseTor: Boolean,
        val shouldUploadZips: Boolean,
        val shouldUploadReadableList: Boolean,
        val shouldUploadUnreadableList: Boolean,
        val shouldUploadExcludedList: Boolean,
        val shouldUploadMissingList: Boolean,
        val shouldUploadSymlinkList: Boolean,
        val shouldUploadGetprop: Boolean,
        val shouldUploadAppLogs: Boolean,
        val zipEncryption: ZipEncryption,
        val useDoubleZipping: Boolean,
        val selectedService: UploadRepository,
        val onFatalError: (String?) -> Unit
    )
}
