package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.presentation.SettingsViewModel
import hu.muzso.android_system_dumper.presentation.utils.drawVerticalScrollbar
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

@Composable
fun HelpScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    HelpContent(
        exclusionList = uiState.exclusionList,
        discoveryRoots = uiState.discoveryRoots,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpContent(
    exclusionList: List<String>,
    discoveryRoots: List<String>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_instructions)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("help_back_button")) {
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
                .drawVerticalScrollbar(scrollState)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            HelpSection(title = stringResource(R.string.help_app_description_title), description = stringResource(R.string.help_app_description_description, stringResource(R.string.app_name)))
            HelpSection(title = stringResource(R.string.help_input_fields_controls_title), description = stringResource(R.string.help_input_fields_controls_description,
                integerResource(R.integer.custom_batch_size_mb_min), integerResource(R.integer.custom_batch_size_mb_max)))
            HelpSection(title = stringResource(R.string.help_features_title), description = stringResource(R.string.help_features_description))
            HelpSection(title = stringResource(R.string.help_privacy_title), description = stringResource(R.string.help_privacy_description))
            HelpSection(title = stringResource(R.string.help_exclusion_list_title), description = exclusionList.joinToString(separator = "\n• ", prefix = stringResource(R.string.help_exclusion_list_description) + "\n\n• "))
            HelpSection(title = stringResource(R.string.help_discovery_roots_title), description = discoveryRoots.joinToString(separator = "\n• ", prefix = stringResource(R.string.help_discovery_roots_description) + "\n\n• "))
        }
    }
}

@Preview
@Composable
fun HelpScreenPreview() {
    AndroidSystemDumperTheme {
        HelpContent(
            exclusionList = listOf("/bugreports/", "/cache/"),
            discoveryRoots = listOf("/", "/acct"),
            onBack = {}
        )
    }
}

@Composable
fun HelpSection(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        }
    }
}
