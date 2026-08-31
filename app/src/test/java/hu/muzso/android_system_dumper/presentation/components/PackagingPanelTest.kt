package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.model.ZipEncryption
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
@Config(sdk = [34], qualifiers = "w1024dp-h2048dp")
class PackagingPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun batchSizeInputWorks() {
        val batchSize = AtomicReference("")
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                PackagingPanel(
                    settingsUiState = SettingsUiState(customBatchSizeMb = ""),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetCustomBatchSizeMb = { batchSize.set(it) },
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("batch_size_input").performTextInput("500")
        Truth.assertThat(batchSize.get()).isEqualTo("500")
    }

    @Test
    fun encryptionSelectorExists() {
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                PackagingPanel(
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetCustomBatchSizeMb = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithText("ZIP encryption").assertExists()
    }

    @Test
    fun zipsToggleWorks() {
        val toggled = AtomicBoolean(false)
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                PackagingPanel(
                    settingsUiState = SettingsUiState(shouldUploadZips = false),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetCustomBatchSizeMb = {},
                    onSetShouldUploadZips = { toggled.set(it) },
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    formatBytes = { "" }
                )
            }
        }
        
        composeTestRule.onNodeWithTag("switch_upload_zips").performClick()
        Truth.assertThat(toggled.get()).isTrue()
    }

    @Test
    fun allUploadTogglesWork() {
        val fileLists = AtomicBoolean(false)
        val getprop = AtomicBoolean(false)
        val applogs = AtomicBoolean(false)

        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                PackagingPanel(
                    settingsUiState = SettingsUiState(
                        shouldUploadFileLists = false,
                        shouldUploadGetprop = false,
                        shouldUploadAppLogs = false
                    ),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetCustomBatchSizeMb = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = { fileLists.set(it) },
                    onSetShouldUploadGetprop = { getprop.set(it) },
                    onSetShouldUploadAppLogs = { applogs.set(it) },
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    formatBytes = { "" }
                )
            }
        }

        composeTestRule.onNodeWithTag("switch_upload_file_lists").performClick()
        composeTestRule.onNodeWithTag("switch_upload_getprop").performClick()
        composeTestRule.onNodeWithTag("switch_upload_applogs").performClick()

        Truth.assertThat(fileLists.get()).isTrue()
        Truth.assertThat(getprop.get()).isTrue()
        Truth.assertThat(applogs.get()).isTrue()
    }

    @Test
    fun doubleZippingToggleDisabledWhenEncryptionIsNone() {
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                PackagingPanel(
                    settingsUiState = SettingsUiState(zipEncryption = ZipEncryption.NONE),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetCustomBatchSizeMb = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("switch_use_double_zipping").assertIsNotEnabled()
    }

    @Test
    fun doubleZippingToggleEnabledWhenEncryptionIsStandard() {
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                PackagingPanel(
                    settingsUiState = SettingsUiState(zipEncryption = ZipEncryption.STANDARD),
                    uploadUiState = UploadUiState(),
                    filesCount = 10,
                    onSetCustomBatchSizeMb = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    formatBytes = { "" }
                )
            }
        }
        composeTestRule.onNodeWithTag("switch_use_double_zipping").assertIsEnabled()
    }
}
