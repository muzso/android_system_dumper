package hu.muzso.android_system_dumper.network.upload

import kotlinx.coroutines.flow.StateFlow

interface UploadProgressTracker {
    val totalUploadedBytes: StateFlow<Long>
    fun incrementTotalUploadedBytes(bytes: Long)
    suspend fun reset()
}
