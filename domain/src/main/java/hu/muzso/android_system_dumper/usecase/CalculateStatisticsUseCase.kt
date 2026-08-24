package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.scan.ScanRepository
import kotlinx.coroutines.flow.map

class CalculateStatisticsUseCase(
    repository: ScanRepository
) {
    val filesCount = repository.scanUpdate.map { it.filesCount }
    val totalBytes = repository.scanUpdate.map { it.totalBytes }
}
