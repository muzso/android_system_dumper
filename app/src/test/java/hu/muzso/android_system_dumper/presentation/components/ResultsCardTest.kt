package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResultsCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun resultsCard_showsCorrectInfo() {
        val state = UploadUiState(
            downloadUrl = "http://test.com",
            generatedPassphrase = "test-passphrase"
        )

        composeTestRule.setContent {
            ResultsCard(
                uploadUiState = state,
                shouldUseTor = false,
                onCopyText = { _, _ -> },
                onNavigateToQrCode = { },
                onOpenUri = { }
            )
        }

        composeTestRule.onNodeWithText("http://test.com").assertExists()
        composeTestRule.onNodeWithText("test-passphrase").assertExists()
    }

    @Test
    fun resultsCard_clicksWork() {
        var urlOpened = ""
        var textCopied = ""
        var qrNavigatedWith = ""

        val state = UploadUiState(downloadUrl = "http://test.com")

        composeTestRule.setContent {
            ResultsCard(
                uploadUiState = state,
                shouldUseTor = false,
                onCopyText = { _, url -> textCopied = url },
                onNavigateToQrCode = { url -> qrNavigatedWith = url },
                onOpenUri = { url -> urlOpened = url }
            )
        }

        composeTestRule.onNodeWithText("http://test.com").performClick()
        Truth.assertThat(urlOpened).isEqualTo("http://test.com")

        composeTestRule.onNodeWithContentDescription("Copy to clipboard").performClick()
        Truth.assertThat(textCopied).isEqualTo("http://test.com")

        composeTestRule.onNodeWithContentDescription("View as QR code").performClick()
        Truth.assertThat(qrNavigatedWith).isEqualTo("http://test.com")
    }
}
