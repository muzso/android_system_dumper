package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import hu.muzso.android_system_dumper.presentation.widgets.ScanButton
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows start when not scanning`() {
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                ScanButton(isScanning = false, onClick = {})
            }
        }
        // Using ignoreCase = true as strings might be localized or capitalized differently
        composeTestRule.onNodeWithText("START", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `shows stop when scanning`() {
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                ScanButton(isScanning = true, onClick = {})
            }
        }
        composeTestRule.onNodeWithText("STOP", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `click triggers callback`() {
        var clicked = false
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                ScanButton(isScanning = false, onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithTag("scan_button").performClick()
        assert(clicked)
    }
}
