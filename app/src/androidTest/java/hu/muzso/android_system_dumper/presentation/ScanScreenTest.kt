package hu.muzso.android_system_dumper.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.accessibility.AccessibilityChecks
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.presentation.components.FilesystemScanCard
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class ScanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    companion object {
        @JvmStatic
        @BeforeClass
        fun setup() {
            AccessibilityChecks.enable()
        }
    }

    @Test
    fun idleState_displaysReady() {
        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = {},
                onToggleScanning = {},
                formatBytes = { "$it B" }
            )
        }

        val readyText = context.getString(R.string.ready)
        composeTestRule.onNodeWithText(readyText).assertIsDisplayed()
    }

    @Test
    fun scanningState_displaysScanning() {
        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = ScanState(scanStatus = ScanStatus.RUNNING, isScanning = true),
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = {},
                onToggleScanning = {},
                formatBytes = { "$it B" }
            )
        }

        val scanningText = context.getString(R.string.scanning)
        composeTestRule.onNodeWithText(scanningText).assertIsDisplayed()
    }

    @Test
    fun completedState_displaysFinished() {
        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = ScanState(scanStatus = ScanStatus.FINISHED),
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = {},
                onToggleScanning = {},
                formatBytes = { "$it B" }
            )
        }

        val finishedText = context.getString(R.string.finished)
        composeTestRule.onNodeWithText(finishedText).assertIsDisplayed()
    }

    @Test
    fun errorState_displaysAborted() {
        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = ScanState(scanStatus = ScanStatus.ABORTED),
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = {},
                onToggleScanning = {},
                formatBytes = { "$it B" }
            )
        }

        val abortedText = context.getString(R.string.aborted)
        composeTestRule.onNodeWithText(abortedText).assertIsDisplayed()
    }

    @Test
    fun clickingScanButton_triggersOnToggleScanning() {
        var clicked = false
        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
                ignoreExcludeList = false,
                onIgnoreExcludeListChange = {},
                onToggleScanning = { clicked = true },
                formatBytes = { "$it B" },
            )
        }

        val startText = context.getString(R.string.start)
        composeTestRule.onNodeWithText(startText).performClick()
        
        assert(clicked)
    }

    @Test
    fun togglingIgnoreExcludeSwitch_triggersCallback() {
        var checkedState = false
        composeTestRule.setContent {
            FilesystemScanCard(
                scanUiState = ScanState(scanStatus = ScanStatus.IDLE),
                ignoreExcludeList = checkedState,
                onIgnoreExcludeListChange = { checkedState = it },
                onToggleScanning = {},
                formatBytes = { "$it B" },
            )
        }

        composeTestRule.onNodeWithTag("switch_upload_ignore_exclude").performClick()
        
        assert(checkedState)
    }
}
