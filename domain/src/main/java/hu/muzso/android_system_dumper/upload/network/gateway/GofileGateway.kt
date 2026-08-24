package hu.muzso.android_system_dumper.upload.network.gateway

import kotlinx.coroutines.flow.Flow

interface GofileGateway {
    fun upload(
        filePath: String,
        fileName: String,
        folderId: String?,
        token: String?
    ): Flow<GatewayResult<GofileUploadDomainModel>>

    fun setBaseUrl(url: String)
    suspend fun torCheck(): Boolean

    /**
     * Logs the current configuration of the network client.
     */
    fun logConfiguration()
}
