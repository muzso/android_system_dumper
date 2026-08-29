package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.scan.ScanRepository
import kotlinx.coroutines.flow.Flow

class StartScanUseCase(
    private val repository: ScanRepository
) {
    fun execute(ignoreExcludeList: Boolean, fileCountLimit: Int = 0): Flow<ScanStatus> = repository.scan(ignoreExcludeList, fileCountLimit)
}
