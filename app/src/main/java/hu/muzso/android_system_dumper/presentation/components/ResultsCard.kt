package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme

private val jetbrainsMonoFontFamily = FontFamily(
    Font(resId = R.font.jetbrains_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.jetbrains_mono_bold, weight = FontWeight.Bold)
)

/**
 * A UI component that displays the results of the upload process.
 * 
 * This card shows information about successful uploads, including the download URL,
 * total bytes uploaded, and the time taken. It also provides actions to copy the URL,
 * open it in a browser, or generate a QR code for easy sharing. If the upload is
 * encrypted, it displays the generated passphrase.
 *
 * @param uploadUiState The current upload UI state.
 * @param shouldUseTor Whether Tor was used for the upload (affects URL formatting).
 * @param onCopyText Callback to copy text to the clipboard.
 * @param onNavigateToQrCode Callback to navigate to the QR code screen.
 * @param onOpenUri Callback to open a URI in the browser.
 * @param modifier The modifier to apply to this component.
 */
@Composable
fun ResultsCard(
    uploadUiState: UploadUiState,
    shouldUseTor: Boolean,
    onCopyText: (String, String) -> Unit,
    onNavigateToQrCode: (String) -> Unit,
    onOpenUri: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val urlLabel = stringResource(R.string.url)

    AnimatedVisibility(
        visible = uploadUiState.downloadUrl != null,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .testTag("step_4_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.step_4_finished),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                uploadUiState.downloadUrl?.let { uri ->
                    Text(
                        text = stringResource(R.string.files_can_be_downloaded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            text = uri,
                            style = if (shouldUseTor) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = TextDecoration.Underline
                            ),
                            fontFamily = jetbrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                                .fillMaxWidth()
                                .clickable {
                                    if (!shouldUseTor) {
                                        onOpenUri(uri)
                                    }
                                }
                        )
                        IconButton(
                            onClick = { onCopyText(urlLabel, uri) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.copy_to_clipboard),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onNavigateToQrCode(uri) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = stringResource(R.string.view_as_qr_code),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    uploadUiState.generatedPassphrase?.let { passphrase ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.zip_passphrase) + ":",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            val zipPassphraseLabel = stringResource(R.string.passphrase)
                            IconButton(
                                onClick = { onCopyText(zipPassphraseLabel, passphrase) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_to_clipboard),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onNavigateToQrCode(passphrase) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode,
                                    contentDescription = "View as QR code",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ResultsCardPreview() {
    AndroidSystemDumperTheme {
        ResultsCard(
            uploadUiState = UploadUiState(
                downloadUrl = "https://example.com/download",
                generatedPassphrase = "sample_passphrase"
            ),
            shouldUseTor = false,
            onCopyText = { _, _ -> },
            onNavigateToQrCode = {},
            onOpenUri = {}
        )
    }
}
