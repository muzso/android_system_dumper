package hu.muzso.android_system_dumper.presentation.screens

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.model.ScanAction
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.presentation.ScanViewModel
import hu.muzso.android_system_dumper.presentation.SettingsViewModel
import hu.muzso.android_system_dumper.presentation.UploadViewModel
import hu.muzso.android_system_dumper.presentation.components.FatalErrorDialog
import hu.muzso.android_system_dumper.presentation.components.ProgressCard
import hu.muzso.android_system_dumper.presentation.components.ResultsCard
import hu.muzso.android_system_dumper.presentation.components.UploadPanel
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme
import hu.muzso.android_system_dumper.upload.network.UploadRepository
import kotlinx.coroutines.launch

/**
 * The main screen of the application, coordinating state between various ViewModels.
 *
 * This Composable observes the UI state from [hu.muzso.android_system_dumper.presentation.ScanViewModel], [hu.muzso.android_system_dumper.presentation.SettingsViewModel],
 * and [hu.muzso.android_system_dumper.presentation.UploadViewModel], and passes the relevant data and interaction callbacks
 * to [MainScreenContent].
 *
 * @param scanViewModel The ViewModel managing system scan state.
 * @param settingsViewModel The ViewModel managing application settings and Tor state.
 * @param uploadViewModel The ViewModel managing the upload process and results.
 * @param onNavigateToQrCode Callback to navigate to the QR code display screen.
 * @param onShowHelp Callback to show the help screen.
 * @param showShortToast Callback to display a short toast message.
 */
@Composable
fun MainScreen(
    scanViewModel: ScanViewModel,
    settingsViewModel: SettingsViewModel,
    uploadViewModel: UploadViewModel,
    onNavigateToQrCode: (String) -> Unit,
    onShowHelp: () -> Unit,
    onNavigateToIpInfo: () -> Unit,
    showShortToast: (String) -> Unit
) {
    val scanUiState by scanViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val uploadUiState by uploadViewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        scanUiState = scanUiState,
        settingsUiState = settingsUiState,
        uploadUiState = uploadUiState,
        onResetResults = {
            scanViewModel.processIntent(ScanAction.ResetResults)
            uploadViewModel.processIntent(UploadViewModel.Intent.ResetResults)
        },
        onToggleScanning = { scanViewModel.processIntent(ScanAction.ToggleScanning(settingsUiState.ignoreExcludeList)) },
        onSetIgnoreExcludeList = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetIgnoreExcludeList(it)) },
        onSetCustomBatchSizeMb = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetCustomBatchSizeMb(it)) },
        onSetProxySpecification = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetProxySpecification(it)) },
        onSetShouldUseTor = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUseTor(it)) },
        onSetShouldUploadZips = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadZips(it)) },
        onSetShouldUploadReadableList = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadReadableList(it)) },
        onSetShouldUploadUnreadableList = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadUnreadableList(it)) },
        onSetShouldUploadExcludedList = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadExcludedList(it)) },
        onSetShouldUploadMissingList = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadMissingList(it)) },
        onSetShouldUploadSymlinkList = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadSymlinkList(it)) },
        onSetShouldUploadGetprop = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadGetprop(it)) },
        onSetShouldUploadAppLogs = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetShouldUploadAppLogs(it)) },
        onSetZipEncryption = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetZipEncryption(it)) },
        onSelectService = { settingsViewModel.processIntent(SettingsViewModel.Intent.SelectService(it)) },
        onToggleUploading = {
            val service = settingsUiState.selectedService
            if (service != null) {
                val uploadSettings = UploadViewModel.UploadSettings(
                    customBatchSizeMb = settingsUiState.customBatchSizeMb,
                    proxySpecification = settingsUiState.proxySpecification,
                    shouldUseTor = settingsUiState.shouldUseTor,
                    shouldUploadZips = settingsUiState.shouldUploadZips,
                    shouldUploadReadableList = settingsUiState.shouldUploadReadableList,
                    shouldUploadUnreadableList = settingsUiState.shouldUploadUnreadableList,
                    shouldUploadExcludedList = settingsUiState.shouldUploadExcludedList,
                    shouldUploadMissingList = settingsUiState.shouldUploadMissingList,
                    shouldUploadSymlinkList = settingsUiState.shouldUploadSymlinkList,
                    shouldUploadGetprop = settingsUiState.shouldUploadGetprop,
                    shouldUploadAppLogs = settingsUiState.shouldUploadAppLogs,
                    zipEncryption = settingsUiState.zipEncryption,
                    selectedService = service,
                    onFatalError = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetFatalError(it)) }
                )
                uploadViewModel.processIntent(UploadViewModel.Intent.ToggleUploading(uploadSettings))
            } else {
                showShortToast("No upload service selected")
            }
        },
        onNavigateToQrCode = onNavigateToQrCode,
        onShowHelp = onShowHelp,
        onNavigateToIpInfo = onNavigateToIpInfo,
        showShortToast = showShortToast,
        formatBytes = uploadViewModel::formatBytes,
        onResetFatalError = {
            settingsViewModel.processIntent(SettingsViewModel.Intent.SetFatalError(null))
            uploadViewModel.processIntent(UploadViewModel.Intent.ResetResults)
            scanViewModel.processIntent(ScanAction.ResetResults)
        }
    )
}

