package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.model.upload.UploadResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UploadRepository {
    val id: String
    val name: String
    val totalUploadedBytes: StateFlow<Long>
    fun incrementTotalUploadedBytes(bytes: Long)
    fun upload(filePath: String, fileName: String): Flow<UploadResult>
    suspend fun reset()
    suspend fun getUrlListUrl(): String
    suspend fun torCheck(): Boolean

    /**
     * Logs the current configuration of the underlying HTTP client.
     * 
     * This is useful for diagnostics, especially to verify proxy settings 
     * before starting an upload.
     */
    suspend fun logConfiguration()
}
