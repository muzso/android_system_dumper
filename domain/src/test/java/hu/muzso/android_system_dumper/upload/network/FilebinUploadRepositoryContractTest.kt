package hu.muzso.android_system_dumper.upload.network

import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.domain.repository.upload.UploadRepositoryContract
import hu.muzso.android_system_dumper.upload.network.gateway.FilebinGateway
import hu.muzso.android_system_dumper.upload.network.gateway.GatewayResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.io.File

class FilebinUploadRepositoryContractTest : UploadRepositoryContract() {
    private val gateway = mockk<FilebinGateway>(relaxed = true)
    private val platformUtils = mockk<PlatformUtils>()

    override fun createRepository(): UploadRepository {
        every { platformUtils.makeBinName() } returns "test-bin"
        return FilebinUploadRepository(gateway, platformUtils)
    }

    override fun setupSuccessResponse(filePath: String, fileName: String) {
        every { gateway.upload(any(), any(), any()) } returns flowOf(
            GatewayResult.Progress(0, File(filePath).length()),
            GatewayResult.Success(Unit),
        )
    }

    override fun setupNetworkErrorResponse() {
        every { gateway.upload(any(), any(), any()) } returns flowOf(
            GatewayResult.Error("Network error")
        )
    }

    override fun setupAuthErrorResponse() {
        // Filebin implementation doesn't specifically handle Auth errors in its mapping yet,
        // but we can test it returns Unknown or whatever it maps to.
        // Actually, the contract expects AuthenticationError for setupAuthErrorResponse.
        // Gofile maps "Auth" to AuthenticationError.
        // Filebin maps only "Network" specifically.
        every { gateway.upload(any(), any(), any()) } returns flowOf(
            GatewayResult.Error("Auth error")
        )
    }
}
