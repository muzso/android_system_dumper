package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import hu.muzso.android_system_dumper.platform.DefaultQrGenerator
import hu.muzso.android_system_dumper.presentation.RoborazziTestConfigRule
import hu.muzso.android_system_dumper.presentation.ScreenshotTestTheme
import hu.muzso.android_system_dumper.presentation.state.DownloadUiState
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class DownloadScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziTestConfigRule()

    private val qrGenerator = DefaultQrGenerator(mockk(relaxed = true))

    @Test
    fun downloadScreen_idle() {
        val bitmap = qrGenerator.generateQrCode("http://192.168.1.100:8080", 512)
        composeTestRule.setContent {
            ScreenshotTestTheme {
                DownloadContent(
                    uiState = DownloadUiState(
                        serverPort = 8080,
                        localIps = listOf("192.168.1.100", "10.0.0.1"),
                        selectedIp = "192.168.1.100",
                        qrBitmap = bitmap,
                        generatedPassphrase = "sample_passphrase"
                    ),
                    onIpSelected = {},
                    onCopyText = { _, _ -> },
                    onNavigateToQrCode = {},
                    onBack = {},
                    formatBytes = { "$it B" }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/download_screen_idle.png")
    }

    @Test
    fun downloadScreen_inProgress() {
        val bitmap = qrGenerator.generateQrCode("http://192.168.1.100:8080", 512)
        composeTestRule.setContent {
            ScreenshotTestTheme {
                DownloadContent(
                    uiState = DownloadUiState(
                        serverPort = 8080,
                        localIps = listOf("192.168.1.100"),
                        selectedIp = "192.168.1.100",
                        qrBitmap = bitmap,
                        successCount = 1,
                        totalCount = 5,
                        currentFileName = "batch_2.zip",
                        currentBytes = 52428800,
                        totalBytes = 104857600,
                        statusText = "Downloading batch_2.zip...",
                        generatedPassphrase = "sample_passphrase"
                    ),
                    onIpSelected = {},
                    onCopyText = { _, _ -> },
                    onNavigateToQrCode = {},
                    onBack = {},
                    formatBytes = { "${it / 1024 / 1024} MB" }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/download_screen_in_progress.png")
    }

    @Test
    fun downloadScreen_inProgress_dark() {
        val bitmap = qrGenerator.generateQrCode("http://192.168.1.100:8080", 512)
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                DownloadContent(
                    uiState = DownloadUiState(
                        serverPort = 8080,
                        localIps = listOf("192.168.1.100"),
                        selectedIp = "192.168.1.100",
                        qrBitmap = bitmap,
                        successCount = 1,
                        totalCount = 5,
                        currentFileName = "batch_2.zip",
                        currentBytes = 52428800,
                        totalBytes = 104857600,
                        statusText = "Downloading batch_2.zip...",
                        generatedPassphrase = "sample_passphrase"
                    ),
                    onIpSelected = {},
                    onCopyText = { _, _ -> },
                    onNavigateToQrCode = {},
                    onBack = {},
                    formatBytes = { "${it / 1024 / 1024} MB" }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/download_screen_in_progress_dark.png")
    }

    @Test
    fun downloadScreen_idle_dark() {
        val bitmap = qrGenerator.generateQrCode("http://192.168.1.100:8080", 512)
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                DownloadContent(
                    uiState = DownloadUiState(
                        serverPort = 8080,
                        localIps = listOf("192.168.1.100", "10.0.0.1"),
                        selectedIp = "192.168.1.100",
                        qrBitmap = bitmap,
                        generatedPassphrase = "sample_passphrase"
                    ),
                    onIpSelected = {},
                    onCopyText = { _, _ -> },
                    onNavigateToQrCode = {},
                    onBack = {},
                    formatBytes = { "$it B" }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/download_screen_idle_dark.png")
    }
}
