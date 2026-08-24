package hu.muzso.android_system_dumper.filesystem

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeXmlParser
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.scan.DefaultArchiveRepository
import hu.muzso.android_system_dumper.scan.DefaultFileCollector
import hu.muzso.android_system_dumper.scan.DefaultMetadataCollector
import hu.muzso.android_system_dumper.scan.DefaultScanRepository
import hu.muzso.android_system_dumper.scan.SelinuxContextAnalyzer
import hu.muzso.android_system_dumper.usecase.GetScanRootUseCase
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import hu.muzso.android_system_dumper.zip.Zip4jZipCreator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.lingala.zip4j.ZipFile
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileSystemIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val logger = FakeFileLogger()
    private val clock = FakeClock()
    private val xmlParser = FakeXmlParser()

    private lateinit var fileSystem: FakeJvmFileSystem
    private lateinit var collector: DefaultFileCollector
    private lateinit var metadataCollector: DefaultMetadataCollector
    private lateinit var scanRepository: DefaultScanRepository
    private lateinit var loadExcludeListUseCase: LoadExcludeListUseCase
    private lateinit var getSeedPathsUseCase: GetSeedPathsUseCase
    private lateinit var getScanRootUseCase: GetScanRootUseCase

    private lateinit var zipCreator: Zip4jZipCreator
    private lateinit var archiveRepository: DefaultArchiveRepository

    @Before
    fun setup() {
        fileSystem = FakeJvmFileSystem(dispatcherProvider, tempFolder.root.toPath())
        collector = DefaultFileCollector()

        loadExcludeListUseCase = mockk()
        getSeedPathsUseCase = mockk()
        getScanRootUseCase = mockk()

        every { getSeedPathsUseCase.execute() } returns emptyList()
        every { loadExcludeListUseCase.execute() } returns emptyList()
        every { getScanRootUseCase.execute() } returns "/"

        metadataCollector =
            DefaultMetadataCollector(
                fileSystem,
                xmlParser,
                logger,
                dispatcherProvider,
                SelinuxContextAnalyzer(),
                getSeedPathsUseCase
            )

        scanRepository = DefaultScanRepository(
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

        zipCreator = Zip4jZipCreator(logger, fileSystem, dispatcherProvider)
        archiveRepository = DefaultArchiveRepository(zipCreator, fileSystem)

        every { loadExcludeListUseCase.execute() } returns emptyList()
        every { getScanRootUseCase.execute() } returns "/"
    }

    @Test
    fun `integration - scan and archive realistic structure`() = runTest(testDispatcher) {
        val root = "/"
        // 1. Setup structure
        val configDir = fileSystem.addDir("config")
        val systemDir = fileSystem.addDir("system")
        val vendorDir = fileSystem.addDir("vendor")
        val cacheDir = fileSystem.addDir("cache_to_exclude")

        val file1 =
            fileSystem.addFileWithText(fileSystem.join(configDir, "settings.conf"), "config data")
        val file2 =
            fileSystem.addFileWithText(fileSystem.join(systemDir, "build.prop"), "system property")
        val file3 = fileSystem.addFileWithText(
            fileSystem.join(vendorDir, "notice.xml"),
            "<notice><file-name>/etc/extra.txt</file-name></notice>"
        )
        fileSystem.addFileWithText(fileSystem.join(cacheDir, "temp.log"), "junk")

        // Mocking seed paths to start from temp root
        every { getSeedPathsUseCase.execute() } returns listOf(root)
        // Mocking exclude list to include cache
        every { loadExcludeListUseCase.execute() } returns listOf(cacheDir)
        // Mocking scan root to temp root
        every { getScanRootUseCase.execute() } returns root

        // Setup XML parser to find an extra path
        val extraFilePath = fileSystem.addFileWithText("etc/extra.txt", "extra info")
        xmlParser.setEntries(listOf(extraFilePath))

        // 2. Run Scan
        scanRepository.scan(ignoreExcludeList = false).test {
            assertThat(awaitItem()).isEqualTo(ScanStatus.RUNNING)
            assertThat(awaitItem()).isEqualTo(ScanStatus.FINISHED)
            awaitComplete()
        }

        val result = scanRepository.scanResult.value
        val readablePaths = result.readableFiles.map { it.path }
        assertThat(readablePaths).contains(file1)
        assertThat(readablePaths).contains(file2)
        assertThat(readablePaths).contains(file3)
        assertThat(readablePaths).contains(extraFilePath)

        // Verify sources
        val file1Entry = result.readableFiles.find { it.path == file1 }
        assertThat(file1Entry?.source).isEqualTo("filesystem scan of /")

        val extraEntry = result.readableFiles.find { it.path == extraFilePath }
        // found via walk-up to / from / (seed), then down to etc/extra.txt
        assertThat(extraEntry?.source).isEqualTo("filesystem scan of /")


        assertThat(result.excludedFiles).contains(cacheDir)
        assertThat(readablePaths.any { it.contains("temp.log") }).isFalse()

        // 3. Create Archive
        val outputFile = fileSystem.addFileOfSize("out/dump.zip")
        // Mapping to relative paths for ZIP
        val zipEntries = result.readableFiles.map {
            ZipFileEntry(it.path, it.path.removePrefix(root))
        }
        val domainResult = archiveRepository.createArchive(
            zipEntries,
            ZipOptions(outputFile, ZipEncryption.NONE),
            false
        )

        assertThat(domainResult).isInstanceOf(DomainResult.Success::class.java)
        assertThat(fileSystem.exists(outputFile)).isTrue()

        // 4. Verify Archive
        val zipFile = ZipFile(File(tempFolder.root, outputFile.removePrefix("/")))
        val entryNames = zipFile.fileHeaders.map { it.fileName }
        assertThat(entryNames).containsAtLeast(
            "config/settings.conf",
            "system/build.prop",
            "vendor/notice.xml",
            "etc/extra.txt"
        )

        // Verify Logs
        logger.assertLogExists("I", "ScanRepository", "Scanning started:")
        logger.assertLogExists("I", "ScanRepository", "Scanning finished.")
        logger.assertLogExists("I", "ScanRepository", "Scan summary:")
    }

    @Test
    fun `integration - handle symlinks`() = runTest(testDispatcher) {
        val root = "/"
        val realDir = fileSystem.addDir("real_dir")
        val realFile = fileSystem.addFileWithText(fileSystem.join(realDir, "data.txt"), "real data")

        val linkDir = try {
            fileSystem.addSymlink("link_dir", realDir)
        } catch (_: Exception) {
            // Symlinks might not be supported on all file systems (e.g. some Windows setups)
            // If it fails, we skip this part of the test to avoid false negatives in restricted environments
            return@runTest
        }

        every { getSeedPathsUseCase.execute() } returns listOf(root)
        every { loadExcludeListUseCase.execute() } returns emptyList()
        every { getScanRootUseCase.execute() } returns root

        scanRepository.scan(ignoreExcludeList = false).test {
            awaitItem() // RUNNING
            awaitItem() // FINISHED
            awaitComplete()
        }

        val result = scanRepository.scanResult.value
        // Should have the canonical path of the file
        assertThat(result.readableFiles.map { it.path }).contains(realFile)
        // Should have recorded the symlink
        assertThat(result.symlinks[linkDir]).isEqualTo(realDir)
    }

    @Test
    fun `integration - metadata discovered directory source propagation`() =
        runTest(testDispatcher) {
            // Seed path is /system
            val root = "/system"

            // Discovered dir is /data/discovered_dir (outside /system)
            val discoveredDir = "/data/discovered_dir"
            val discoveredDirInFs = fileSystem.addDir(discoveredDir)
            val fileInside = fileSystem.addFileWithText(
                fileSystem.join(discoveredDirInFs, "data.txt"),
                "some data"
            )

            val noticePath = "/system/etc/notice.xml"
            fileSystem.addFileWithText(
                noticePath,
                "<notice><file-name>${discoveredDir}</file-name></notice>"
            )

            every { getSeedPathsUseCase.execute() } returns listOf(root)
            every { loadExcludeListUseCase.execute() } returns emptyList()
            every { getScanRootUseCase.execute() } returns "/"

            // Mock XML parser to "discover" the directory
            xmlParser.setEntries(listOf(discoveredDir))

            scanRepository.scan(ignoreExcludeList = false).test {
                awaitItem() // RUNNING
                awaitItem() // FINISHED
                awaitComplete()
            }

            val result = scanRepository.scanResult.value
            val fileEntry = result.readableFiles.find { it.path == fileInside }

            assertThat(fileEntry).isNotNull()
            // Should inherit the RC analysis source from its parent discovery
            assertThat(fileEntry?.source).isEqualTo("notice.xml analysis of $noticePath")
        }
}