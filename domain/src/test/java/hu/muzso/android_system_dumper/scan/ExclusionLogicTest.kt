package hu.muzso.android_system_dumper.scan

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeMetadataCollector
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.usecase.GetScanRootUseCase
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExclusionLogicTest {

    private val fileSystem = FakeMemoryFileSystem()
    private val collector = DefaultFileCollector()
    private val metadataCollector = FakeMetadataCollector()
    private val logger = FakeFileLogger()
    private val clock = mockk<Clock>(relaxed = true)
    private val loadExcludeListUseCase = mockk<LoadExcludeListUseCase>()
    private val getSeedPathsUseCase = mockk<GetSeedPathsUseCase>()
    private val getScanRootUseCase = mockk<GetScanRootUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)

    private lateinit var repository: DefaultScanRepository

    @Before
    fun setup() {
        every { getScanRootUseCase.execute() } returns "/"
        every { getSeedPathsUseCase.execute() } returns listOf("/")
        every { clock.now() } returns Instant.ofEpochMilli(1000)
    }

    private fun createRepository() {
        repository = DefaultScanRepository(
            fileSystem,
            collector,
            metadataCollector,
            logger,
            clock,
            loadExcludeListUseCase,
            getSeedPathsUseCase,
            getScanRootUseCase,
            dispatcherProvider,
        )
    }

    @Test
    fun `when ignoreExcludeList is false, directory matching prefix is excluded`() = runTest(testDispatcher) {
        every { loadExcludeListUseCase.execute() } returns listOf("/proc")
        fileSystem.addDir("/proc")
        fileSystem.addFileWithText("/proc/version", "test")
        
        createRepository()
        
        repository.scan(ignoreExcludeList = false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }
        
        val result = repository.scanResult.value
        assertThat(result.excludedFiles).contains("/proc")
        assertThat(result.readableFiles.map { it.path }).doesNotContain("/proc/version")
    }

    @Test
    fun `when ignoreExcludeList is true, everything is scanned`() = runTest(testDispatcher) {
        every { loadExcludeListUseCase.execute() } returns listOf("/mnt")
        fileSystem.addDir("/mnt")
        fileSystem.addFileWithText("/mnt/probe.txt", "test")
        
        createRepository()
        
        repository.scan(ignoreExcludeList = true).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }
        
        val result = repository.scanResult.value
        assertThat(result.excludedFiles).isEmpty()
        assertThat(result.readableFiles.map { it.path }).contains("/mnt/probe.txt")
    }

    @Test
    fun `symlinks under proc are followed`() = runTest(testDispatcher) {
        every { loadExcludeListUseCase.execute() } returns listOf("/mnt")
        fileSystem.addDir("/proc")
        fileSystem.addFileWithText("/dir/something.txt", "test")
        fileSystem.addSymlink("/proc/342/link", "/dir/something.txt")

        createRepository()

        repository.scan(ignoreExcludeList = false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        val result = repository.scanResult.value
        assertThat(result.excludedFiles).isEmpty()
        assertThat(result.symlinks.any { it.key == "/proc/342/link" && it.value == "/dir/something.txt" }).isTrue()
        assertThat(result.readableFiles.map { it.path }).contains("/dir/something.txt")
    }
}
