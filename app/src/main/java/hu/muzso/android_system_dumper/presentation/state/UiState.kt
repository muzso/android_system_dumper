package hu.muzso.android_system_dumper.presentation.state

import android.graphics.Bitmap
import hu.muzso.android_system_dumper.model.IpInfo
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.network.upload.UploadRepository

data class SettingsUiState(
    val customBatchSizeMb: String = "200",
    val proxySpecification: String = "",
    val shouldUseTor: Boolean = true,
    val shouldUploadZips: Boolean = true,
    val shouldUploadReadableList: Boolean = true,
    val shouldUploadUnreadableList: Boolean = true,
    val shouldUploadExcludedList: Boolean = true,
    val shouldUploadMissingList: Boolean = true,
    val shouldUploadSymlinkList: Boolean = true,
    val shouldUploadGetprop: Boolean = false,
    val shouldUploadAppLogs: Boolean = false,
    val zipEncryption: ZipEncryption = ZipEncryption.STANDARD,
    val useDoubleZipping: Boolean = false,
    val ignoreExcludeList: Boolean = false,
    val selectedService: UploadRepository? = null,
    val services: List<UploadRepository> = emptyList(),
    val selectedIpSource: String = "",
    val availableIpSources: List<String> = emptyList(),
    val fatalError: String? = null,
    val exclusionList: List<String> = emptyList(),
    val discoveryRoots: List<String> = emptyList()
)

sealed class AppState {
    object MainScreen : AppState()
    data class QrCodeScreen(val qrcodeText: String, val previousState: AppState) : AppState()
    object HelpScreen : AppState()
    object IpInfoScreen : AppState()
    object DownloadScreen : AppState()
}

data class DownloadUiState(
    val serverPort: Int = 0,
    val localIps: List<String> = emptyList(),
    val selectedIp: String = "",
    val qrBitmap: Bitmap? = null,
    val successCount: Int = 0,
    val totalCount: Int = 0,
    val currentFileName: String = "",
    val currentBytes: Long = 0,
    val totalBytes: Long = 0,
    val statusText: String = "",
    val totalDownloadedBytes: Long = 0,
    val isFinished: Boolean = false,
    val generatedPassphrase: String? = null
)

sealed interface DownloadResult {
    data class ServerStarted(val port: Int, val localIps: List<String>, val passphrase: String?) : DownloadResult
    data class IpSelected(val ip: String, val qrBitmap: Bitmap?) : DownloadResult
    data class ProgressUpdated(val progress: hu.muzso.android_system_dumper.model.download.DownloadProgress) : DownloadResult
    object Reset : DownloadResult
}

fun reduce(state: DownloadUiState, result: DownloadResult): DownloadUiState {
    return when (result) {
        is DownloadResult.ServerStarted -> state.copy(
            serverPort = result.port,
            localIps = result.localIps,
            selectedIp = result.localIps.firstOrNull() ?: "",
            generatedPassphrase = result.passphrase
        )
        is DownloadResult.IpSelected -> state.copy(
            selectedIp = result.ip,
            qrBitmap = result.qrBitmap
        )
        is DownloadResult.ProgressUpdated -> state.copy(
            successCount = result.progress.successCount,
            totalCount = result.progress.totalCount,
            currentFileName = result.progress.currentFileName,
            currentBytes = result.progress.currentBytes,
            totalBytes = result.progress.totalBytes,
            statusText = result.progress.statusText,
            totalDownloadedBytes = result.progress.totalDownloadedBytes,
            isFinished = result.progress.isFinished
        )
        DownloadResult.Reset -> DownloadUiState()
    }
}

sealed class IpInfoUiState {
    object Loading : IpInfoUiState()
    data class Success(val ipInfo: IpInfo) : IpInfoUiState()
    data class Error(val message: String) : IpInfoUiState()
}

data class UploadUiState(
    val totalZips: Int = 0,
    val uploadedZips: Int = 0,
    val currentZipUploadBytes: Long = 0L,
    val currentZipTotalBytes: Long = 0L,
    val isUploading: Boolean = false,
    val uploadStatusText: String = "",
    val downloadUrl: String? = null,
    val generatedPassphrase: String? = null,
    val qrBitmap: Bitmap? = null
)

sealed interface UploadResult {
    object PreparationStarted : UploadResult
    data class StatusTextChanged(val text: String) : UploadResult
    data class TotalPlannedUploads(val count: Int) : UploadResult
    data class SuccessfulUploads(val count: Int) : UploadResult
    data class ProgressUpdated(val currentBytes: Long, val totalBytes: Long) : UploadResult
    data class UploadFinished(
        val downloadUrl: String?,
        val uploadedZips: Int,
        val passphrase: String?,
        val statusText: String
    ) : UploadResult
    object UploadError : UploadResult
    object UploadAborted : UploadResult
    data class QrGenerated(val bitmap: Bitmap?) : UploadResult
    object Reset : UploadResult
}

fun reduce(state: UploadUiState, result: UploadResult): UploadUiState {
    return when (result) {
        UploadResult.PreparationStarted -> state.copy(
            isUploading = true,
            downloadUrl = null,
            uploadedZips = 0,
            totalZips = 0,
            generatedPassphrase = null,
            currentZipUploadBytes = 0L,
            currentZipTotalBytes = 0L
        )
        is UploadResult.StatusTextChanged -> state.copy(uploadStatusText = result.text)
        is UploadResult.TotalPlannedUploads -> state.copy(totalZips = result.count)
        is UploadResult.SuccessfulUploads -> state.copy(uploadedZips = result.count)
        is UploadResult.ProgressUpdated -> state.copy(
            currentZipUploadBytes = result.currentBytes,
            currentZipTotalBytes = result.totalBytes
        )
        is UploadResult.UploadFinished -> state.copy(
            isUploading = false,
            downloadUrl = result.downloadUrl,
            uploadedZips = result.uploadedZips,
            generatedPassphrase = result.passphrase,
            uploadStatusText = result.statusText
        )
        UploadResult.UploadError -> state.copy(isUploading = false)
        UploadResult.UploadAborted -> state.copy(
            isUploading = false,
            uploadStatusText = result.toString() // Or a specific string, but keeping it simple for now
        )
        is UploadResult.QrGenerated -> state.copy(qrBitmap = result.bitmap)
        UploadResult.Reset -> UploadUiState()
    }
}

sealed interface SettingsResult {
    data class AppStateChanged(val appState: AppState) : SettingsResult
}

fun reduce(result: SettingsResult): AppState {
    return when (result) {
        is SettingsResult.AppStateChanged -> result.appState
    }
}
