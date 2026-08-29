package hu.muzso.android_system_dumper.network

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.DefaultPlatformUtils
import hu.muzso.android_system_dumper.common.RandomProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.scan.DefaultArchiveRepository
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import hu.muzso.android_system_dumper.zip.Zip4jZipCreator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.lingala.zip4j.ZipFile
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.Date
import kotlin.random.Random

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DoubleZippingFilenameTest {

    private val fileSystem = FakeMemoryFileSystem()
    private val logger = FakeFileLogger()
    private val clock = FakeClock()
    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val randomProvider = mockk<RandomProvider>()
    private val systemInfo = mockk<SystemInfo>()
    
    private val platformUtils = DefaultPlatformUtils(randomProvider)
    private val batchingLogic = BatchingLogic(logger)
    private val batchFilesUseCase = BatchFilesUseCase(batchingLogic)
    private val zipCreator = Zip4jZipCreator(logger, fileSystem, dispatcherProvider)
    private val archiveRepository = DefaultArchiveRepository(zipCreator, fileSystem)
    private val createArchiveUseCase = CreateArchiveUseCase(archiveRepository, platformUtils)
    private val cleanupUseCase = CleanupUseCase(fileSystem)

    private lateinit var generator: DefaultArchiveGenerator

    @Before
    fun setup() {
        generator = DefaultArchiveGenerator(
            fileSystem = fileSystem,
            clock = clock,
            logger = logger,
            systemInfo = systemInfo,
            batchFilesUseCase = batchFilesUseCase,
            createArchiveUseCase = createArchiveUseCase,
            cleanupUseCase = cleanupUseCase
        )
        clock.setNow(Instant.parse("2026-08-28T20:11:22Z"))
        every { randomProvider.getRandom() } returns Random(42)
    }

    @Test
    fun `generateBatch creates inner zip with correct name when double zipping is enabled`() = runTest(testDispatcher) {
        fileSystem.addFileOfSize("/data/file1.txt", 100)
        
        val scanResult = ScanResult(
            readableFiles = listOf(FileEntry("/data/file1.txt", 100, "source"))
        )
        
        val parameters = UploadParameters(
            customBatchSizeMb = "1",
            proxySpecification = "",
            shouldUseTor = false,
            shouldUploadZips = true,
            shouldUploadReadableList = false,
            shouldUploadUnreadableList = false,
            shouldUploadExcludedList = false,
            shouldUploadMissingList = false,
            shouldUploadSymlinkList = false,
            shouldUploadGetprop = false,
            shouldUploadAppLogs = false,
            zipEncryption = ZipEncryption.NONE,
            selectedService = mockk(),
            maxBatches = 0,
            useDoubleZipping = true
        )
        
        generator.prepare(parameters, scanResult)
        
        val result = generator.generateBatch(1)
        assertThat(result).isInstanceOf(hu.muzso.android_system_dumper.model.DomainResult.Success::class.java)
        
        val generatedZip = (result as hu.muzso.android_system_dumper.model.DomainResult.Success).data
        val dateStr = platformUtils.formatDate2Filename(Date.from(clock.now()))
        assertThat(generatedZip.filename).isEqualTo("${dateStr}_1.zip")
        
        // Verify the content of the outer ZIP
        verifyInnerZipName(generatedZip.path, "${dateStr}_1.plain.zip")
    }

    @Test
    fun `generateMisc creates inner zip with correct name when double zipping is enabled`() = runTest(testDispatcher) {
        val scanResult = ScanResult(
            readableFiles = listOf(FileEntry("/data/file1.txt", 100, "source"))
        )
        fileSystem.addFileOfSize("/data/file1.txt", 100)

        val parameters = UploadParameters(
            customBatchSizeMb = "1",
            proxySpecification = "",
            shouldUseTor = false,
            shouldUploadZips = false,
            shouldUploadReadableList = true,
            shouldUploadUnreadableList = false,
            shouldUploadExcludedList = false,
            shouldUploadMissingList = false,
            shouldUploadSymlinkList = false,
            shouldUploadGetprop = false,
            shouldUploadAppLogs = false,
            zipEncryption = ZipEncryption.NONE,
            selectedService = mockk(),
            maxBatches = 0,
            useDoubleZipping = true
        )

        generator.prepare(parameters, scanResult)
        
        val result = generator.generateMisc()
        assertThat(result).isInstanceOf(hu.muzso.android_system_dumper.model.DomainResult.Success::class.java)
        
        val generatedZip = (result as hu.muzso.android_system_dumper.model.DomainResult.Success).data
        val dateStr = platformUtils.formatDate2Filename(Date.from(clock.now()))
        assertThat(generatedZip.filename).isEqualTo("${dateStr}_misc.zip")
        
        verifyInnerZipName(generatedZip.path, "${dateStr}_misc.plain.zip")
    }

    private suspend fun verifyInnerZipName(path: String, expectedInnerName: String) {
        val tempFile = File.createTempFile("outer", ".zip")
        try {
            fileSystem.openInputStream(path).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            ZipFile(tempFile).use { zipFile ->
                val entries = zipFile.fileHeaders.map { it.fileName }
                assertThat(entries).containsExactly(expectedInnerName)
            }
        } finally {
            tempFile.delete()
        }
    }
}
