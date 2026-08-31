package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import hu.muzso.android_system_dumper.presentation.widgets.SettingsDropdownSelector
import hu.muzso.android_system_dumper.presentation.widgets.SettingsSwitchRow
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

/**
 * A UI component that provides configuration options and controls for the file transfer process.
 *
 * This panel includes settings for the upload service, proxy configuration, and Tor usage. 
 * It also displays the current upload progress and provides buttons to start/stop the 
 * upload or start the local HTTP server.
 *
 * @param settingsUiState The current settings UI state.
 * @param uploadUiState The current upload UI state.
 * @param filesCount The number of files found during the scan.
 * @param onSetProxySpecification Callback to update the proxy specification.
 * @param onSetShouldUseTor Callback to toggle Tor usage.
 * @param onSelectService Callback to change the upload service.
 * @param onToggleUploading Callback to start or stop the upload.
 * @param formatBytes Function to format byte values for display.
 * @param modifier The modifier to apply to this component.
 */
@Composable
fun UploadPanel(
    settingsUiState: SettingsUiState,
    uploadUiState: UploadUiState,
    filesCount: Int,
    onSetProxySpecification: (String) -> Unit,
    onSetShouldUseTor: (Boolean) -> Unit,
    onSelectService: (UploadRepository) -> Unit,
    onToggleUploading: () -> Unit,
    onStartHttpServer: () -> Unit,
    formatBytes: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("step_3_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.step_3_file_transfer),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            settingsUiState.selectedService?.let { selectedService ->
                SettingsDropdownSelector(
                    label = stringResource(R.string.file_sharing_service),
                    items = settingsUiState.services,
                    selectedItem = selectedService,
                    onItemSelected = onSelectService,
                    itemLabel = { it.name }
                )
            }
            OutlinedTextField(
                value = settingsUiState.proxySpecification,
                onValueChange = onSetProxySpecification,
                label = { Text(stringResource(R.string.proxy)) },
                enabled = !settingsUiState.shouldUseTor,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proxy_input"),
                singleLine = true
            )
            SettingsSwitchRow(
                label = stringResource(R.string.use_tor_network),
                checked = settingsUiState.shouldUseTor,
                onCheckedChange = onSetShouldUseTor,
                testTag = "switch_use_tor"
            )

            if (uploadUiState.totalZips > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = stringResource(R.string.progress_of_file_uploads),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uploadUiState.uploadedZips} / ${uploadUiState.totalZips}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (uploadUiState.isUploading && uploadUiState.totalZips > 0) {
                val percentage = if (uploadUiState.currentZipTotalBytes > 0) {
                    (uploadUiState.currentZipUploadBytes.toDouble() / uploadUiState.currentZipTotalBytes * 100).coerceIn(
                        0.0,
                        100.0
                    )
                } else 0.0

                val progressFraction = if (uploadUiState.currentZipTotalBytes > 0) {
                    (uploadUiState.currentZipUploadBytes.toFloat() / uploadUiState.currentZipTotalBytes).coerceIn(
                        0f,
                        1f
                    )
                } else 0f
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        drawStopIndicator = {},
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(
                                java.util.Locale.US,
                                stringResource(R.string.percent_progress),
                                percentage,
                                stringResource(R.string.status_uploaded)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                R.string.x_of_y,
                                formatBytes(uploadUiState.currentZipUploadBytes),
                                formatBytes(uploadUiState.currentZipTotalBytes)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uploadUiState.uploadStatusText.isNotEmpty()) {
                Text(
                    text = uploadUiState.uploadStatusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onToggleUploading,
                enabled = filesCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("upload_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uploadUiState.isUploading) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (uploadUiState.isUploading) stringResource(R.string.stop) else stringResource(R.string.upload),
                    fontWeight = FontWeight.Bold
                )
            }

            if (!uploadUiState.isUploading) {
                Text(
                    text = stringResource(R.string.or),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onStartHttpServer,
                    enabled = filesCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("http_server_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.start_http_server),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun UploadPanelPreview() {
    AndroidSystemDumperTheme {
        UploadPanel(
            settingsUiState = SettingsUiState(),
            uploadUiState = UploadUiState(),
            filesCount = 10,
            onSetProxySpecification = {},
            onSetShouldUseTor = {},
            onSelectService = {},
            onToggleUploading = {},
            onStartHttpServer = {},
            formatBytes = { "$it B" }
        )
    }
}
