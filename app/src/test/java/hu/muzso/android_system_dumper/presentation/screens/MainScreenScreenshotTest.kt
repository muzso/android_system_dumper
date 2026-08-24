package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.presentation.RoborazziTestConfigRule
import hu.muzso.android_system_dumper.presentation.ScreenshotTestTheme
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class MainScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziTestConfigRule()

    @Test
    fun mainScreen_idle() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                MainScreenContent(
                    scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
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
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "$it B" },
                    onResetFatalError = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_idle.png")
    }

    @Test
    fun mainScreen_idle_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                MainScreenContent(
                    scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
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
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "$it B" },
                    onResetFatalError = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_idle_dark.png")
    }
}
