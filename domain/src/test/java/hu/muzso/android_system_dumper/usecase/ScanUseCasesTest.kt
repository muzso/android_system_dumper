package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ScanUpdate
import hu.muzso.android_system_dumper.scan.ScanRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ScanUseCasesTest {

    @Test
    fun `LoadExcludeListUseCase returns expected list`() {
        val useCase = LoadExcludeListUseCase()
        val result = useCase.execute()
        assertThat(result).contains("/data")
        assertThat(result).contains("/sys")
    }

    @Test
    fun `GetSeedPathsUseCase returns non-empty list`() {
        val useCase = GetSeedPathsUseCase()
        val result = useCase.execute()
        assertThat(result).isNotEmpty()
        assertThat(result).contains("/")
    }

    @Test
    fun `GetScanRootUseCase returns root`() {
        val useCase = GetScanRootUseCase()
        assertThat(useCase.execute()).isEqualTo("/")
    }

    @Test
    fun `StartScanUseCase calls repository`() = runTest {
        val repository = mockk<ScanRepository>()
        val useCase = StartScanUseCase(repository)
        every { repository.scan(any()) } returns flowOf(ScanStatus.RUNNING)

        val result = useCase.execute(true).first()
        assertThat(result).isEqualTo(ScanStatus.RUNNING)
        verify { repository.scan(true).run { } }
    }

    @Test
    fun `CancelScanUseCase cancels job`() = runTest {
        val useCase = CancelScanUseCase()
        val job = Job()
        useCase.execute(job)
        assertThat(job.isCancelled).isTrue()
    }

    @Test
    fun `CancelScanUseCase handles null job`() = runTest {
        val useCase = CancelScanUseCase()
        useCase.execute(null)
        // No exception expected
    }

    @Test
    fun `ClearScanResultsUseCase calls repository`() {
        val repository = mockk<ScanRepository>()
        val useCase = ClearScanResultsUseCase(repository)
        every { repository.clear() } returns Unit

        useCase.execute()
        verify { repository.clear() }
    }

    @Test
    fun `CalculateStatisticsUseCase maps scanUpdate`() = runTest {
        val repository = mockk<ScanRepository>()
        val scanUpdateFlow = MutableStateFlow(ScanUpdate(0, 0L))
        every { repository.scanUpdate } returns scanUpdateFlow

        val useCase = CalculateStatisticsUseCase(repository)
        
        scanUpdateFlow.value = ScanUpdate(filesCount = 10, totalBytes = 1024L)
        
        assertThat(useCase.filesCount.first()).isEqualTo(10)
        assertThat(useCase.totalBytes.first()).isEqualTo(1024L)
    }
}
