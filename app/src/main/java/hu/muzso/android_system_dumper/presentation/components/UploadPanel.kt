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
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import hu.muzso.android_system_dumper.presentation.widgets.SettingsDropdownSelector
import hu.muzso.android_system_dumper.presentation.widgets.SettingsSwitchRow
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme
import hu.muzso.android_system_dumper.upload.network.UploadRepository

/**
 * A UI component that provides configuration options and controls for the upload process.
 * 
 * This panel includes settings for the upload service, ZIP encryption, batch size,
 * proxy configuration, and toggles for selecting which data to upload. It also
 * displays the current upload progress and status.
 *
 * @param settingsUiState The current settings UI state.
 * @param uploadUiState The current upload UI state.
 * @param filesCount The number of files found during the scan.
 * @param onSetCustomBatchSizeMb Callback to update the custom batch size.
 * @param onSetProxySpecification Callback to update the proxy specification.
 * @param onSetShouldUseTor Callback to toggle Tor usage.
 * @param onSetShouldUploadZips Callback to toggle ZIP upload.
 * @param onSetShouldUploadReadableList Callback to toggle readable list upload.
 * @param onSetShouldUploadUnreadableList Callback to toggle unreadable list upload.
 * @param onSetShouldUploadExcludedList Callback to toggle excluded list upload.
 * @param onSetShouldUploadMissingList Callback to toggle missing list upload.
 * @param onSetShouldUploadSymlinkList Callback to toggle symlink list upload.
 * @param onSetShouldUploadGetprop Callback to toggle getprop upload.
 * @param onSetShouldUploadAppLogs Callback to toggle app logs upload.
 * @param onSetZipEncryption Callback to update ZIP encryption.
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
    formatBytes: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("step_2_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.step_2_packaging_upload),
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
            SettingsDropdownSelector(
                label = "ZIP encryption",
                items = ZipEncryption.entries,
                selectedItem = settingsUiState.zipEncryption,
                onItemSelected = onSetZipEncryption,
                itemLabel = {
                    when (it) {
                        ZipEncryption.NONE -> "None"
                        ZipEncryption.STANDARD -> "Standard"
                        ZipEncryption.AES -> "AES"
                    }
                }
            )
            OutlinedTextField(
                value = settingsUiState.customBatchSizeMb,
                onValueChange = onSetCustomBatchSizeMb,
                label = { Text(stringResource(R.string.custom_batch_size_mb)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_size_input"),
                singleLine = true
            )
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
            Text(
                text = stringResource(R.string.select_what_to_upload),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            SettingsSwitchRow(
                label = stringResource(R.string.readable_files_bundled_into_zip_archives),
                checked = settingsUiState.shouldUploadZips,
                onCheckedChange = onSetShouldUploadZips,
                testTag = "switch_upload_zips"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.list_of_readable_files),
                checked = settingsUiState.shouldUploadReadableList,
                onCheckedChange = onSetShouldUploadReadableList,
                testTag = "switch_upload_readble"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.list_of_unreadable_files),
                checked = settingsUiState.shouldUploadUnreadableList,
                onCheckedChange = onSetShouldUploadUnreadableList,
                testTag = "switch_upload_unreadable"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.list_of_excluded_files),
                checked = settingsUiState.shouldUploadExcludedList,
                onCheckedChange = onSetShouldUploadExcludedList,
                testTag = "switch_upload_exluded"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.list_of_missing_files),
                checked = settingsUiState.shouldUploadMissingList,
                onCheckedChange = onSetShouldUploadMissingList,
                testTag = "switch_upload_missing"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.list_of_symlinks),
                checked = settingsUiState.shouldUploadSymlinkList,
                onCheckedChange = onSetShouldUploadSymlinkList,
                testTag = "switch_upload_symlink"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.output_of_getprop),
                checked = settingsUiState.shouldUploadGetprop,
                onCheckedChange = onSetShouldUploadGetprop,
                testTag = "switch_upload_getprop"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.upload_logs),
                checked = settingsUiState.shouldUploadAppLogs,
                onCheckedChange = onSetShouldUploadAppLogs,
                testTag = "switch_upload_applogs"
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

            if (uploadUiState.isUploading && uploadUiState.currentZipTotalBytes > 0) {
                val percentage =
                    (uploadUiState.currentZipUploadBytes.toDouble() / uploadUiState.currentZipTotalBytes * 100).coerceIn(
                        0.0,
                        100.0
                    )
                val progressFraction =
                    (uploadUiState.currentZipUploadBytes.toFloat() / uploadUiState.currentZipTotalBytes).coerceIn(
                        0f,
                        1f
                    )
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
                                stringResource(R.string.percent_uploaded),
                                percentage
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
                    text = if (uploadUiState.isUploading) stringResource(R.string.stop) else stringResource(R.string.start),
                    fontWeight = FontWeight.Bold
                )
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
            formatBytes = { "$it B" }
        )
    }
}
