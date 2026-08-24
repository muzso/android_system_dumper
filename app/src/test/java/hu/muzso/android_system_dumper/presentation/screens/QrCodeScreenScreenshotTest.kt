package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import hu.muzso.android_system_dumper.presentation.RoborazziTestConfigRule
import hu.muzso.android_system_dumper.presentation.ScreenshotTestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class QrCodeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziTestConfigRule()

    @Test
    fun qrCodeScreen_content() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                QrCodeContent(
                    text = "https://muzso.hu",
                    qrBitmap = null, // Can't easily mock Bitmap in unit tests without extra setup, testing structure
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/qr_code_screen.png")
    }

    @Test
    fun qrCodeScreen_content_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                QrCodeContent(
                    text = "https://muzso.hu",
                    qrBitmap = null,
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/qr_code_screen_dark.png")
    }
}
