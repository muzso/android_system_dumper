package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ErrorDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorDialog_showsMessage() {
        val errorMessage = "Critical failure"
        composeTestRule.setContent {
            ErrorDialog(
                error = errorMessage,
                onReset = { }
            )
        }

        composeTestRule.onNodeWithText("Error").assertExists()
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun errorDialog_closeButtonWorks() {
        var resetCalled = false
        composeTestRule.setContent {
            ErrorDialog(
                error = "Error",
                onReset = { resetCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Close").performClick()
        Truth.assertThat(resetCalled).isTrue()
    }
}
