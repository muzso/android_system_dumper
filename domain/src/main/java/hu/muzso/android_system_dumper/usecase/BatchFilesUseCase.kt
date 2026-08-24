package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.upload.BatchingLogic

class BatchFilesUseCase(
    private val batchingLogic: BatchingLogic
) {
    fun execute(
        files: List<String>,
        fileSizes: Map<String, Long>,
        batchSizeInBytes: Long,
        maxBatches: Int
    ): List<List<String>> = batchingLogic.splitIntoBatches(files, fileSizes, batchSizeInBytes, maxBatches)
}
