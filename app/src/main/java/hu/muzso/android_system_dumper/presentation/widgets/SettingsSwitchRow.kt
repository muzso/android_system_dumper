package hu.muzso.android_system_dumper.presentation.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

@Composable
fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview
@Composable
fun SettingsSwitchRowPreview() {
    AndroidSystemDumperTheme {
        SettingsSwitchRow(
            label = "Sample Setting",
            checked = true,
            onCheckedChange = {},
            testTag = "sample_tag"
        )
    }
}
