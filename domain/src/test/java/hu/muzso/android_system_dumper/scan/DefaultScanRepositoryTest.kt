package hu.muzso.android_system_dumper.scan

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeMetadataCollector
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ScanUpdate
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultScanRepositoryTest {

    private lateinit var repository: DefaultScanRepository
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var dispatcherProvider: FakeDispatcherProvider

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        dispatcherProvider = FakeDispatcherProvider(testDispatcher)
        repository = DefaultScanRepository(
            fileSystem = FakeMemoryFileSystem(),
            collector = DefaultFileCollector(),
            metadataCollector = FakeMetadataCollector(),
            logger = FakeFileLogger(),
            clock = FakeClock(),
            loadExcludeListUseCase = mockk(relaxed = true),
            getSeedPathsUseCase = mockk(relaxed = true),
            getScanRootUseCase = mockk(relaxed = true),
            dispatcherProvider = dispatcherProvider
        )
    }

    @Test
    fun `initial state is empty`() {
        val result = repository.scanResult.value
        assertThat(result.readableFiles).isEmpty()
        assertThat(result.unreadableFiles).isEmpty()
        assertThat(result.excludedFiles).isEmpty()
        assertThat(result.missingFiles).isEmpty()
        assertThat(result.symlinks).isEmpty()

        val update = repository.scanUpdate.value
        assertThat(update.filesCount).isEqualTo(0)
        assertThat(update.totalBytes).isEqualTo(0L)
    }

    @Test
    fun `updateResult updates scanResult and scanUpdate`() {
        val files = listOf(
            FileEntry("/path/1", 100L, "source1"),
            FileEntry("/path/2", 200L, "source2")
        )
        val result = ScanResult(readableFiles = files)

        repository.updateResult(result)

        assertThat(repository.scanResult.value).isEqualTo(result)
        assertThat(repository.scanUpdate.value).isEqualTo(ScanUpdate(2, 300L))
    }

    @Test
    fun `updateProgress updates only scanUpdate`() {
        val update = ScanUpdate(10, 5000L)
        
        repository.updateProgress(update)

        assertThat(repository.scanUpdate.value).isEqualTo(update)
        assertThat(repository.scanResult.value.readableFiles).isEmpty()
    }

    @Test
    fun `clear resets state`() {
        val files = listOf(FileEntry("/path/1", 100L, "source"))
        repository.updateResult(ScanResult(readableFiles = files))
        
        repository.clear()

        assertThat(repository.scanResult.value.readableFiles).isEmpty()
        assertThat(repository.scanUpdate.value.filesCount).isEqualTo(0)
    }

    @Test
    fun `concurrent updates handle race conditions`() = runTest(testDispatcher) {
        val iterations = 1000
        launch {
            repeat(iterations) {
                repository.updateProgress(ScanUpdate(it, it.toLong()))
            }
        }
        launch {
            repeat(iterations) {
                repository.updateProgress(ScanUpdate(it * 2, it.toLong() * 2))
            }
        }
    }
}
