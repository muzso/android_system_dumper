package hu.muzso.android_system_dumper.domain.repository.scan

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.scan.ScanRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

abstract class ScanRepositoryContract {

    abstract fun createScanRepository(seedPaths: List<String>, excludeList: List<String>): ScanRepository

    @Test
    fun scan_emits_RUNNING_and_FINISHED() = runTest {
        val repository = createScanRepository(listOf("/"), emptyList())
        val statuses = repository.scan(ignoreExcludeList = false).toList()

        assertThat(statuses).containsAtLeast(ScanStatus.RUNNING, ScanStatus.FINISHED)
    }

    @Test
    fun scan_can_be_cancelled() = runTest {
        val repository = createScanRepository(listOf("/"), emptyList())
        val statuses = mutableListOf<ScanStatus>()
        
        val job = launch {
            repository.scan(ignoreExcludeList = false).collect { 
                statuses.add(it)
                if (it == ScanStatus.RUNNING) {
                    cancel()
                }
            }
        }
        job.join()

        assertThat(statuses).contains(ScanStatus.RUNNING)
    }
}
