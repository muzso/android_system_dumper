package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.model.ScanStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ScanSystemUseCaseTest {

    private val startScanUseCase = mockk<StartScanUseCase>()
    private val cancelScanUseCase = mockk<CancelScanUseCase>()
    private val clearScanResultsUseCase = mockk<ClearScanResultsUseCase>()
    private val calculateStatisticsUseCase = mockk<CalculateStatisticsUseCase>(relaxed = true)
    private val getSeedPathsUseCase = mockk<GetSeedPathsUseCase>()

    private val useCase = ScanSystemUseCase(
        startScanUseCase,
        cancelScanUseCase,
        clearScanResultsUseCase,
        calculateStatisticsUseCase,
        getSeedPathsUseCase
    )

    @Test
    fun `getSeedPaths calls GetSeedPathsUseCase`() {
        val paths = listOf("/test")
        every { getSeedPathsUseCase.execute() } returns paths
        assertThat(useCase.getSeedPaths()).isEqualTo(paths)
        verify { getSeedPathsUseCase.execute() }
    }

    @Test
    fun `execute calls StartScanUseCase`() {
        every { startScanUseCase.execute(any()) } returns flowOf(ScanStatus.RUNNING)
        useCase.execute(true).run { }
        verify { startScanUseCase.execute(true).run { } }
    }

    @Test
    fun `cancel calls CancelScanUseCase`() = runTest {
        val job = Job()
        coEvery { cancelScanUseCase.execute(any()) } returns Unit
        useCase.cancel(job)
        coVerify { cancelScanUseCase.execute(job) }
    }

    @Test
    fun `clearResults calls ClearScanResultsUseCase`() {
        every { clearScanResultsUseCase.execute() } returns Unit
        useCase.clearResults()
        verify { clearScanResultsUseCase.execute() }
    }
}
