package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UploadPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startButtonDisabledWhenNoFiles() {
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                UploadPanel(
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(),
                    filesCount = 0,
                    onSetProxySpecification = {},
                    onSetMaxUploadRetries = {},
                    onSetShouldUseTor = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("upload_button").assertIsNotEnabled()
    }

    @Test
    fun showsProgressWhenUploading() {
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                UploadPanel(
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(
                        isUploading = true,
                        uploadStatusText = "Uploading batch 1"
                    ),
                    filesCount = 10,
                    onSetProxySpecification = {},
                    onSetMaxUploadRetries = {},
                    onSetShouldUseTor = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithText("Uploading batch 1").assertExists()
    }

    @Test
    fun torToggleWorks() {
        val toggled = AtomicBoolean(false)
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                UploadPanel(
                    settingsUiState = SettingsUiState(shouldUseTor = false),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetProxySpecification = {},
                    onSetMaxUploadRetries = {},
                    onSetShouldUseTor = { toggled.set(it) },
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("switch_use_tor").performClick()
        Truth.assertThat(toggled.get()).isTrue()
    }

    @Test
    fun proxyInputWorks() {
        val proxy = AtomicReference("")
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                UploadPanel(
                    settingsUiState = SettingsUiState(proxySpecification = "", shouldUseTor = false),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetProxySpecification = { proxy.set(it) },
                    onSetMaxUploadRetries = {},
                    onSetShouldUseTor = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("proxy_input").performTextInput("127.0.0.1:8080")
        Truth.assertThat(proxy.get()).isEqualTo("127.0.0.1:8080")
    }

    @Test
    fun maxUploadRetriesInputWorks() {
        val retries = AtomicReference("")
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                UploadPanel(
                    settingsUiState = SettingsUiState(maxUploadRetries = ""),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetProxySpecification = {},
                    onSetMaxUploadRetries = { retries.set(it) },
                    onSetShouldUseTor = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("max_upload_retries_input").performTextInput("10")
        Truth.assertThat(retries.get()).isEqualTo("10")
    }

    @Test
    fun httpServerButtonWorks() {
        val clicked = AtomicBoolean(false)
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                UploadPanel(
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetProxySpecification = {},
                    onSetMaxUploadRetries = {},
                    onSetShouldUseTor = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = { clicked.set(true) },
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("http_server_button").performClick()
        Truth.assertThat(clicked.get()).isTrue()
    }
}
