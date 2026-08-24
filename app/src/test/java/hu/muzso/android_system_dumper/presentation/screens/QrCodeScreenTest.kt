package hu.muzso.android_system_dumper.presentation.screens

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.presentation.UploadViewModel
import hu.muzso.android_system_dumper.presentation.state.UploadUiState
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
class QrCodeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun qrCodeScreen_integrationTest() {
        val viewModel = mockk<UploadViewModel>(relaxed = true)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val state = UploadUiState(qrBitmap = bitmap)
        every { viewModel.uiState } returns MutableStateFlow(state)

        composeTestRule.setContent {
            QrCodeScreen(
                text = "test-url",
                uploadViewModel = viewModel,
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText("test-url").assertExists()
        composeTestRule.onNodeWithContentDescription("QR Code").assertExists()
    }

    @Test
    fun qrCodeScreen_showsCorrectUrl() {
        val url = "http://test.com"
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        composeTestRule.setContent {
            QrCodeContent(
                text = url,
                qrBitmap = bitmap,
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText(url).assertExists()
    }

    @Test
    fun qrCodeScreen_backButtonWorks() {
        var backCalled = false
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        composeTestRule.setContent {
            QrCodeContent(
                text = "url",
                qrBitmap = bitmap,
                onBack = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        Truth.assertThat(backCalled).isTrue()
    }

    @Test
    fun qrCodeScreen_showsErrorWhenNoBitmap() {
        composeTestRule.setContent {
            QrCodeContent(
                text = "url",
                qrBitmap = null,
                onBack = { }
            )
        }

        composeTestRule.onNodeWithText("Failed to generate QR Code").assertExists()
    }
}
