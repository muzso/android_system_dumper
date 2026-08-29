package hu.muzso.android_system_dumper.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.network.ArchiveGenerator
import hu.muzso.android_system_dumper.network.download.HttpDownloadServer
import hu.muzso.android_system_dumper.presentation.state.DownloadResult
import hu.muzso.android_system_dumper.presentation.state.DownloadUiState
import hu.muzso.android_system_dumper.presentation.state.reduce
import hu.muzso.android_system_dumper.scan.ScanRepository
import hu.muzso.android_system_dumper.usecase.GenerateQrUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Download Screen, managing the local HTTP server and its UI state.
 */
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadServer: HttpDownloadServer,
    private val networkUtils: NetworkUtils,
    private val generateQrUseCase: GenerateQrUseCase,
    private val scanRepository: ScanRepository,
    private val archiveGenerator: ArchiveGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadServer.progress.collect { progress ->
                progress?.let {
                    _uiState.update { state -> reduce(state, DownloadResult.ProgressUpdated(it)) }
                }
            }
        }
    }

    /**
     * Starts the HTTP server with the given parameters and scan results from repository.
     */
    fun startServer(parameters: UploadParameters) {
        if (_uiState.value.serverPort != 0) return

        val scanResult = scanRepository.scanResult.value
        val port = downloadServer.start(parameters, scanResult)
        val passphrase = archiveGenerator.getEncryptionPassphrase()
        val localIps = networkUtils.getLocalIPv4Addresses()
        _uiState.update { reduce(it, DownloadResult.ServerStarted(port, localIps, passphrase)) }
        if (localIps.isNotEmpty()) {
            selectIp(localIps.first())
        }
    }

    /**
     * Stops the HTTP server and resets the UI state.
     */
    fun stopServer() {
        downloadServer.stop()
        _uiState.update { reduce(it, DownloadResult.Reset) }
    }

    /**
     * Selects a local IP address for the server URL and regenerates the QR code.
     */
    fun selectIp(ip: String) {
        val port = _uiState.value.serverPort
        val url = "http://$ip:$port/"
        val qrBitmap = generateQrUseCase.execute(url, 1024) as? Bitmap
        _uiState.update { reduce(it, DownloadResult.IpSelected(ip, qrBitmap)) }
    }

    override fun onCleared() {
        super.onCleared()
        downloadServer.stop()
    }
}
