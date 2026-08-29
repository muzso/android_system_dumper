package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.model.ScanStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanSystemUseCase @Inject constructor(
    private val startScanUseCase: StartScanUseCase,
    private val cancelScanUseCase: CancelScanUseCase,
    private val clearScanResultsUseCase: ClearScanResultsUseCase,
    calculateStatisticsUseCase: CalculateStatisticsUseCase,
    private val getSeedPathsUseCase: GetSeedPathsUseCase
) {
    val filesCount = calculateStatisticsUseCase.filesCount
    val totalBytes = calculateStatisticsUseCase.totalBytes

    fun getSeedPaths(): List<String> = getSeedPathsUseCase.execute()

    fun execute(ignoreExcludeList: Boolean, fileCountLimit: Int = 0): Flow<ScanStatus> = startScanUseCase.execute(ignoreExcludeList, fileCountLimit)
    suspend fun cancel(job: Job?) = cancelScanUseCase.execute(job)
    fun clearResults() = clearScanResultsUseCase.execute()
}
