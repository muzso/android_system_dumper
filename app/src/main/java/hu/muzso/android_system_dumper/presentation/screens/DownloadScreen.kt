package hu.muzso.android_system_dumper.presentation.screens

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.presentation.DownloadViewModel
import hu.muzso.android_system_dumper.presentation.SettingsViewModel
import hu.muzso.android_system_dumper.presentation.state.DownloadUiState
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.widgets.SettingsDropdownSelector
import kotlinx.coroutines.launch

private val jetbrainsMonoFontFamily = FontFamily(
    Font(resId = R.font.jetbrains_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.jetbrains_mono_bold, weight = FontWeight.Bold)
)

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToQrCode: (String) -> Unit,
    showShortToast: (String) -> Unit,
    formatBytes: (Long) -> String
) {
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val selectedService = settingsUiState.selectedService ?: settingsUiState.services.first()
        val uploadSettings = UploadParameters(
            customBatchSizeMb = settingsUiState.customBatchSizeMb.toIntOrNull() ?: SettingsUiState.DEFAULT_CUSTOM_BATCH_SIZE_MB.toInt(),
            proxySpecification = settingsUiState.proxySpecification,
            shouldUseTor = false,
            shouldUploadZips = settingsUiState.shouldUploadZips,
            shouldUploadFileLists = settingsUiState.shouldUploadFileLists,
            shouldUploadGetprop = settingsUiState.shouldUploadGetprop,
            shouldUploadAppLogs = settingsUiState.shouldUploadAppLogs,
            maxUploadRetries = settingsUiState.maxUploadRetries.toIntOrNull() ?: SettingsUiState.DEFAULT_MAX_UPLOAD_RETRIES.toInt(),
            zipEncryption = settingsUiState.zipEncryption,
            useDoubleZipping = settingsUiState.useDoubleZipping,
            selectedService = selectedService,
            maxBatches = 0
        )
        viewModel.startServer(uploadSettings)
    }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    DownloadContent(
        uiState = uiState,
        onIpSelected = { viewModel.selectIp(it) },
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
        onBack = onBack,
        formatBytes = formatBytes
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadContent(
    uiState: DownloadUiState,
    onIpSelected: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onNavigateToQrCode: (String) -> Unit,
    onBack: () -> Unit,
    formatBytes: (Long) -> String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsDropdownSelector(
                        label = stringResource(R.string.select_network_interface),
                        items = uiState.localIps,
                        selectedItem = uiState.selectedIp,
                        onItemSelected = onIpSelected,
                        itemLabel = { if (it.isNotEmpty()) "$it:${uiState.serverPort}" else "" }
                    )

                    uiState.generatedPassphrase?.let { passphrase ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.zip_passphrase) + ":",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = passphrase,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = jetbrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            val zipPassphraseLabel = stringResource(R.string.passphrase)
                            IconButton(
                                onClick = { onCopyText(zipPassphraseLabel, passphrase) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_to_clipboard),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onNavigateToQrCode(passphrase) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode,
                                    contentDescription = "View as QR code",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            uiState.qrBitmap?.let { bitmap ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .size(256.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Download QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (uiState.selectedIp.isNotEmpty()) {
                        val url = "http://${uiState.selectedIp}:${uiState.serverPort}/"
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (uiState.totalCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row {
                            Text(
                                text = stringResource(R.string.progress_of_file_downloads),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.successCount} / ${uiState.totalCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val percentage = if (uiState.totalBytes > 0) {
                            (uiState.currentBytes.toDouble() / uiState.totalBytes * 100).coerceIn(0.0, 100.0)
                        } else 0.0

                        val progressFraction = if (uiState.totalBytes > 0) {
                            (uiState.currentBytes.toFloat() / uiState.totalBytes).coerceIn(0f, 1f)
                        } else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                drawStopIndicator = {},
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, stringResource(R.string.percent_progress), percentage, stringResource(R.string.status_downloaded)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.x_of_y, formatBytes(uiState.currentBytes), formatBytes(uiState.totalBytes)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.statusText.isNotEmpty()) {
                            Text(
                                text = uiState.statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
