package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.presentation.widgets.ScanButton
import hu.muzso.android_system_dumper.presentation.widgets.SettingsSwitchRow
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

/**
 * A UI component that displays the progress of the filesystem scan.
 * 
 * This card shows the current number of files found and the total size scanned.
 * It provides a toggle for ignoring the exclusion list and a button to start
 * or stop the scan.
 *
 * @param scanUiState The current state of the system scan.
 * @param ignoreExcludeList Whether the exclusion list is currently being ignored.
 * @param onIgnoreExcludeListChange Callback when the ignore-exclusion toggle is changed.
 * @param onToggleScanning Callback to start or stop the system scan.
 * @param formatBytes Function to format byte counts for display.
 * @param modifier The modifier to apply to this component.
 */
@Composable
fun ProgressCard(
    scanUiState: ScanState,
    ignoreExcludeList: Boolean,
    onIgnoreExcludeListChange: (Boolean) -> Unit,
    onToggleScanning: () -> Unit,
    formatBytes: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("step_1_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.step_1_filesystem_scan),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row {
                        Text(
                            text = stringResource(R.string.no_of_readable_files),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = scanUiState.filesCount.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row {
                        Text(
                            text = stringResource(R.string.total_size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatBytes(scanUiState.totalBytes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (scanUiState.scanStatus) {
                                ScanStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                                ScanStatus.FINISHED -> MaterialTheme.colorScheme.secondaryContainer
                                ScanStatus.ABORTED -> MaterialTheme.colorScheme.errorContainer
                                ScanStatus.IDLE -> MaterialTheme.colorScheme.outlineVariant
                                is ScanStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                            }
                        )
                        .padding(horizontal = 12.dp, 8.dp)
                ) {
                    Text(
                        text = when (scanUiState.scanStatus) {
                            ScanStatus.RUNNING -> stringResource(R.string.scanning)
                            ScanStatus.FINISHED -> stringResource(R.string.finished)
                            ScanStatus.ABORTED -> stringResource(R.string.aborted)
                            ScanStatus.IDLE -> stringResource(R.string.ready)
                            is ScanStatus.ERROR -> stringResource(R.string.error)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (scanUiState.scanStatus) {
                            ScanStatus.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                            ScanStatus.FINISHED -> MaterialTheme.colorScheme.onSecondaryContainer
                            ScanStatus.ABORTED -> MaterialTheme.colorScheme.onErrorContainer
                            ScanStatus.IDLE -> MaterialTheme.colorScheme.onSurface
                            is ScanStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (scanUiState.isScanning) CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(28.dp),
                strokeWidth = 3.dp
            )
            SettingsSwitchRow(
                label = stringResource(R.string.ignore_exclude_list),
                checked = ignoreExcludeList,
                onCheckedChange = onIgnoreExcludeListChange,
                testTag = "switch_upload_ignore_exclude"
            )
            ScanButton(
                isScanning = scanUiState.isScanning,
                onClick = onToggleScanning
            )
        }
    }
}

@Preview
@Composable
fun ProgressCardPreview() {
    AndroidSystemDumperTheme {
        ProgressCard(
            scanUiState = ScanState(
                scanStatus = ScanStatus.IDLE,
                isScanning = false,
                filesCount = 10,
                totalBytes = 1024L
            ),
            ignoreExcludeList = false,
            onIgnoreExcludeListChange = {},
            onToggleScanning = {},
            formatBytes = { "$it B" }
        )
    }
}
