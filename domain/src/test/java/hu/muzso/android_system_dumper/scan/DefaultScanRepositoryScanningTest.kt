package hu.muzso.android_system_dumper.scan

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeMetadataCollector
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
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
class DefaultScanRepositoryScanningTest {

    private val fileSystem = FakeMemoryFileSystem()
    private val collector = mockk<FileCollector>(relaxed = true)
    private val metadataCollector = mockk<MetadataCollector>(relaxed = true)
    private var logger = FakeFileLogger()
    private val clock = mockk<Clock>(relaxed = true)
    private val loadExcludeListUseCase = mockk<LoadExcludeListUseCase>()
    private val getSeedPathsUseCase = mockk<GetSeedPathsUseCase>()
    private val getScanRootUseCase = mockk<GetScanRootUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)

    private lateinit var repository: DefaultScanRepository

    @Before
    fun setup() {
        logger = FakeFileLogger()
        repository = DefaultScanRepository(
            fileSystem,
            collector,
            metadataCollector,
            logger,
            clock,
            loadExcludeListUseCase,
            getSeedPathsUseCase,
            getScanRootUseCase,
            dispatcherProvider
        )
        every { loadExcludeListUseCase.execute() } returns emptyList()
        every { getSeedPathsUseCase.execute() } returns listOf("/")
        every { getScanRootUseCase.execute() } returns "/"
        every { clock.now() } returns Instant.ofEpochMilli(1000)
    }

    @Test
    fun `scan logs start and finish`() = runTest(testDispatcher) {
        every { collector.getCollectedResult() } returns ScanResult()

        repository.scan(ignoreExcludeList = false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        logger.assertLogExists("I", "ScanRepository", "Scanning started:")
        logger.assertLogExists("I", "ScanRepository", "Scanning finished.")
        logger.assertLogExists("I", "ScanRepository", "Scan summary:")
    }

    @Test
    fun `scan throttles progress updates to 100ms`() = runTest(testDispatcher) {
        fileSystem.addFileOfSize("/f1.txt", 100)
        fileSystem.addFileOfSize("/f2.txt", 100)
        
        val r2 = ScanResult(readableFiles = listOf(
            FileEntry("/f1.txt", 100, "filesystem scan of /"),
            FileEntry("/f2.txt", 100, "filesystem scan of /")
        ))
        
        // 1. Same timestamp -> only final report
        every { collector.getCollectedResult() } returns r2
        every { clock.now() } returns Instant.ofEpochMilli(1000)

        repository.scan(false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }
        
        // Verify final state is correct
        assertThat(repository.scanUpdate.value.filesCount).isEqualTo(2)

        // 2. Advancing time -> should publish progress
        val r1 = ScanResult(readableFiles = listOf(FileEntry("/f1.txt", 100, "filesystem scan of /")))
        every { collector.getCollectedResult() } returnsMany listOf(r1, r2, r2)
        every { clock.now() } returnsMany listOf(
            Instant.ofEpochMilli(1000), // Start
            Instant.ofEpochMilli(1101), // After f1 -> publishes r1
            Instant.ofEpochMilli(1202), // After f2 -> publishes r2
            Instant.ofEpochMilli(1202)  // Final report
        )
        
        repository.scan(false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        assertThat(repository.scanUpdate.value.filesCount).isEqualTo(2)
    }

    @Test
    fun `scan handles symlink loops via visitedCanonicalDirs`() = runTest(testDispatcher) {
        fileSystem.addDir("/loop")
        fileSystem.addSymlink("/loop/to_parent", "/") // Points back to root
        
        every { collector.getCollectedResult() } returns ScanResult()

        repository.scan(false).test {
            awaitItem() // RUNNING
            awaitItem() // FINISHED
            awaitComplete()
        }
        
        // If it didn't crash or loop infinitely, it worked.
        logger.assertLogExists("I", "ScanRepository", "Scanning finished.")
    }

    @Test
    fun `scan follows symlink chains`() = runTest(testDispatcher) {
        fileSystem.addDir("/proc/3453")
        fileSystem.addSymlink("/proc/3453/exe", "/usr/bin/blabla")
        fileSystem.addSymlink("/usr/bin/blabla", "/usr/share/bin/blabla-4.0")
        fileSystem.addFileWithText("/usr/share/bin/blabla-4.0", "something")

        val repo = DefaultScanRepository(
            fileSystem,
            DefaultFileCollector(),
            FakeMetadataCollector(),
            logger,
            clock,
            loadExcludeListUseCase,
            getSeedPathsUseCase,
            getScanRootUseCase,
            dispatcherProvider
        )

        repo.scan(false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        val result = repo.scanResult.value

        assertThat(result.readableFiles.map { it.path }).contains("/usr/share/bin/blabla-4.0")
    }
}
