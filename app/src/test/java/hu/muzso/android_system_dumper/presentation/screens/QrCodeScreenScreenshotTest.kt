package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import hu.muzso.android_system_dumper.platform.DefaultQrGenerator
import hu.muzso.android_system_dumper.presentation.RoborazziTestConfigRule
import hu.muzso.android_system_dumper.presentation.ScreenshotTestTheme
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
class QrCodeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziTestConfigRule()

    private val qrGenerator = DefaultQrGenerator(mockk(relaxed = true))
    private val url = "https://example.com/"

    @Test
    fun qrCodeScreen_content() {
        val bitmap = qrGenerator.generateQrCode(url, 512)
        composeTestRule.setContent {
            ScreenshotTestTheme {
                QrCodeContent(
                    text = url,
                    qrBitmap = bitmap,
                ) { }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/qr_code_screen.png")
    }

    @Test
    fun qrCodeScreen_content_dark() {
        val bitmap = qrGenerator.generateQrCode(url, 512)
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                QrCodeContent(
                    text = url,
                    qrBitmap = bitmap,
                ) { }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/qr_code_screen_dark.png")
    }
}
