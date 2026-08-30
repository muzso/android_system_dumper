package hu.muzso.android_system_dumper.presentation

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.config.AppConfig
import hu.muzso.android_system_dumper.model.ScanAction
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.usecase.ScanSystemUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val scanSystemUseCase = mockk<ScanSystemUseCase>(relaxed = true)
    private val appConfig = mockk<AppConfig>(relaxed = true)

    private lateinit var viewModel: ScanViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { scanSystemUseCase.filesCount } returns MutableStateFlow(0)
        every { scanSystemUseCase.totalBytes } returns MutableStateFlow(0L)
        
        viewModel = ScanViewModel(scanSystemUseCase, appConfig)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value
        assertThat(state.scanStatus).isEqualTo(ScanStatus.IDLE)
        assertThat(state.isScanning).isFalse()
        assertThat(state.filesCount).isEqualTo(0)
        assertThat(state.totalBytes).isEqualTo(0L)
    }

    @Test
    fun `scan completion updates state`() = runTest {
        val statuses = listOf(ScanStatus.RUNNING, ScanStatus.FINISHED)
        every { scanSystemUseCase.execute(any(), any()) } returns flowOf(*statuses.toTypedArray())

        viewModel.processIntent(ScanAction.ToggleScanning(ignoreExcludeList = false))

        assertThat(viewModel.uiState.value.scanStatus).isEqualTo(ScanStatus.FINISHED)
        assertThat(viewModel.uiState.value.isScanning).isFalse()
    }

    @Test
    fun `stopScanning cancels scan and updates state`() = runTest {
        viewModel.processIntent(ScanAction.ToggleScanning(ignoreExcludeList = false))
        assertThat(viewModel.uiState.value.isScanning).isTrue()

        viewModel.processIntent(ScanAction.StopScanning)
        
        assertThat(viewModel.uiState.value.isScanning).isFalse()
        coVerify { scanSystemUseCase.cancel(any()) }
    }

    @Test
    fun `repeated toggle scanning works correctly`() = runTest {
        every { scanSystemUseCase.execute(any(), any()) } returns flowOf(ScanStatus.RUNNING, ScanStatus.FINISHED)

        // First scan
        viewModel.processIntent(ScanAction.ToggleScanning(ignoreExcludeList = false))
        assertThat(viewModel.uiState.value.scanStatus).isEqualTo(ScanStatus.FINISHED)

        // Second scan
        viewModel.processIntent(ScanAction.ToggleScanning(ignoreExcludeList = false))
        assertThat(viewModel.uiState.value.scanStatus).isEqualTo(ScanStatus.FINISHED)
        
        verify(exactly = 2) { 
            scanSystemUseCase.execute(any(), any()).run { } 
        }
    }

    @Test
    fun `resetResults updates state and clears repository`() = runTest {
        viewModel.processIntent(ScanAction.ResetResults)

        assertThat(viewModel.uiState.value.scanStatus).isEqualTo(ScanStatus.IDLE)
        assertThat(viewModel.uiState.value.isScanning).isFalse()
        verify { scanSystemUseCase.clearResults() }
    }
}
