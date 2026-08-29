package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.upload.UploadResult
import hu.muzso.android_system_dumper.network.upload.gateway.FilebinGateway
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FilebinUploadRepository @Inject constructor(
    private val gateway: FilebinGateway,
    private val platformUtils: PlatformUtils
) : UploadRepository {

    override val id: String = "filebin.net"
    override val name: String = "Filebin"

    private val _totalUploadedBytes = MutableStateFlow(0L)
    override val totalUploadedBytes: StateFlow<Long> = _totalUploadedBytes.asStateFlow()

    override fun incrementTotalUploadedBytes(bytes: Long) {
        _totalUploadedBytes.value += bytes
    }

    private var baseUrl = "https://filebin.net/"
    private var currentBin: String? = null

    /**
     * Uploads a file to Filebin, creating a new bin if one hasn't been established.
     * 
     * This method generates a unique bin name on the first upload and persists it for 
     * subsequent uploads in the same session. It maps [GatewayResult] updates from the
     * underlying network gateway to [UploadResult] events.
     *
     * @param filePath The local path to the file to be uploaded.
     * @param fileName The name to give the file on the server.
     * @return A flow of [UploadResult] representing the progress and outcome.
     */
    override fun upload(filePath: String, fileName: String): Flow<UploadResult> {
        val binName = currentBin ?: platformUtils.makeBinName().also { currentBin = it }

        return gateway.upload(binName, fileName, filePath).map { result ->
            when (result) {
                is GatewayResult.Progress -> UploadResult.Progress(result.bytesWritten, result.totalBytes)
                is GatewayResult.Success -> UploadResult.Success("https://filebin.net/$binName/$fileName")
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
        currentBin = null
        _totalUploadedBytes.value = 0L
    }

    override suspend fun getUrlListUrl(): String = currentBin?.let { "$baseUrl$it/" } ?: ""

    override suspend fun torCheck(): Boolean = gateway.torCheck()

    override suspend fun logConfiguration() {
        gateway.logConfiguration()
    }
}
