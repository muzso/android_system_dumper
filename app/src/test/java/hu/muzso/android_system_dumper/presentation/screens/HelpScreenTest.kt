package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import hu.muzso.android_system_dumper.presentation.SettingsViewModel
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HelpScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun helpScreen_showsCorrectSections() {
        val exclusionList = listOf("exclude1", "exclude2")
        val discoveryRoots = listOf("seed1", "seed2")

        composeTestRule.setContent {
            HelpContent(
                exclusionList = exclusionList,
                discoveryRoots = discoveryRoots,
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText("Exclusion list").assertExists()
        composeTestRule.onNodeWithText("exclude1", substring = true).assertExists()
        composeTestRule.onNodeWithText("exclude2", substring = true).assertExists()
        
        composeTestRule.onNodeWithText("Discovery roots").assertExists()
        composeTestRule.onNodeWithText("seed1", substring = true).assertExists()
        composeTestRule.onNodeWithText("seed2", substring = true).assertExists()
    }

    @Test
    fun helpScreen_integrationTest() {
        val viewModel = mockk<SettingsViewModel>(relaxed = true)
        val state = SettingsUiState(
            exclusionList = listOf("integrated-exclude"),
            discoveryRoots = listOf("integrated-seed")
        )
        every { viewModel.uiState } returns MutableStateFlow(state)

        composeTestRule.setContent {
            HelpScreen(
                settingsViewModel = viewModel,
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText("integrated-exclude", substring = true).assertExists()
        composeTestRule.onNodeWithText("integrated-seed", substring = true).assertExists()
    }
}
