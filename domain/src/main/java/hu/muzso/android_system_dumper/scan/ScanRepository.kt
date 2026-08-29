package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ScanUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ScanRepository {
    val scanResult: StateFlow<ScanResult>
    val scanUpdate: StateFlow<ScanUpdate>
    fun scan(ignoreExcludeList: Boolean, fileCountLimit: Int = 0): Flow<ScanStatus>
    fun updateResult(result: ScanResult)
    fun updateProgress(update: ScanUpdate, result: ScanResult? = null)
    fun clear()
}
