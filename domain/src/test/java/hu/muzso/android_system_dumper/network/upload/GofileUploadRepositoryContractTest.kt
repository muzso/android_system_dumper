package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.domain.repository.upload.UploadRepositoryContract
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import hu.muzso.android_system_dumper.network.upload.gateway.GofileGateway
import hu.muzso.android_system_dumper.network.upload.gateway.GofileUploadDomainModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.io.File

class GofileUploadRepositoryContractTest : UploadRepositoryContract() {
    private val gateway = mockk<GofileGateway>(relaxed = true)

    override fun createRepository(): UploadRepository {
        return GofileUploadRepository(gateway)
    }

    override fun setupSuccessResponse(filePath: String, fileName: String) {
        every { gateway.upload(any(), any(), any(), any()) } returns flowOf(
            GatewayResult.Progress(0, File(filePath).length()),
            GatewayResult.Success(GofileUploadDomainModel(
                downloadPage = "https://gofile.io/d/test",
                guestToken = "token",
                parentFolder = "folder",
            ))
        )
    }

    override fun setupNetworkErrorResponse() {
        every { gateway.upload(any(), any(), any(), any()) } returns flowOf(
            GatewayResult.Error("Network error")
        )
    }

    override fun setupAuthErrorResponse() {
        every { gateway.upload(any(), any(), any(), any()) } returns flowOf(
            GatewayResult.Error("Auth error")
        )
    }
}
