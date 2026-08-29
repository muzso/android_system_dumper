package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.upload.UploadResult
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import hu.muzso.android_system_dumper.network.upload.gateway.GofileGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GofileUploadRepository @Inject constructor(
    private val gateway: GofileGateway
) : UploadRepository {

    override val id: String = "gofile.io"
    override val name: String = "Gofile"

    private val _totalUploadedBytes = MutableStateFlow(0L)
    override val totalUploadedBytes: StateFlow<Long> = _totalUploadedBytes.asStateFlow()

    override fun incrementTotalUploadedBytes(bytes: Long) {
        _totalUploadedBytes.value += bytes
    }

    private var urlListUrl = ""
    private var authToken: String? = null
    private var folderId: String? = null

    /**
     * Uploads a file to Gofile, managing session tokens and folder IDs.
     * 
     * This method tracks the [authToken] and [folderId] returned by Gofile on the 
     * first upload and reuses them for subsequent uploads to ensure all files 
     * are grouped together. It translates gateway results into [UploadResult] events.
     *
     * @param filePath The local path to the file to be uploaded.
     * @param fileName The name to give the file on the server.
     * @return A flow of [UploadResult] representing the progress and outcome.
     */
    override fun upload(filePath: String, fileName: String): Flow<UploadResult> {
        return gateway.upload(filePath, fileName, folderId, authToken).map { result ->
            when (result) {
                is GatewayResult.Progress -> UploadResult.Progress(result.bytesWritten, result.totalBytes)
                is GatewayResult.Success -> {
                    val data = result.data
                    if (urlListUrl.isEmpty()) urlListUrl = data.downloadPage
                    if (authToken == null) authToken = data.guestToken
                    if (folderId == null) folderId = data.parentFolder
                    
                    UploadResult.Success(data.downloadPage)
                }
                is GatewayResult.Error -> {
                    val uploadError = when {
                        result.message.contains("Network", ignoreCase = true) -> UploadError.NetworkError(result.message, result.throwable)
                        result.message.contains("Auth", ignoreCase = true) -> UploadError.AuthenticationError(result.message)
                        else -> UploadError.Unknown(result.message, result.throwable)
                    }
                    UploadResult.Error(uploadError)
                }
            }
        }
    }

    override suspend fun reset() {
        urlListUrl = ""
        authToken = null
        folderId = null
        _totalUploadedBytes.value = 0L
    }

    override suspend fun getUrlListUrl(): String = urlListUrl
    
    override suspend fun torCheck(): Boolean = gateway.torCheck()

    override suspend fun logConfiguration() {
        gateway.logConfiguration()
    }

    fun setBaseUrl(url: String) {
        gateway.setBaseUrl(url)
    }
}
