package hu.muzso.android_system_dumper.presentation.widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

@Composable
fun ScanButton(
    isScanning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("scan_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = if (isScanning) stringResource(R.string.stop) else stringResource(R.string.start),
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
fun ScanButtonPreview() {
    AndroidSystemDumperTheme {
        ScanButton(isScanning = false, onClick = {})
    }
}

@Preview
@Composable
fun ScanButtonScanningPreview() {
    AndroidSystemDumperTheme {
        ScanButton(isScanning = true, onClick = {})
    }
}
