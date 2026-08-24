package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.scan.ScanRepository

class ClearScanResultsUseCase(
    private val repository: ScanRepository
) {
    fun execute() {
        repository.clear()
    }
}
