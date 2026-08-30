package hu.muzso.android_system_dumper.presentation

import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.config.AppConfig
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.network.ArchiveGenerator
import hu.muzso.android_system_dumper.network.download.HttpDownloadServer
import hu.muzso.android_system_dumper.scan.ScanRepository
import hu.muzso.android_system_dumper.usecase.GenerateQrUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

class DownloadViewModelTest {
    private val downloadServer = mockk<HttpDownloadServer>(relaxed = true)
    private val networkUtils = mockk<NetworkUtils>()
    private val generateQrUseCase = mockk<GenerateQrUseCase>()
    private val scanRepository = mockk<ScanRepository>()
    private val archiveGenerator = mockk<ArchiveGenerator>()
    private val appConfig = mockk<AppConfig>()

    private lateinit var viewModel: DownloadViewModel

    @Before
    fun setup() {
        every { downloadServer.progress } returns MutableStateFlow(null)
        every { appConfig.httpServerIpAddress } returns ""
        viewModel = DownloadViewModel(downloadServer, networkUtils, generateQrUseCase, scanRepository, archiveGenerator, appConfig)
    }

    @Test
    fun `startServer starts server and selects first IP`() {
        val params = mockk<UploadParameters>()
        val scanResult = mockk<ScanResult>()
        every { scanRepository.scanResult.value } returns scanResult
        every { downloadServer.start(params, scanResult) } returns 12345
        every { networkUtils.getLocalIPv4Addresses() } returns listOf("192.168.1.1", "10.0.0.1")
        every { generateQrUseCase.execute(any(), any()) } returns null
        every { archiveGenerator.getEncryptionPassphrase() } returns "test_passphrase"
        
        viewModel.startServer(params)
        
        verify { downloadServer.start(params, scanResult) }
        Truth.assertThat(viewModel.uiState.value.serverPort).isEqualTo(12345)
        Truth.assertThat(viewModel.uiState.value.selectedIp).isEqualTo("192.168.1.1")
        Truth.assertThat(viewModel.uiState.value.generatedPassphrase).isEqualTo("test_passphrase")
    }

    @Test
    fun `startServer uses IP address from configuration if provided`() {
        val params = mockk<UploadParameters>()
        val scanResult = mockk<ScanResult>()
        every { scanRepository.scanResult.value } returns scanResult
        every { downloadServer.start(params, scanResult) } returns 12345
        every { appConfig.httpServerIpAddress } returns "10.10.10.10"
        every { generateQrUseCase.execute(any(), any()) } returns null
        every { archiveGenerator.getEncryptionPassphrase() } returns "test_passphrase"

        viewModel.startServer(params)

        Truth.assertThat(viewModel.uiState.value.selectedIp).isEqualTo("10.10.10.10")
        verify(exactly = 0) { networkUtils.getLocalIPv4Addresses() }
    }

    @Test
    fun `startServer does nothing if server is already running`() {
        val params = mockk<UploadParameters>()
        val scanResult = mockk<ScanResult>()
        every { scanRepository.scanResult.value } returns scanResult
        every { downloadServer.start(params, scanResult) } returns 12345
        every { networkUtils.getLocalIPv4Addresses() } returns listOf("192.168.1.1")
        every { generateQrUseCase.execute(any(), any()) } returns null
        every { archiveGenerator.getEncryptionPassphrase() } returns "test_passphrase"

        // First call starts server
        viewModel.startServer(params)
        verify(exactly = 1) { downloadServer.start(params, scanResult) }

        // Second call should return early
        viewModel.startServer(params)
        verify(exactly = 1) { downloadServer.start(params, scanResult) }
    }

    @Test
    fun `stopServer stops server and resets state`() {
        viewModel.stopServer()
        verify { downloadServer.stop() }
        Truth.assertThat(viewModel.uiState.value.serverPort).isEqualTo(0)
    }
}
