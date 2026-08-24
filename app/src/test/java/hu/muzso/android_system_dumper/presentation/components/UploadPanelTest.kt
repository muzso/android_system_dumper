package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1024dp-h2048dp")
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
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = { toggled.set(it) },
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
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("switch_use_tor").performClick()
        Truth.assertThat(toggled.get()).isTrue()
    }

    @Test
    fun zipsToggleWorks() {
        val toggled = AtomicBoolean(false)
        composeTestRule.setContent {
            UploadPanel(
                settingsUiState = SettingsUiState(shouldUploadZips = false),
                uploadUiState = UploadUiState(),
                filesCount = 10,
                onSetCustomBatchSizeMb = {},
                onSetProxySpecification = {},
                onSetShouldUseTor = {},
                onSetShouldUploadZips = { toggled.set(it) },
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
                formatBytes = { "" }
            )
        }
        
        composeTestRule.onNodeWithTag("switch_upload_zips")
            .performClick()
        
        Truth.assertThat(toggled.get()).isTrue()
    }

    @Test
    fun allUploadTogglesWork() {
        val results = mutableMapOf<String, Boolean>()
        composeTestRule.setContent {
            UploadPanel(
                settingsUiState = SettingsUiState(
                    shouldUploadReadableList = false,
                    shouldUploadUnreadableList = false,
                    shouldUploadExcludedList = false,
                    shouldUploadMissingList = false,
                    shouldUploadSymlinkList = false,
                    shouldUploadGetprop = false,
                    shouldUploadAppLogs = false
                ),
                uploadUiState = UploadUiState(),
                filesCount = 10,
                onSetCustomBatchSizeMb = {},
                onSetProxySpecification = {},
                onSetShouldUseTor = {},
                onSetShouldUploadZips = {},
                onSetShouldUploadReadableList = { results["readable"] = it },
                onSetShouldUploadUnreadableList = { results["unreadable"] = it },
                onSetShouldUploadExcludedList = { results["excluded"] = it },
                onSetShouldUploadMissingList = { results["missing"] = it },
                onSetShouldUploadSymlinkList = { results["symlink"] = it },
                onSetShouldUploadGetprop = { results["getprop"] = it },
                onSetShouldUploadAppLogs = { results["applogs"] = it },
                onSetZipEncryption = {},
                onSelectService = {},
                onToggleUploading = {},
                formatBytes = { "" }
            )
        }

        composeTestRule.onNodeWithTag("switch_upload_readble").performClick()
        composeTestRule.onNodeWithTag("switch_upload_unreadable").performClick()
        composeTestRule.onNodeWithTag("switch_upload_exluded").performClick()
        composeTestRule.onNodeWithTag("switch_upload_missing").performClick()
        composeTestRule.onNodeWithTag("switch_upload_symlink").performClick()
        composeTestRule.onNodeWithTag("switch_upload_getprop").performClick()
        composeTestRule.onNodeWithTag("switch_upload_applogs").performClick()

        Truth.assertThat(results["readable"]).isTrue()
        Truth.assertThat(results["unreadable"]).isTrue()
        Truth.assertThat(results["excluded"]).isTrue()
        Truth.assertThat(results["missing"]).isTrue()
        Truth.assertThat(results["symlink"]).isTrue()
        Truth.assertThat(results["getprop"]).isTrue()
        Truth.assertThat(results["applogs"]).isTrue()
    }
}
