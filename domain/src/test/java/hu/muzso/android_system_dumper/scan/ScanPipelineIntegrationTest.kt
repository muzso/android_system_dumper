package hu.muzso.android_system_dumper.scan

import app.cash.turbine.test
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeMetadataCollector
import hu.muzso.android_system_dumper.domain.fixtures.FakeZipCreator
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import hu.muzso.android_system_dumper.usecase.GetScanRootUseCase
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanPipelineIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val logger = FakeFileLogger()
    private val clock = FakeClock()
    private val fileSystem = FakeJvmFileSystem(dispatcherProvider)
    private val metadataCollector = FakeMetadataCollector(fileSystem)
    private val collector = DefaultFileCollector()

    private val loadExcludeListUseCase = mockk<LoadExcludeListUseCase>()
    private val getSeedPathsUseCase = mockk<GetSeedPathsUseCase>()
    private val getScanRootUseCase = mockk<GetScanRootUseCase>()
    private val platformUtils = mockk<PlatformUtils>(relaxed = true)

    private lateinit var scanRepository: DefaultScanRepository
    private lateinit var zipCreator: FakeZipCreator
    private lateinit var archiveRepository: DefaultArchiveRepository
    private lateinit var createArchiveUseCase: CreateArchiveUseCase

    @Before
    fun setup() {
        scanRepository = DefaultScanRepository(
            fileSystem = fileSystem,
            collector = collector,
            metadataCollector = metadataCollector,
            logger = logger,
            clock = clock,
            loadExcludeListUseCase = loadExcludeListUseCase,
            getSeedPathsUseCase = getSeedPathsUseCase,
            getScanRootUseCase = getScanRootUseCase,
            dispatcherProvider = dispatcherProvider
        )

        zipCreator = FakeZipCreator(fileSystem)
        archiveRepository = DefaultArchiveRepository(zipCreator, fileSystem)
        createArchiveUseCase = CreateArchiveUseCase(archiveRepository, platformUtils)

        every { getScanRootUseCase.execute() } returns "/"
    }

    @Test
    fun `complete scan and archive pipeline happy path`() = runTest(testDispatcher) {
        // 1. Setup Filesystem
        fileSystem.addDir("/") // Root

        fileSystem.addFileOfSize("/etc/notice.xml", size = 100L)
        fileSystem.addFileOfSize("/etc/version.txt", size = 20L)

        fileSystem.addFileOfSize("/system/build.prop", size = 500L)

        fileSystem.addFileOfSize("/cache/temp.log", size = 1000L)

        // 2. Setup Exclusions and Seeds
        every { getSeedPathsUseCase.execute() } returns listOf("/etc", "/system")
        every { loadExcludeListUseCase.execute() } returns listOf("/cache")

        // 3. Setup Metadata Collector
        // If notice.xml is scanned, it will "discover" /vendor/manifest.xml
        metadataCollector.setTrigger(
            fileSystem.getCanonicalPath("/etc/notice.xml"),
            listOf("/vendor/manifest.xml")
        )

        fileSystem.addFileOfSize("/vendor/manifest.xml", size = 300L)

        // 4. Execute Scan
        scanRepository.scan(ignoreExcludeList = false).test {
            Truth.assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            Truth.assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        // 5. Verify Scan Results
        val result = scanRepository.scanResult.value

        val etcNotice = result.readableFiles.find { it.path.endsWith("/etc/notice.xml") }
        val etcVersion = result.readableFiles.find { it.path.endsWith("/etc/version.txt") }
        val systemProp = result.readableFiles.find { it.path.endsWith("/system/build.prop") }
        val vendorManifest = result.readableFiles.find { it.path.endsWith("/vendor/manifest.xml") }

        Truth.assertThat(etcNotice?.source).isEqualTo("filesystem scan of /etc")
        Truth.assertThat(etcVersion?.source).isEqualTo("filesystem scan of /etc")
        Truth.assertThat(systemProp?.source).isEqualTo("filesystem scan of /system")

        // Note: the initial filesystem scan won't find /vendor/manifest.xml, because /vendor is not in the
        //       seed path list and the scan won't climb up to "/" (it's above all of the seed paths).
        //       So /vendor/manifest.xml will only be found through the metadata analysis of /etc/notice.xml.
        Truth.assertThat(vendorManifest?.source)
            .isEqualTo("fake metadata analysis of /etc/notice.xml")

        val paths = result.readableFiles.map { it.path }
        Truth.assertThat(paths).hasSize(4)

        val totalSize = result.readableFiles.sumOf { it.size }
        Truth.assertThat(totalSize).isEqualTo(100L + 20L + 500L + 300L)

        // 6. Verify Progress Updates
        val update = scanRepository.scanUpdate.value
        Truth.assertThat(update.filesCount).isEqualTo(4)
        Truth.assertThat(update.totalBytes).isEqualTo(920L)

        // 7. Verify Logs (Regression Tests)
        logger.assertLogExists("I", "ScanRepository", "Scanning started:")
        logger.assertLogExists("I", "ScanRepository", "Scanning finished.")
        logger.assertLogExists(
            "I",
            "ScanRepository",
            "Scan summary: 4 readable files, 920 bytes total"
        )

        // 8. Create Archive
        val zipFilePath = "/cache/dump.zip"
        val zipEntries =
            result.readableFiles.map { ZipFileEntry(it.path, it.path.removePrefix("/")) }

        val archiveResult = createArchiveUseCase.execute(
            files = zipEntries,
            options = ZipOptions(
                outputFilePath = zipFilePath,
                encryptionMethod = ZipEncryption.NONE,
                passphrase = null
            ),
            readIntoMemory = false
        )

        // 9. Verify Archive
        Truth.assertThat(archiveResult).isInstanceOf(DomainResult.Success::class.java)
        Truth.assertThat(zipCreator.createCalledCount).isEqualTo(1)
        Truth.assertThat(zipCreator.lastOptions!!.outputFilePath).isEqualTo("/cache/dump.zip")
        Truth.assertThat(zipCreator.lastFiles!!.map { it.zipPath }).containsExactly(
            "etc/notice.xml",
            "etc/version.txt",
            "system/build.prop",
            "vendor/manifest.xml"
        )
    }

    @Test
    fun `filesystem scan source takes precedence over metadata analysis source`() =
        runTest(testDispatcher) {
            fileSystem.addDir("/")
            fileSystem.addDir("/etc")
            fileSystem.addFileOfSize("/etc/notice.xml", 100L)
            fileSystem.addFileOfSize("/etc/duplicate.txt", 50L)

            // notice.xml points to duplicate.txt, which is ALREADY in /etc (so found by scan)
            metadataCollector.setTrigger("/etc/notice.xml", listOf("/etc/duplicate.txt"))

            every { getSeedPathsUseCase.execute() } returns listOf("/etc")
            every { loadExcludeListUseCase.execute() } returns emptyList()

            scanRepository.scan(false).test {
                awaitItem() // RUNNING
                awaitItem() // FINISHED
                awaitComplete()
            }

            val result = scanRepository.scanResult.value
            val duplicateEntry =
                result.readableFiles.find { it.path.endsWith("/etc/duplicate.txt") }

            // Filesystem source should win
            Truth.assertThat(duplicateEntry?.source).isEqualTo("filesystem scan of /etc")
        }
}