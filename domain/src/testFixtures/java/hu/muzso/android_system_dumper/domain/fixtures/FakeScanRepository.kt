package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ScanUpdate
import hu.muzso.android_system_dumper.scan.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeScanRepository : ScanRepository {
    private val _scanResult = MutableStateFlow(ScanResult())
    override val scanResult: StateFlow<ScanResult> = _scanResult.asStateFlow()

    private val _scanUpdate = MutableStateFlow(ScanUpdate(0, 0L))
    override val scanUpdate: StateFlow<ScanUpdate> = _scanUpdate.asStateFlow()

    private var statuses: List<ScanStatus> = listOf(ScanStatus.RUNNING, ScanStatus.FINISHED)
    var lastIgnoreExcludeList: Boolean? = null
        private set

    fun setStatuses(newStatuses: List<ScanStatus>) {
        statuses = newStatuses
    }

    override fun scan(ignoreExcludeList: Boolean, fileCountLimit: Int): Flow<ScanStatus> {
        lastIgnoreExcludeList = ignoreExcludeList
        return statuses.asFlow()
    }

    override fun updateResult(result: ScanResult) {
        _scanResult.value = result
    }

    override fun updateProgress(update: ScanUpdate, result: ScanResult?) {
        _scanUpdate.value = update
        if (result != null) _scanResult.value = result
    }

    override fun clear() {
        _scanResult.value = ScanResult()
        _scanUpdate.value = ScanUpdate(0, 0L)
    }
}
