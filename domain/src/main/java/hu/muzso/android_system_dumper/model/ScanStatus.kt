package hu.muzso.android_system_dumper.model

sealed class ScanStatus {
    object IDLE : ScanStatus()
    object RUNNING : ScanStatus()
    object FINISHED : ScanStatus()
    object ABORTED : ScanStatus()
    data class ERROR(val error: ScanError) : ScanStatus()
}
