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
class HelpScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziTestConfigRule()

    @Test
    fun helpScreen_content() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                HelpContent(
                    exclusionList = listOf("/bugreports/", "/cache/", "/data/"),
                    discoveryRoots = listOf("/", "/system", "/vendor"),
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/help_screen.png")
    }

    @Test
    fun helpScreen_content_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                HelpContent(
                    exclusionList = listOf("/bugreports/", "/cache/", "/data/"),
                    discoveryRoots = listOf("/", "/system", "/vendor"),
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/help_screen_dark.png")
    }
}
