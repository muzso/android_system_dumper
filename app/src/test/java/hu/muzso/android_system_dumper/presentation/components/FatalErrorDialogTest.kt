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
class FatalErrorDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fatalErrorDialog_showsMessage() {
        val errorMessage = "Critical failure"
        composeTestRule.setContent {
            FatalErrorDialog(
                error = errorMessage,
                onReset = { }
            )
        }

        composeTestRule.onNodeWithText("Fatal Error").assertExists()
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun fatalErrorDialog_resetButtonWorks() {
        var resetCalled = false
        composeTestRule.setContent {
            FatalErrorDialog(
                error = "Error",
                onReset = { resetCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Reset").performClick()
        Truth.assertThat(resetCalled).isTrue()
    }
}
