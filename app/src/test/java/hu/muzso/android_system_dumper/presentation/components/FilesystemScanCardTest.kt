package hu.muzso.android_system_dumper.presentation.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FilesystemScanCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun filesystemScanCard_showsCorrectInfo() {
        val state = ScanState(
            scanStatus = ScanStatus.FINISHED,
            filesCount = 123,
            totalBytes = 456789
        )

        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = state,
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = { },
                onToggleScanning = { },
                formatBytes = { "$it bytes" }
            )
        }

        composeTestRule.onNodeWithText("123").assertExists()
        composeTestRule.onNodeWithText("456789 bytes").assertExists()
    }

    @Test
    fun filesystemScanCard_toggleScanningWork() {
        var toggleScanCalled = false
        val state = ScanState(scanStatus = ScanStatus.IDLE)

        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = state,
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = { },
                onToggleScanning = { toggleScanCalled = true },
                formatBytes = { "" }
            )
        }

        composeTestRule.onNodeWithText("Start").performClick()
        Truth.assertThat(toggleScanCalled).isTrue()
    }

    @Test
    fun filesystemScanCard_ignoreExcludeToggleWorks() {
        var ignoreExcludeValue = false
        val state = ScanState(scanStatus = ScanStatus.IDLE)

        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = state,
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = { ignoreExcludeValue = it },
                onToggleScanning = { },
                formatBytes = { "" }
            )
        }

        composeTestRule.onNodeWithText("Ignore exclude list").performClick()
        Truth.assertThat(ignoreExcludeValue).isTrue()
    }
}
