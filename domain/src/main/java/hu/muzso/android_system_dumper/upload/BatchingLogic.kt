package hu.muzso.android_system_dumper.upload

import hu.muzso.android_system_dumper.logging.FileLogger

class BatchingLogic(private val logger: FileLogger) {
    private val tag = "BatchingLogic"

    /**
     * Splits a list of files into batches based on their sizes and a maximum batch size.
     *
     * @param files The list of file paths to be batched.
     * @param fileSizes A map of file paths to their respective sizes in bytes.
     * @param batchSizeInBytes The maximum size of a single batch in bytes.
     * @param maxBatches The maximum number of batches to create. If 0 or less, no limit is applied.
     * @return A list of lists, where each inner list represents a batch of file paths.
     */
    fun splitIntoBatches(
        files: List<String>,
        fileSizes: Map<String, Long>,
        batchSizeInBytes: Long,
        maxBatches: Int
    ): List<List<String>> {
        val batches = ArrayList<List<String>>()
        var currentBatch = ArrayList<String>()
        var currentBatchSize = 0L

        logger.i(tag, "Splitting files into batches ... (batchSizeInBytes: $batchSizeInBytes, maxBatches: $maxBatches)")

        var totalSize = 0L
        for (filePath in files) {
            val size = fileSizes[filePath] ?: 0L
            totalSize += size
            if (size > batchSizeInBytes) {
                if (currentBatch.isNotEmpty()) {
                    batches.add(currentBatch)
                    currentBatch = ArrayList()
                    currentBatchSize = 0L
                }
                batches.add(listOf(filePath))
            } else {
                if (currentBatchSize + size > batchSizeInBytes) {
                    batches.add(currentBatch)
                    currentBatch = ArrayList()
                    currentBatch.add(filePath)
                    currentBatchSize = size
                } else {
                    currentBatch.add(filePath)
                    currentBatchSize += size
                }
            }
            if (maxBatches > 0 && batches.size >= maxBatches) break
        }
        if (currentBatch.isNotEmpty() && (maxBatches <= 0 || batches.size < maxBatches)) {
            batches.add(currentBatch)
        }
        return batches
    }
}