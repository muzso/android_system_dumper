package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import hu.muzso.android_system_dumper.presentation.widgets.SettingsDropdownSelector
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDropdownSelectorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows selected item and expands on click`() {
        var selected: String? = null
        composeTestRule.setContent {
            AndroidSystemDumperTheme {
                SettingsDropdownSelector(
                    label = "Service",
                    items = listOf("Filebin", "Gofile"),
                    selectedItem = "Filebin",
                    onItemSelected = { selected = it },
                    itemLabel = { it }
                )
            }
        }

        // Selected value should be visible
        composeTestRule.onNodeWithText("Filebin").assertIsDisplayed()
        // Click to expand
        composeTestRule.onNodeWithText("Filebin").performClick()
        
        // Option should now be visible and clickable
        composeTestRule.onNodeWithText("Gofile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gofile").performClick()
        
        assert(selected == "Gofile")
    }
}
