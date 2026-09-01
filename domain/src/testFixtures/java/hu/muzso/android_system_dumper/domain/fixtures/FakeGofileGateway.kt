package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import hu.muzso.android_system_dumper.network.upload.gateway.GofileGateway
import hu.muzso.android_system_dumper.network.upload.gateway.GofileUploadDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeGofileGateway : GofileGateway {
    var result: GatewayResult<GofileUploadDomainModel> = GatewayResult.Error("Not initialized")
    var lastFolderId: String? = null
    var lastToken: String? = null
    var internalBaseUrl: String = ""

    override fun upload(
        filePath: String,
        fileName: String,
        folderId: String?,
        token: String?
    ): Flow<GatewayResult<GofileUploadDomainModel>> = flow {
        lastFolderId = folderId
        lastToken = token
        emit(GatewayResult.Progress(0, 100L))
        emit(GatewayResult.Progress(100L, 100L))
        emit(result)
    }

    override fun setBaseUrl(url: String) {
        internalBaseUrl = url
    }

    override suspend fun torCheck(maxRetries: Int): Boolean = true

    override fun logConfiguration() {}
}
