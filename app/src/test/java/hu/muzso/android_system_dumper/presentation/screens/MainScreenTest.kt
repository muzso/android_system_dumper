package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.presentation.ScanViewModel
import hu.muzso.android_system_dumper.presentation.SettingsViewModel
import hu.muzso.android_system_dumper.presentation.UploadViewModel
import hu.muzso.android_system_dumper.presentation.state.AppState
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1024dp-h2048dp")
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_integrationTest() {
        val scanViewModel = mockk<ScanViewModel>(relaxed = true)
        val settingsViewModel = mockk<SettingsViewModel>(relaxed = true)
        val uploadViewModel = mockk<UploadViewModel>(relaxed = true)

        every { scanViewModel.uiState } returns MutableStateFlow(ScanState())
        every { settingsViewModel.uiState } returns MutableStateFlow(SettingsUiState())
        every { uploadViewModel.uiState } returns MutableStateFlow(UploadUiState())
        every { settingsViewModel.appState } returns MutableStateFlow(AppState.MainScreen)

        composeTestRule.setContent {
            MainScreen(
                scanViewModel = scanViewModel,
                settingsViewModel = settingsViewModel,
                uploadViewModel = uploadViewModel,
                onNavigateToQrCode = {},
                onShowHelp = {},
                onNavigateToIpInfo = {},
                onNavigateToDownload = {},
                showShortToast = {}
            )
        }

        composeTestRule.onNodeWithText("Step 1: Filesystem Scan").assertExists()
    }

    @Test
    fun mainScreenContent_showsAllSections() {
        composeTestRule.setContent {
            MainScreenContent(
                scanUiState = ScanState(),
                settingsUiState = SettingsUiState(),
                uploadUiState = UploadUiState(),
                onResetResults = { },
                onToggleScanning = { },
                onSetIgnoreExcludeList = { },
                onSetCustomBatchSizeMb = { },
                onSetProxySpecification = { },
                onSetShouldUseTor = { },
                onSetShouldUploadZips = { },
                onSetShouldUploadFileLists = { },
                onSetShouldUploadGetprop = { },
                onSetShouldUploadAppLogs = { },
                onSetMaxUploadRetries = { },
                onSetZipEncryption = { },
                onSetUseDoubleZipping = { },
                onSelectService = { },
                onToggleUploading = { },
                onStartHttpServer = { },
                onNavigateToQrCode = { },
                onShowHelp = { },
                onNavigateToIpInfo = { },
                showShortToast = { },
                formatBytes = { "" },
                onResetFatalError = { }
            )
        }

        composeTestRule.onNodeWithText("Step 1: Filesystem Scan").assertExists()
        composeTestRule.onNodeWithText("Step 2: Packaging").assertExists()
        composeTestRule.onNodeWithText("Step 3: File Transfer").assertExists()
    }

    @Test
    fun mainScreenContent_scanButtonTriggersCallback() {
        var toggled = false
        composeTestRule.setContent {
            MainScreenContent(
                scanUiState = ScanState(),
                settingsUiState = SettingsUiState(),
                uploadUiState = UploadUiState(),
                onResetResults = { },
                onToggleScanning = { toggled = true },
                onSetIgnoreExcludeList = { },
                onSetCustomBatchSizeMb = { },
                onSetProxySpecification = { },
                onSetShouldUseTor = { },
                onSetShouldUploadZips = { },
                onSetShouldUploadFileLists = { },
                onSetShouldUploadGetprop = { },
                onSetShouldUploadAppLogs = { },
                onSetMaxUploadRetries = { },
                onSetZipEncryption = { },
                onSetUseDoubleZipping = { },
                onSelectService = { },
                onToggleUploading = { },
                onStartHttpServer = { },
                onNavigateToQrCode = { },
                onShowHelp = { },
                onNavigateToIpInfo = { },
                showShortToast = { },
                formatBytes = { "" },
                onResetFatalError = { }
            )
        }

        composeTestRule.onNodeWithTag("scan_button").performClick()
        assert(toggled)
    }

    @Test
    fun mainScreenContent_uploadButtonTriggersCallback() {
        var toggled = false
        composeTestRule.setContent {
            MainScreenContent(
                scanUiState = ScanState(scanStatus = ScanStatus.FINISHED, filesCount = 10),
                settingsUiState = SettingsUiState(),
                uploadUiState = UploadUiState(),
                onResetResults = { },
                onToggleScanning = { },
                onSetIgnoreExcludeList = { },
                onSetCustomBatchSizeMb = { },
                onSetProxySpecification = { },
                onSetShouldUseTor = { },
                onSetShouldUploadZips = { },
                onSetShouldUploadFileLists = { },
                onSetShouldUploadGetprop = { },
                onSetShouldUploadAppLogs = { },
                onSetMaxUploadRetries = { },
                onSetZipEncryption = { },
                onSetUseDoubleZipping = { },
                onSelectService = { },
                onToggleUploading = { toggled = true },
                onStartHttpServer = { },
                onNavigateToQrCode = { },
                onShowHelp = { },
                onNavigateToIpInfo = { },
                showShortToast = { },
                formatBytes = { "" },
                onResetFatalError = { }
            )
        }

        composeTestRule.onNodeWithTag("upload_button").performClick()
        assert(toggled)
    }
}