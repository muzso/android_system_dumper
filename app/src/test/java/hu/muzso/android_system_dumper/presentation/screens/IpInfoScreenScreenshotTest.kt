package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import hu.muzso.android_system_dumper.model.IpInfo
import hu.muzso.android_system_dumper.presentation.RoborazziTestConfigRule
import hu.muzso.android_system_dumper.presentation.ScreenshotTestTheme
import hu.muzso.android_system_dumper.presentation.state.IpInfoUiState
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class IpInfoScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziTestConfigRule()

    private val sampleSuccessState = IpInfoUiState.Success(
        IpInfo(
            sourceUrl = "https://example.com/api/ip",
            data = mapOf(
                "ip" to "1.2.3.4",
                "city" to "Budapest",
                "country" to "Hungary",
                "loc" to "47.4979,19.0402",
                "org" to "AS1234 Example ISP",
                "postal" to "1011",
                "timezone" to "Europe/Budapest",
                "asn" to mapOf(
                    "asn" to "AS1234",
                    "name" to "Example ISP",
                    "domain" to "example.com",
                    "route" to "1.2.3.0/24",
                    "type" to "isp"
                )
            )
        )
    )

    private val sampleSettingsState = SettingsUiState(
        selectedIpSource = "https://example.com/api/ip",
        availableIpSources = listOf("https://example.com/api/ip", "https://api.ipify.org")
    )

    @Test
    fun ipInfoScreen_success() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                IpInfoContent(
                    uiState = sampleSuccessState,
                    settingsUiState = sampleSettingsState,
                    onSourceSelected = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ip_info_screen_success.png")
    }

    @Test
    fun ipInfoScreen_success_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                IpInfoContent(
                    uiState = sampleSuccessState,
                    settingsUiState = sampleSettingsState,
                    onSourceSelected = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ip_info_screen_success_dark.png")
    }

    @Test
    fun ipInfoScreen_loading() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                IpInfoContent(
                    uiState = IpInfoUiState.Loading,
                    settingsUiState = sampleSettingsState,
                    onSourceSelected = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ip_info_screen_loading.png")
    }

    @Test
    fun ipInfoScreen_loading_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                IpInfoContent(
                    uiState = IpInfoUiState.Loading,
                    settingsUiState = sampleSettingsState,
                    onSourceSelected = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ip_info_screen_loading_dark.png")
    }

    @Test
    fun ipInfoScreen_error() {
        composeTestRule.setContent {
            ScreenshotTestTheme {
                IpInfoContent(
                    uiState = IpInfoUiState.Error("Failed to fetch IP information. Please check your internet connection."),
                    settingsUiState = sampleSettingsState,
                    onSourceSelected = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ip_info_screen_error.png")
    }

    @Test
    fun ipInfoScreen_error_dark() {
        composeTestRule.setContent {
            ScreenshotTestTheme(darkTheme = true) {
                IpInfoContent(
                    uiState = IpInfoUiState.Error("Failed to fetch IP information. Please check your internet connection."),
                    settingsUiState = sampleSettingsState,
                    onSourceSelected = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ip_info_screen_error_dark.png")
    }
}
