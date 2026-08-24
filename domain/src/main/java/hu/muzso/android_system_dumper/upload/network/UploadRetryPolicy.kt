package hu.muzso.android_system_dumper.upload.network

interface UploadRetryPolicy {
    suspend fun <T> withRetry(
        label: String,
        retries: Int,
        onStatusUpdate: suspend (label: String, attempt: Int, totalRetries: Int) -> Unit,
        block: suspend () -> T
    ): T
}
