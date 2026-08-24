package hu.muzso.android_system_dumper.presentation.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.model.IpInfo
import hu.muzso.android_system_dumper.presentation.state.IpInfoUiState
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IpInfoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ipInfoScreen_showsIpInfo() {
        val ipInfo = IpInfo(
            sourceUrl = "http://test.com",
            data = mapOf(
                "country" to "Test Country",
                "ip" to "1.2.3.4",
                "nested" to mapOf("key" to "value")
            )
        )
        val ipState = IpInfoUiState.Success(ipInfo)
        val settingsState = createSettingsState()

        composeTestRule.setContent {
            IpInfoContent(
                uiState = ipState,
                settingsUiState = settingsState,
                onSourceSelected = { },
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText("1.2.3.4").assertExists()
        composeTestRule.onNodeWithText("country:", substring = true).assertExists()
        composeTestRule.onNodeWithText("Test Country").assertExists()
        composeTestRule.onNodeWithText("key:", substring = true).assertExists()
        composeTestRule.onNodeWithText("value").assertExists()
    }

    @Test
    fun ipInfoScreen_showsError() {
        val ipState = IpInfoUiState.Error("Failed to fetch")
        val settingsState = createSettingsState()

        composeTestRule.setContent {
            IpInfoContent(
                uiState = ipState,
                settingsUiState = settingsState,
                onSourceSelected = { },
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText("Failed to fetch").assertExists()
    }

    @Test
    fun ipInfoScreen_sourceSelectionWorks() {
        var selectedSource = ""
        val ipState = IpInfoUiState.Loading
        val settingsState = createSettingsState()

        composeTestRule.setContent {
            IpInfoContent(
                uiState = ipState,
                settingsUiState = settingsState,
                onSourceSelected = { selectedSource = it },
                onBack = { }
            )
        }

        // Open dropdown
        composeTestRule.onNodeWithText("Source").performClick()
        // Select item
        composeTestRule.onNodeWithText("Source 2").performClick()
        Truth.assertThat(selectedSource).isEqualTo("Source 2")
    }

    @Test
    fun ipInfoScreen_backButtonWorks() {
        var backCalled = false
        val ipState = IpInfoUiState.Loading
        val settingsState = createSettingsState()

        composeTestRule.setContent {
            IpInfoContent(
                uiState = ipState,
                settingsUiState = settingsState,
                onSourceSelected = { },
                onBack = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        Truth.assertThat(backCalled).isTrue()
    }

    private fun createSettingsState() = SettingsUiState(
        availableIpSources = listOf("Source 1", "Source 2"),
        selectedIpSource = "Source 1"
    )
}
