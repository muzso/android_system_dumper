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
    fun `scan honors fileCountLimit in Phase 1`() = runTest(testDispatcher) {
        val limit = 1
        val filesCount = limit + 5
        for (i in 1..filesCount) {
            fileSystem.addDir("/d$i")
            fileSystem.addFileOfSize("/d$i/f$i.txt", 100)
        }

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

        // Seed with /data
        every { getSeedPathsUseCase.execute() } returns listOf("/")

        repo.scan(ignoreExcludeList = false, fileCountLimit = limit).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        val result = repo.scanResult.value

        // Scanner checks whether limit is reached after a directory's entries were processed.
        // In this test each directory has just one file, so directory order is irrelevant.
        // Limit is 1, /d1/f1.txt is found, readableFilesCountInPhase1 becomes 1.
        assertThat(result.readableFiles.size).isEqualTo(limit)
        logger.assertLogExists("D", "ScanRepository", "Phase 1: Loop file count limit ($limit) reached ($limit), stopping this loop.")
    }

    @Test
    fun `scan honors fileCountLimit in Phase 1 approximately`() = runTest(testDispatcher) {
        val filesCountPerDir = 5
        val limit = filesCountPerDir * 2 + 2
        for (k in 1..10) {
            fileSystem.addDir("/d$k")
            for (i in 1..filesCountPerDir) {
                fileSystem.addFileOfSize("/d$k/d${k}_f$i.txt", 100)
            }
        }

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

        // Seed with /data
        every { getSeedPathsUseCase.execute() } returns listOf("/")

        repo.scan(ignoreExcludeList = false, fileCountLimit = limit).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        val result = repo.scanResult.value
        val modulus = limit % filesCountPerDir
        val expectedReadableFiles = if (modulus == 0) limit else limit + filesCountPerDir - modulus

        // Scanner checks whether limit is reached after a directory's entries were processed.
        // In this test each directory has the same number of files, so directory order is irrelevant.
        // If limit is exactly the multiple of directory size, then limit will be perfectly honored.
        // If limit is other than exactly the multiple of directory size, then number of found (readable)
        // files will be the value that is rounded up to the next multiple of directory size.
        assertThat(result.readableFiles.size).isEqualTo(expectedReadableFiles)
        logger.assertLogExists("D", "ScanRepository", "Phase 1: Loop file count limit ($limit) reached ($expectedReadableFiles), stopping this loop.")
    }

    @Test
    fun `scan resets fileCountLimit counter when Phase 1 restarts`() = runTest(testDispatcher) {
        val metaCollector = FakeMetadataCollector()
        val repo = DefaultScanRepository(
            fileSystem,
            DefaultFileCollector(),
            metaCollector,
            logger,
            clock,
            loadExcludeListUseCase,
            getSeedPathsUseCase,
            getScanRootUseCase,
            dispatcherProvider
        )

        fileSystem.addDir("/data3")
        fileSystem.addFileWithText("/data3/00_meta.txt", "/data4")
        fileSystem.addFileOfSize("/data3/01_f1.txt", 100)
        fileSystem.addDir("/data3/data31")
        fileSystem.addFileOfSize("/data3/data31/02_f2.txt", 100)

        fileSystem.addDir("/data4")
        fileSystem.addFileOfSize("/data4/03_f3.txt", 100)
        fileSystem.addFileOfSize("/data4/04_f4.txt", 100)
        fileSystem.addDir("/data4/data41")
        fileSystem.addFileOfSize("/data4/data41/05_f5.txt", 100)

        metaCollector.setTrigger("/data3/00_meta.txt", listOf("/data4"))
        every { getSeedPathsUseCase.execute() } returns listOf("/data3")

        repo.scan(ignoreExcludeList = false, fileCountLimit = 1).test {
            awaitItem() // RUNNING
            awaitItem() // FINISHED
            awaitComplete()
        }

        val result = repo.scanResult.value

        // Run#1 finds all direct children of /data3, meaning: 00_meta.txt, 01_f1.txt (count=2)
        // Limit is reached, phase#1 loop breaks.
        // Phase#2 runs, MetadataCollector returns "/data4" by processing "00_meta.txt".
        // Run#2 starts after Phase 2.
        // Run#2 traverses /data4. Finds direct children of /data4: 03_f3.txt, 04_f4.txt (count=2).
        // Limit reached, phase#1 loop breaks.
        // readableFiles should be 4 files: 00_meta.txt, 01_f1.txt, 03_f3.txt, 04_f4.txt.
        assertThat(result.readableFiles.size).isEqualTo(4)
        
        // Should have logged twice.
        val logs = logger.events.filter { it.level == "D" && it.message?.contains("Phase 1: Loop file count limit") == true }
        assertThat(logs.size).isEqualTo(2)
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
