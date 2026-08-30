package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.model.IpInfo
import hu.muzso.android_system_dumper.presentation.IpInfoViewModel
import hu.muzso.android_system_dumper.presentation.SettingsViewModel
import hu.muzso.android_system_dumper.presentation.state.IpInfoUiState
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.utils.drawVerticalScrollbar
import hu.muzso.android_system_dumper.presentation.widgets.SettingsDropdownSelector
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

@Composable
fun IpInfoScreen(
    viewModel: IpInfoViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(settingsUiState.selectedIpSource) {
        viewModel.fetchIpInfo(settingsUiState.selectedIpSource)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IpInfoContent(
        uiState = uiState,
        settingsUiState = settingsUiState,
        onSourceSelected = { settingsViewModel.processIntent(SettingsViewModel.Intent.SetSelectedIpSource(it)) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpInfoContent(
    uiState: IpInfoUiState,
    settingsUiState: SettingsUiState,
    onSourceSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val torCheckerScreenTitle = stringResource(R.string.tor_checker_screen)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(torCheckerScreenTitle) },
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
                .drawVerticalScrollbar(scrollState)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SourceSelectionCard(
                selectedSource = settingsUiState.selectedIpSource,
                availableSources = settingsUiState.availableIpSources,
                onSourceSelected = onSourceSelected
            )

            when (uiState) {
                is IpInfoUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
                }
                is IpInfoUiState.Success -> {
                    IpInfoCard(
                        data = uiState.ipInfo.data
                    )
                }
                is IpInfoUiState.Error -> {
                    IpInfoCard(error = uiState.message)
                }
            }
        }
    }
}

@Composable
private fun SourceSelectionCard(
    selectedSource: String,
    availableSources: List<String>,
    onSourceSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsDropdownSelector(
                label = "Source",
                items = availableSources,
                selectedItem = selectedSource,
                onItemSelected = onSourceSelected,
                itemLabel = { it }
            )
        }
    }
}

@Composable
private fun IpInfoCard(
    data: Map<String, Any>? = null,
    error: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (data != null) {
                IpInfoSection(data = data)
            } else if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun IpInfoSection(
    data: Map<String, Any>,
    indentLevel: Int = 0
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val entries = data.entries.toList()
        entries.forEachIndexed { index, entry ->
            val value = entry.value
            if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                IpInfoNestedSection(
                    label = entry.key,
                    data = value as Map<String, Any>,
                    indentLevel = indentLevel
                )
            } else {
                PropertyRow(
                    label = entry.key,
                    value = value.toString(),
                    indentLevel = indentLevel
                )
            }
            
            if (indentLevel == 0 && index < entries.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun IpInfoNestedSection(
    label: String,
    data: Map<String, Any>,
    indentLevel: Int
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = (indentLevel * 16).dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        IpInfoSection(data = data, indentLevel = indentLevel + 1)
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    indentLevel: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun IpInfoScreenPreview() {
    AndroidSystemDumperTheme {
        IpInfoContent(
            uiState = IpInfoUiState.Success(
                IpInfo(
                    sourceUrl = "https://example.com",
                    data = mapOf(
                        "Ip" to "1.2.3.4",
                        "Country" to "Earth",
                        "Connection" to mapOf(
                            "Isp" to "Example ISP",
                            "Type" to "Fiber"
                        )
                    )
                )
            ),
            settingsUiState = SettingsUiState(
                selectedIpSource = "https://example.com",
                availableIpSources = listOf("https://example.com")
            ),
            onSourceSelected = {},
            onBack = {}
        )
    }
}
