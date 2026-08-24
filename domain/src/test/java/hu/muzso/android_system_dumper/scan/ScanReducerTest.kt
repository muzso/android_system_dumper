package hu.muzso.android_system_dumper.scan

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.model.ScanError
import hu.muzso.android_system_dumper.model.ScanState
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ScanningResult
import hu.muzso.android_system_dumper.model.reduce
import org.junit.Test

class ScanReducerTest {

    private val idleState = ScanState(scanStatus = ScanStatus.IDLE, isScanning = false)
    private val scanningState = ScanState(
        scanStatus = ScanStatus.RUNNING,
        isScanning = true,
        filesCount = 10,
        totalBytes = 100L
    )
    private val finishedState = ScanState(
        scanStatus = ScanStatus.FINISHED,
        isScanning = false,
        filesCount = 100,
        totalBytes = 1000L
    )
    private val errorState =
        ScanState(scanStatus = ScanStatus.ERROR(ScanError.Unknown("fail")), isScanning = false)

    private val allBaseStates = listOf(idleState, scanningState, finishedState, errorState)

    @Test
    fun `ScanStarted sets isScanning to true regardless of current state`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, ScanningResult.ScanStarted)
            assertThat(newState.isScanning).isTrue()
            // Status remains unchanged by this specific event in the current implementation
            assertThat(newState.scanStatus).isEqualTo(state.scanStatus)
        }
    }

    @Test
    fun `ScanStopped sets isScanning to false regardless of current state`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, ScanningResult.ScanStopped)
            assertThat(newState.isScanning).isFalse()
            assertThat(newState.scanStatus).isEqualTo(state.scanStatus)
        }
    }

    @Test
    fun `StatusChanged updates status and isScanning correctly`() {
        val statuses = listOf(
            ScanStatus.IDLE,
            ScanStatus.RUNNING,
            ScanStatus.FINISHED,
            ScanStatus.ABORTED,
            ScanStatus.ERROR(ScanError.Unknown("err"))
        )

        allBaseStates.forEach { state ->
            statuses.forEach { newStatus ->
                val newState = reduce(state, ScanningResult.StatusChanged(newStatus))
                assertThat(newState.scanStatus).isEqualTo(newStatus)
                assertThat(newState.isScanning).isEqualTo(newStatus == ScanStatus.RUNNING)
            }
        }
    }

    @Test
    fun `ProgressUpdated updates counts and preserves status and isScanning`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, ScanningResult.ProgressUpdated(50, 5000L))
            assertThat(newState.filesCount).isEqualTo(50)
            assertThat(newState.totalBytes).isEqualTo(5000L)
            assertThat(newState.scanStatus).isEqualTo(state.scanStatus)
            assertThat(newState.isScanning).isEqualTo(state.isScanning)
        }
    }

    @Test
    fun `Reset always returns initial state`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, ScanningResult.Reset)
            assertThat(newState).isEqualTo(ScanState())
        }
    }
}