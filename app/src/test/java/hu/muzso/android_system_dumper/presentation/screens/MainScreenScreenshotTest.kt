package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.presentation.RoborazziTestConfigRule
import hu.muzso.android_system_dumper.presentation.ScreenshotTestTheme
import hu.muzso.android_system_dumper.presentation.state.FatalError
import hu.muzso.android_system_dumper.presentation.state.FatalErrorPhase
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
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "$it B" },
                    onResetFatalError = {},
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
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "$it B" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_idle_dark.png")
    }

    @Test
    fun mainScreen_scanning() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.RUNNING,
                        isScanning = true,
                        filesCount = 1234,
                        totalBytes = 567890123L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_scanning.png")
    }

    @Test
    fun mainScreen_scanning_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.RUNNING,
                        isScanning = true,
                        filesCount = 1234,
                        totalBytes = 567890123L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_scanning_dark.png")
    }

    @Test
    fun mainScreen_uploading() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.FINISHED,
                        filesCount = 5000,
                        totalBytes = 1073741824L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(
                        isUploading = true,
                        uploadStatusText = "Archiving batch 2/10...",
                        totalZips = 10,
                        uploadedZips = 1,
                        currentZipUploadBytes = 52428800L,
                        currentZipTotalBytes = 104857600L
                    ),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_uploading.png")
    }

    @Test
    fun mainScreen_uploading_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.FINISHED,
                        filesCount = 5000,
                        totalBytes = 1073741824L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(
                        isUploading = true,
                        uploadStatusText = "Archiving batch 2/10...",
                        totalZips = 10,
                        uploadedZips = 1,
                        currentZipUploadBytes = 52428800L,
                        currentZipTotalBytes = 104857600L
                    ),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_uploading_dark.png")
    }

    @Test
    fun mainScreen_fatalError() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                MainScreenContent(
                    scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
                    settingsUiState = SettingsUiState(
                        fatalError = FatalError("Application crashed due to missing permissions.", FatalErrorPhase.UPLOAD)
                    ),
                    uploadUiState = UploadUiState(),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "$it B" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_fatal_error.png")
    }

    @Test
    fun mainScreen_fatalError_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                MainScreenContent(
                    scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
                    settingsUiState = SettingsUiState(
                        fatalError = FatalError("Application crashed due to missing permissions.", FatalErrorPhase.UPLOAD)
                    ),
                    uploadUiState = UploadUiState(),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "$it B" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_fatal_error_dark.png")
    }

    @Test
    fun mainScreen_success() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.FINISHED,
                        filesCount = 5000,
                        totalBytes = 1073741824L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(
                        isUploading = false,
                        uploadStatusText = "Upload completed successfully",
                        downloadUrl = "https://gofile.io/d/example123",
                        totalZips = 5,
                        uploadedZips = 5,
                        generatedPassphrase = "securePassphrase123"
                    ),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_success.png")
    }

    @Test
    fun mainScreen_success_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.FINISHED,
                        filesCount = 5000,
                        totalBytes = 1073741824L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(
                        isUploading = false,
                        uploadStatusText = "Upload completed successfully",
                        downloadUrl = "https://gofile.io/d/example123",
                        totalZips = 5,
                        uploadedZips = 5,
                        generatedPassphrase = "securePassphrase123"
                    ),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_success_dark.png")
    }

    @Test
    fun mainScreen_partialSuccess() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.FINISHED,
                        filesCount = 5000,
                        totalBytes = 1073741824L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(
                        isUploading = false,
                        uploadStatusText = "Partial success: 3/5 zips uploaded",
                        downloadUrl = "https://gofile.io/d/partial123",
                        totalZips = 5,
                        uploadedZips = 3,
                        generatedPassphrase = "partialPassphrase"
                    ),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_partial_success.png")
    }

    @Test
    fun mainScreen_partialSuccess_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                MainScreenContent(
                    scanUiState = ScanState(
                        scanStatus = ScanStatus.FINISHED,
                        filesCount = 5000,
                        totalBytes = 1073741824L
                    ),
                    settingsUiState = SettingsUiState(),
                    uploadUiState = UploadUiState(
                        isUploading = false,
                        uploadStatusText = "Partial success: 3/5 zips uploaded",
                        downloadUrl = "https://gofile.io/d/partial123",
                        totalZips = 5,
                        uploadedZips = 3,
                        generatedPassphrase = "partialPassphrase"
                    ),
                    onResetResults = {},
                    onToggleScanning = {},
                    onSetIgnoreExcludeList = {},
                    onSetCustomBatchSizeMb = {},
                    onSetProxySpecification = {},
                    onSetShouldUseTor = {},
                    onSetShouldUploadZips = {},
                    onSetShouldUploadFileLists = {},
                    onSetShouldUploadGetprop = {},
                    onSetShouldUploadAppLogs = {},
                    onSetMaxUploadRetries = {},
                    onSetZipEncryption = {},
                    onSetUseDoubleZipping = {},
                    onSelectService = {},
                    onToggleUploading = {},
                    onStartHttpServer = {},
                    onNavigateToQrCode = {},
                    onNavigateToIpInfo = {},
                    onShowHelp = {},
                    showShortToast = {},
                    formatBytes = { "${it / 1024 / 1024} MB" },
                    onResetFatalError = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_screen_partial_success_dark.png")
    }
}
