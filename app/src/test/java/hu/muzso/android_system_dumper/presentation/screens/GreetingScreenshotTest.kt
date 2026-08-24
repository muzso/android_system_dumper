package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
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
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val roborazziRule = RoborazziTestConfigRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
        ScreenshotTestTheme {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Android System Dumper",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun greeting_screenshot_dark() {
    composeTestRule.setContent {
        ScreenshotTestTheme(darkTheme = true) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Android System Dumper",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting_dark.png")
  }
}