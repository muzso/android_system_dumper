package hu.muzso.android_system_dumper.filesystem

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeXmlParser
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DirEntry
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.platform.NativeBridge
import hu.muzso.android_system_dumper.scan.DefaultFileCollector
import hu.muzso.android_system_dumper.scan.DefaultMetadataCollector
import hu.muzso.android_system_dumper.scan.DefaultScanRepository
import hu.muzso.android_system_dumper.scan.SelinuxContextAnalyzer
import hu.muzso.android_system_dumper.usecase.GetScanRootUseCase
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import hu.muzso.android_system_dumper.usecase.StartScanUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class FileSystemIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fileSystem: SystemFileSystem
    private val nativeBridge = mockk<NativeBridge>()
    private lateinit var repository: DefaultScanRepository
    private val logger = mockk<FileLogger>(relaxed = true)
    private val clock = FakeClock()
    private val xmlParser = FakeXmlParser()
    private val loadExcludeListUseCase = mockk<LoadExcludeListUseCase>()
    private val getSeedPathsUseCase = mockk<GetSeedPathsUseCase>()
    private val getScanRootUseCase = mockk<GetScanRootUseCase>()

    private val dispatcherProvider = object : DispatcherProvider {
        override fun main(): CoroutineDispatcher = Dispatchers.Main
        override fun default(): CoroutineDispatcher = Dispatchers.Default
        override fun io(): CoroutineDispatcher = Dispatchers.IO
        override fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
    }

    private lateinit var startScanUseCase: StartScanUseCase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = tempFolder.root

        every { nativeBridge.listDirectory(any(), any()) } answers {
            val path = firstArg<String>()
            val dir = File(path)
            dir.listFiles()?.map { file ->
                val type = when {
                    Files.isSymbolicLink(file.toPath()) -> DirEntry.TYPE_LINK
                    file.isDirectory -> DirEntry.TYPE_DIR
                    else -> DirEntry.TYPE_FILE
                }
                DirEntry(file.name, type)
            }?.toTypedArray() ?: emptyArray()
        }

        fileSystem = SystemFileSystem(context, nativeBridge, dispatcherProvider)

        val collector = DefaultFileCollector()
        every { getSeedPathsUseCase.execute() } returns emptyList()
        val metadataCollector =
            DefaultMetadataCollector(
                fileSystem, xmlParser, logger, dispatcherProvider, SelinuxContextAnalyzer(), getSeedPathsUseCase
            )

        every { getScanRootUseCase.execute() } returns root.canonicalPath

        repository = DefaultScanRepository(
            fileSystem, collector, metadataCollector, logger,
            clock, loadExcludeListUseCase, getSeedPathsUseCase,
            getScanRootUseCase, dispatcherProvider
        )

        startScanUseCase = StartScanUseCase(repository)
    }

    @Test
    fun scan_findsFilesAndDirectories() = runTest {
        // Arrange
        val root = tempFolder.root
        File(root, "file1.txt").apply { writeText("content1") }
        val subDir = File(root, "subdir").apply { mkdir() }
        File(subDir, "file2.txt").apply { writeText("content2") }

        every { getSeedPathsUseCase.execute() } returns listOf(root.absolutePath)
        every { loadExcludeListUseCase.execute() } returns emptyList()

        // Act
        val statuses = startScanUseCase.execute(ignoreExcludeList = false).toList()

        // Assert
        assertThat(statuses).contains(ScanStatus.FINISHED)

        val results = repository.scanResult.value
        val paths = results.readableFiles.map { it.path }

        assertThat(paths).containsAtLeast(
            File(root, "file1.txt").canonicalPath,
            File(subDir, "file2.txt").canonicalPath
        )
    }

    @Test
    fun scan_respectsExclusions() = runTest {
        // Arrange
        val root = tempFolder.root
        File(root, "included.txt").apply { writeText("data") }
        val excludedDir = File(root, "excluded").apply { mkdir() }
        File(excludedDir, "secret.txt").apply { writeText("secret") }

        every { getSeedPathsUseCase.execute() } returns listOf(root.absolutePath)
        every { loadExcludeListUseCase.execute() } returns listOf(excludedDir.absolutePath)

        // Act
        startScanUseCase.execute(ignoreExcludeList = false).toList()

        // Assert
        val results = repository.scanResult.value
        val paths = results.readableFiles.map { it.path }

        assertThat(paths).contains(File(root, "included.txt").canonicalPath)
        assertThat(paths).doesNotContain(File(excludedDir, "secret.txt").canonicalPath)
        assertThat(results.excludedFiles).contains(excludedDir.absolutePath)
    }

    @Test
    fun scan_followsSymlinks() = runTest {
        // Arrange
        val root = tempFolder.root
        val targetFile = File(root, "target.txt").apply { writeText("original") }
        val linkFile = File(root, "link.txt")

        try {
            Files.createSymbolicLink(linkFile.toPath(), targetFile.toPath())
        } catch (_: Exception) {
            // Symlinks might not be supported on all filesystems/OS versions during tests
            return@runTest
        }

        every { getSeedPathsUseCase.execute() } returns listOf(root.absolutePath)
        every { loadExcludeListUseCase.execute() } returns emptyList()

        // Act
        startScanUseCase.execute(ignoreExcludeList = false).toList()

        // Assert
        val results = repository.scanResult.value
        assertThat(results.symlinks).containsKey(linkFile.absolutePath)
        assertThat(results.symlinks[linkFile.absolutePath]).isEqualTo(targetFile.canonicalPath)
    }
}
