package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import hu.muzso.android_system_dumper.presentation.widgets.SettingsDropdownSelector
import hu.muzso.android_system_dumper.presentation.widgets.SettingsSwitchRow
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

/**
 * A UI component that provides configuration options for packaging the scanned data.
 * 
 * This panel includes settings for ZIP encryption, batch size, and toggles for 
 * selecting which data types (ZIPs, file lists, logs, etc.) to include in the 
 * transfer process.
 *
 * @param settingsUiState The current settings UI state.
 * @param uploadUiState The current upload UI state.
 * @param filesCount The number of files found during the scan.
 * @param onSetCustomBatchSizeMb Callback to update the custom batch size.
 * @param onSetShouldUploadZips Callback to toggle ZIP upload.
 * @param onSetShouldUploadReadableList Callback to toggle readable list upload.
 * @param onSetShouldUploadUnreadableList Callback to toggle unreadable list upload.
 * @param onSetShouldUploadExcludedList Callback to toggle excluded list upload.
 * @param onSetShouldUploadMissingList Callback to toggle missing list upload.
 * @param onSetShouldUploadSymlinkList Callback to toggle symlink list upload.
 * @param onSetShouldUploadGetprop Callback to toggle getprop upload.
 * @param onSetShouldUploadAppLogs Callback to toggle app logs upload.
 * @param onSetZipEncryption Callback to update ZIP encryption.
 * @param onSetUseDoubleZipping Callback to toggle double-zipping.
 * @param formatBytes Function to format byte values for display.
 * @param modifier The modifier to apply to this component.
 */
@Composable
fun PackagingPanel(
    settingsUiState: SettingsUiState,
    uploadUiState: UploadUiState,
    filesCount: Int,
    onSetCustomBatchSizeMb: (String) -> Unit,
    onSetShouldUploadZips: (Boolean) -> Unit,
    onSetShouldUploadReadableList: (Boolean) -> Unit,
    onSetShouldUploadUnreadableList: (Boolean) -> Unit,
    onSetShouldUploadExcludedList: (Boolean) -> Unit,
    onSetShouldUploadMissingList: (Boolean) -> Unit,
    onSetShouldUploadSymlinkList: (Boolean) -> Unit,
    onSetShouldUploadGetprop: (Boolean) -> Unit,
    onSetShouldUploadAppLogs: (Boolean) -> Unit,
    onSetZipEncryption: (ZipEncryption) -> Unit,
    onSetUseDoubleZipping: (Boolean) -> Unit,
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
                text = stringResource(R.string.step_2_packaging),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            SettingsDropdownSelector(
                label = stringResource(R.string.zip_encryption),
                items = ZipEncryption.entries,
                selectedItem = settingsUiState.zipEncryption,
                onItemSelected = onSetZipEncryption,
                itemLabel = { it.name }
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
            SettingsSwitchRow(
                label = stringResource(R.string.use_double_zipping),
                checked = settingsUiState.useDoubleZipping,
                onCheckedChange = onSetUseDoubleZipping,
                enabled = settingsUiState.zipEncryption != ZipEncryption.NONE,
                testTag = "switch_use_double_zipping"
            )
            Text(
                text = stringResource(R.string.select_what_to_package),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            SettingsSwitchRow(
                label = stringResource(R.string.readable_files_filesystem),
                checked = settingsUiState.shouldUploadZips,
                onCheckedChange = onSetShouldUploadZips,
                testTag = "switch_upload_zips"
            )
            SettingsSwitchRow(
                label = stringResource(R.string.list_of_readable_files),
                checked = settingsUiState.shouldUploadReadableList,
                onCheckedChange = onSetShouldUploadReadableList,
                testTag = "switch_upload_readable"
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
                testTag = "switch_upload_excluded"
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
        }
    }
}

@Preview
@Composable
fun PackagingPanelPreview() {
    AndroidSystemDumperTheme {
        PackagingPanel(
            settingsUiState = SettingsUiState(),
            uploadUiState = UploadUiState(),
            filesCount = 10,
            onSetCustomBatchSizeMb = {},
            onSetShouldUploadZips = {},
            onSetShouldUploadReadableList = {},
            onSetShouldUploadUnreadableList = {},
            onSetShouldUploadExcludedList = {},
            onSetShouldUploadMissingList = {},
            onSetShouldUploadSymlinkList = {},
            onSetShouldUploadGetprop = {},
            onSetShouldUploadAppLogs = {},
            onSetZipEncryption = {},
            onSetUseDoubleZipping = {},
            formatBytes = { "$it B" }
        )
    }
}
