package hu.muzso.android_system_dumper.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.muzso.android_system_dumper.model.ScanAction
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanningResult
import hu.muzso.android_system_dumper.model.reduce
import hu.muzso.android_system_dumper.usecase.ScanSystemUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanSystemUseCase: ScanSystemUseCase
) : ViewModel() {

    private var scanJob: Job? = null

    private val _uiState = MutableStateFlow(ScanState())
    val uiState: StateFlow<ScanState> = _uiState.asStateFlow()

    /**
     * Initializes the ViewModel by subscribing to file count and total size updates
     * from the [ScanSystemUseCase]. These updates are reduced into the [uiState].
     */
    init {
        viewModelScope.launch {
            combine(
                scanSystemUseCase.filesCount,
                scanSystemUseCase.totalBytes
            ) { count, bytes ->
                ScanningResult.ProgressUpdated(count, bytes)
            }.collect { result ->
                _uiState.update { state -> reduce(state, result) }
            }
        }
    }

    /**
     * Processes scanning-related intents from the UI.
     * 
     * This maps UI actions (like toggling or stopping the scan) to the appropriate 
     * internal methods that interact with the [ScanSystemUseCase].
     *
     * @param action The intent/action triggered by the user.
     */
    fun processIntent(action: ScanAction) {
        when (action) {
            is ScanAction.ToggleScanning -> toggleScanning(action.ignoreExcludeList)
            ScanAction.StopScanning -> stopScanning()
            ScanAction.ResetResults -> resetResults()
        }
    }

    private fun toggleScanning(ignoreExcludeList: Boolean) {
        if (_uiState.value.isScanning) {
            stopScanning()
        } else {
            startScanning(ignoreExcludeList)
        }
    }

    /**
     * Initiates the system scanning process.
     * 
     * This method updates the UI state to indicate scanning has started, launches 
     * a coroutine to execute the [scanSystemUseCase], and collects status updates 
     * to update the [uiState].
     *
     * @param ignoreExcludeList Whether to ignore the configured exclusion list during the scan.
     */
    private fun startScanning(ignoreExcludeList: Boolean) {
        _uiState.update { reduce(it, ScanningResult.ScanStarted) }
        
        scanJob = viewModelScope.launch {
            scanSystemUseCase.execute(ignoreExcludeList)
                .collect { status ->
                    _uiState.update { reduce(it, ScanningResult.StatusChanged(status)) }
                }
        }
    }

    private fun stopScanning() {
        viewModelScope.launch {
            scanSystemUseCase.cancel(scanJob)
            _uiState.update { reduce(it, ScanningResult.ScanStopped) }
        }
    }

    private fun resetResults() {
        _uiState.update { reduce(it, ScanningResult.Reset) }
        scanSystemUseCase.clearResults()
    }
}
