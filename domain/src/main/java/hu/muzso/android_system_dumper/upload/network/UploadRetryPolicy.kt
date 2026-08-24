package hu.muzso.android_system_dumper.upload.network

interface UploadRetryPolicy {
    suspend fun <T> withRetry(
        label: String,
        retries: Int,
        onStatusUpdate: suspend (label: String, attempt: Int, totalRetries: Int) -> Unit,
        onFailure: suspend (attempt: Int, ex: Exception) -> Unit = { _, _ -> },
        block: suspend () -> T
    ): T
}
