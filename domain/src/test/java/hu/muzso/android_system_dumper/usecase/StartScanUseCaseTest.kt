package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeScanRepository
import hu.muzso.android_system_dumper.model.ScanStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class StartScanUseCaseTest {

    private val repository = FakeScanRepository()
    private lateinit var startScanUseCase: StartScanUseCase

    @Before
    fun setup() {
        startScanUseCase = StartScanUseCase(repository)
    }

    @Test
    fun `execute calls repository scan`() = runTest {
        val expectedStatuses = listOf(ScanStatus.RUNNING, ScanStatus.FINISHED)
        repository.setStatuses(expectedStatuses)

        val result = startScanUseCase.execute(ignoreExcludeList = false).toList()

        assertThat(result).isEqualTo(expectedStatuses)
        assertThat(repository.lastIgnoreExcludeList).isFalse()
    }
}