/**
 * The content area of the main screen, defining the overall layout and UI components.
 *
 * This Composable uses a [androidx.compose.material3.Scaffold] with a top bar and a scrollable column containing
 * the [hu.muzso.android_system_dumper.presentation.components.ProgressCard], [hu.muzso.android_system_dumper.presentation.components.UploadPanel], and [hu.muzso.android_system_dumper.presentation.components.ResultsCard].
 *
 * @param scanUiState The current state of the system scan.
 * @param settingsUiState The current application settings.
 * @param uploadUiState The current state of the upload process.
 * @param onResetResults Callback to reset all scan and upload results.
 * @param onToggleScanning Callback to start or stop the system scan.
 * @param onSetIgnoreExcludeList Callback to toggle whether the exclusion list is ignored.
 * @param onSetCustomBatchSizeMb Callback to update the custom batch size for uploads.
 * @param onSetProxySpecification Callback to update the proxy configuration.
 * @param onSetShouldUseTor Callback to toggle the use of Tor for uploads.
 * @param onSetShouldUploadZips Callback to toggle uploading files bundled in ZIPs.
 * @param onSetShouldUploadReadableList Callback to toggle uploading the list of readable files.
 * @param onSetShouldUploadUnreadableList Callback to toggle uploading the list of unreadable files.
 * @param onSetShouldUploadExcludedList Callback to toggle uploading the list of excluded files.
 * @param onSetShouldUploadMissingList Callback to toggle uploading the list of missing files.
 * @param onSetShouldUploadSymlinkList Callback to toggle uploading the list of symlinks.
 * @param onSetShouldUploadGetprop Callback to toggle uploading system properties.
 * @param onSetShouldUploadAppLogs Callback to toggle uploading application logs.
 * @param onSetZipEncryption Callback to update the ZIP encryption method.
 * @param onSelectService Callback to change the selected upload service.
 * @param onToggleUploading Callback to start or stop the upload process.
 * @param onNavigateToQrCode Callback to navigate to the QR code screen.
 * @param onShowHelp Callback to show the help screen.
 * @param showShortToast Callback to show a toast message.
 * @param formatBytes Function to format byte counts into human-readable strings.
 * @param onResetFatalError Callback to clear a fatal error state.
 * @param modifier The modifier to apply to this layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    scanUiState: ScanState,
    settingsUiState: SettingsUiState,
    uploadUiState: UploadUiState,
    onResetResults: () -> Unit,
    onToggleScanning: () -> Unit,
    onSetIgnoreExcludeList: (Boolean) -> Unit,
    onSetCustomBatchSizeMb: (String) -> Unit,
    onSetProxySpecification: (String) -> Unit,
    onSetShouldUseTor: (Boolean) -> Unit,
    onSetShouldUploadZips: (Boolean) -> Unit,
    onSetShouldUploadReadableList: (Boolean) -> Unit,
    onSetShouldUploadUnreadableList: (Boolean) -> Unit,
    onSetShouldUploadExcludedList: (Boolean) -> Unit,
    onSetShouldUploadMissingList: (Boolean) -> Unit,
    onSetShouldUploadSymlinkList: (Boolean) -> Unit,
    onSetShouldUploadGetprop: (Boolean) -> Unit,
    onSetShouldUploadAppLogs: (Boolean) -> Unit,
    onSetZipEncryption: (ZipEncryption) -> Unit,
    onSelectService: (UploadRepository) -> Unit,
    onToggleUploading: () -> Unit,
    onNavigateToQrCode: (String) -> Unit,
    onShowHelp: () -> Unit,
    onNavigateToIpInfo: () -> Unit,
    showShortToast: (String) -> Unit,
    formatBytes: (Long) -> String,
    onResetFatalError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var previousMaxScrollValue by remember { mutableIntStateOf(0) }
    var uploadPanelRootBottom by remember { mutableFloatStateOf(0f) }
    var viewportRootBottom by remember { mutableFloatStateOf(0f) }

    // Auto-scroll to bottom when new elements are revealed during upload
    LaunchedEffect(
        scrollState.maxValue,
        uploadUiState.isUploading,
        uploadUiState.downloadUrl
    ) {
        val newMax = scrollState.maxValue
        if (newMax > previousMaxScrollValue) {
            val wasAtBottom = scrollState.value >= (previousMaxScrollValue - 20)
            val isPanelOffScreen = uploadPanelRootBottom > (viewportRootBottom + 10)

            if ((uploadUiState.isUploading || uploadUiState.downloadUrl != null) &&
                (wasAtBottom || isPanelOffScreen)
            ) {
                scrollState.animateScrollTo(newMax)
            }
        }
        previousMaxScrollValue = newMax
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToIpInfo) {
                        Text(
                            text = "IP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShowHelp) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                    }
                    IconButton(onClick = onResetResults) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .onGloballyPositioned {
                    viewportRootBottom = it.positionInRoot().y + it.size.height
                }
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressCard(
                scanUiState = scanUiState,
                ignoreExcludeList = settingsUiState.ignoreExcludeList,
                onIgnoreExcludeListChange = onSetIgnoreExcludeList,
                onToggleScanning = onToggleScanning,
                formatBytes = formatBytes
            )

            UploadPanel(
                modifier = Modifier.onGloballyPositioned {
                    uploadPanelRootBottom = it.positionInRoot().y + it.size.height
                },
                settingsUiState = settingsUiState,
                uploadUiState = uploadUiState,
                filesCount = scanUiState.filesCount,
                onSetCustomBatchSizeMb = onSetCustomBatchSizeMb,
                onSetProxySpecification = onSetProxySpecification,
                onSetShouldUseTor = onSetShouldUseTor,
                onSetShouldUploadZips = onSetShouldUploadZips,
                onSetShouldUploadReadableList = onSetShouldUploadReadableList,
                onSetShouldUploadUnreadableList = onSetShouldUploadUnreadableList,
                onSetShouldUploadExcludedList = onSetShouldUploadExcludedList,
                onSetShouldUploadMissingList = onSetShouldUploadMissingList,
                onSetShouldUploadSymlinkList = onSetShouldUploadSymlinkList,
                onSetShouldUploadGetprop = onSetShouldUploadGetprop,
                onSetShouldUploadAppLogs = onSetShouldUploadAppLogs,
                onSetZipEncryption = onSetZipEncryption,
                onSelectService = onSelectService,
                onToggleUploading = onToggleUploading,
                formatBytes = formatBytes
            )

            val clipboard = LocalClipboard.current
            val uriHandler = LocalUriHandler.current
            val scope = rememberCoroutineScope()

            ResultsCard(
                uploadUiState = uploadUiState,
                shouldUseTor = settingsUiState.shouldUseTor,
                onCopyText = { label, text ->
                    scope.launch {
                        try {
                            clipboard.setClipEntry(ClipData.newPlainText(label, text).toClipEntry())
                            showShortToast("$label copied to clipboard")
                        } catch (_: Exception) {
                            showShortToast("Failed to copy $label")
                        }
                    }
                },
                onNavigateToQrCode = onNavigateToQrCode,
                onOpenUri = { uriHandler.openUri(it) }
            )
        }

        settingsUiState.fatalError?.let { error ->
            FatalErrorDialog(
                error = error,
                onReset = onResetFatalError
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenContentPreview() {
    AndroidSystemDumperTheme {
        MainScreenContent(
            scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
            settingsUiState = SettingsUiState(),
            uploadUiState = UploadUiState(),
            onResetResults = {},
            onToggleScanning = {},
            onSetIgnoreExcludeList = {},
            onSetCustomBatchSizeMb = {},
            onSetProxySpecification = {},
            onSetShouldUseTor = {},
            onSetShouldUploadZips = {},
            onSetShouldUploadReadableList = {},
            onSetShouldUploadUnreadableList = {},
            onSetShouldUploadExcludedList = {},
            onSetShouldUploadMissingList = {},
            onSetShouldUploadSymlinkList = {},
            onSetShouldUploadGetprop = {},
            onSetShouldUploadAppLogs = {},
            onSetZipEncryption = {},
            onSelectService = {},
            onToggleUploading = {},
            onNavigateToQrCode = {},
            onShowHelp = {},
            onNavigateToIpInfo = {},
            showShortToast = {},
            formatBytes = { "$it B" },
            onResetFatalError = {}
        )
    }
}