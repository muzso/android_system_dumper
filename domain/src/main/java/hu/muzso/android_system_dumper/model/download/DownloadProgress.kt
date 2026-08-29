package hu.muzso.android_system_dumper.model.download

/**
 * Data class representing the progress of file downloads from the local HTTP server.
 */
data class DownloadProgress(
    val successCount: Int,
    val totalCount: Int,
    val currentFileName: String = "",
    val currentBytes: Long = 0,
    val totalBytes: Long = 0,
    val statusText: String = "",
    val totalDownloadedBytes: Long = 0,
    val startTime: Long = 0,
    val isFinished: Boolean = false
)
