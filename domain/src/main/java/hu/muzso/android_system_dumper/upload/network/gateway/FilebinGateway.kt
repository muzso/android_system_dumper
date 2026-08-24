package hu.muzso.android_system_dumper.upload.network.gateway

import kotlinx.coroutines.flow.Flow

interface FilebinGateway {
    fun upload(
        bin: String,
        fileName: String,
        filePath: String
    ): Flow<GatewayResult<Unit>>

    fun setBaseUrl(url: String)
    suspend fun torCheck(): Boolean

    /**
     * Logs the current configuration of the network client.
     */
    fun logConfiguration()
}
