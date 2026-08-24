package hu.muzso.android_system_dumper.model

data class ScanState(
    val scanStatus: ScanStatus = ScanStatus.IDLE,
    val isScanning: Boolean = false,
    val filesCount: Int = 0,
    val totalBytes: Long = 0L
)

sealed interface ScanningResult {
    object ScanStarted : ScanningResult
    object ScanStopped : ScanningResult
    data class StatusChanged(val status: ScanStatus) : ScanningResult
    data class ProgressUpdated(val filesCount: Int, val totalBytes: Long) : ScanningResult
    object Reset : ScanningResult
}

sealed interface ScanAction {
    data class ToggleScanning(val ignoreExcludeList: Boolean) : ScanAction
    object StopScanning : ScanAction
    object ResetResults : ScanAction
}

fun reduce(state: ScanState, result: ScanningResult): ScanState {
    return when (result) {
        ScanningResult.ScanStarted -> state.copy(isScanning = true)
        ScanningResult.ScanStopped -> state.copy(isScanning = false)
        is ScanningResult.StatusChanged -> {
            val isScanning = result.status == ScanStatus.RUNNING
            state.copy(
                scanStatus = result.status,
                isScanning = isScanning
            )
        }
        is ScanningResult.ProgressUpdated -> state.copy(
            filesCount = result.filesCount,
            totalBytes = result.totalBytes
        )
        ScanningResult.Reset -> ScanState()
    }
}