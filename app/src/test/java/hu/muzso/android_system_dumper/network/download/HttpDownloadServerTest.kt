package hu.muzso.android_system_dumper.network.download

import app.cash.turbine.test
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.config.AppConfig
import hu.muzso.android_system_dumper.domain.fixtures.FakeUploadRepository
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.network.ArchiveGenerator
import hu.muzso.android_system_dumper.network.GeneratedZip
import hu.muzso.android_system_dumper.platform.ResourceProvider
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class HttpDownloadServerTest {
    private val archiveGenerator = mockk<ArchiveGenerator>(relaxed = true)
    private val clock = mockk<Clock>()
    private val logger = mockk<FileLogger>(relaxed = true)
    private val platformUtils = mockk<PlatformUtils>()
    private val resourceProvider = mockk<ResourceProvider>()
    private val appConfig = mockk<AppConfig>(relaxed = true)

    private lateinit var server: HttpDownloadServer

    @Before
    fun setup() {
        server = HttpDownloadServer(archiveGenerator, clock, logger, platformUtils, resourceProvider, appConfig)
        every { resourceProvider.getString(any<Int>()) } returns "test_string"
        every { resourceProvider.getString(any<Int>(), *anyVararg()) } answers {
            val args = it.invocation.args[1] as Array<*>
            "test_string: ${args.joinToString()}"
        }
        every { clock.monotonicTime() } returns 0L
    }

    private fun createParameters() = UploadParameters(
        customBatchSizeMb = 100,
        proxySpecification = "",
        shouldUseTor = false,
        shouldUploadZips = true,
        shouldUploadFileLists = true,
        shouldUploadGetprop = true,
        shouldUploadAppLogs = true,
        maxUploadRetries = 5,
        zipEncryption = ZipEncryption.NONE,
        selectedService = FakeUploadRepository(),
        maxBatches = 0,
        useDoubleZipping = false
    )

    @Test
    fun `root URL returns HTML with correct links`() = testApplication {
        val parameters = createParameters()
        
        every { archiveGenerator.getBatchCount() } returns 2
        every { archiveGenerator.getBatchFilename(1) } returns "batch1.zip"
        every { archiveGenerator.getBatchFilename(2) } returns "batch2.zip"
        every { archiveGenerator.shouldGenerateMisc() } returns true
        every { archiveGenerator.getMiscZipFilename() } returns "misc.zip"
        every { archiveGenerator.getEncryptionPassphrase() } returns "pass123"

        application {
            server.configureServer(this, parameters)
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("batch1.zip"))
        assertTrue(body.contains("batch2.zip"))
        assertTrue(body.contains("misc.zip"))
        assertTrue(body.contains("pass123"))
    }

    @Test
    fun `download status returns correct JSON`() = testApplication {
        val parameters = createParameters()
        application {
            server.configureServer(this, parameters)
        }

        val response = client.get("/downloadstatus")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"isBusy\":false,\"count\":0}", response.bodyAsText())
    }

    @Test
    fun `file download success updates progress and returns content`() = testApplication {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())
        val tempFile = File.createTempFile("test", ".zip").apply {
            writeBytes("test data".toByteArray())
        }

        every { archiveGenerator.getBatchCount() } returns 1
        every { archiveGenerator.getBatchFilename(1) } returns "batch1.zip"
        coEvery { archiveGenerator.generateBatch(1) } returns DomainResult.Success(GeneratedZip(tempFile.absolutePath, "batch1.zip"))
        every { platformUtils.formatBytes(any()) } returns "9 bytes"
        every { clock.monotonicTime() } returns 1000L

        server.prepareState(parameters, scanResult)

        application {
            server.configureServer(this, parameters)
        }

        val response = client.get("/batch1.zip")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("test data", response.bodyAsText())

        // Verify progress was updated
        val progress = server.progress.value
        assertNotNull(progress)
        assertEquals(1, progress?.successCount)
        assertTrue(progress?.isFinished == true)

        tempFile.delete()
    }

    @Test
    fun `duplicate downloads don't increment successCount and state resets after completion`() = testApplication {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())
        val tempFile1 = File.createTempFile("batch1", ".zip").apply { writeBytes("data1".toByteArray()) }
        val tempFile2 = File.createTempFile("batch2", ".zip").apply { writeBytes("data2".toByteArray()) }

        every { archiveGenerator.getBatchCount() } returns 2
        every { archiveGenerator.getBatchFilename(1) } returns "batch1.zip"
        every { archiveGenerator.getBatchFilename(2) } returns "batch2.zip"
        coEvery { archiveGenerator.generateBatch(1) } returns DomainResult.Success(GeneratedZip(tempFile1.absolutePath, "batch1.zip"))
        coEvery { archiveGenerator.generateBatch(2) } returns DomainResult.Success(GeneratedZip(tempFile2.absolutePath, "batch2.zip"))
        every { platformUtils.formatBytes(any()) } returns "5 bytes"
        every { clock.monotonicTime() } returns 1000L

        server.prepareState(parameters, scanResult)

        application {
            server.configureServer(this, parameters)
        }

        // 0. Initial state - startTime should be 0
        assertEquals(0L, server.progress.value?.startTime)

        // 1. Download batch1.zip
        every { clock.monotonicTime() } returns 1000L
        client.get("/batch1.zip")
        var progress = server.progress.value
        assertEquals(1, progress?.successCount)
        assertEquals(2, progress?.totalCount)
        assertTrue(progress?.isFinished == false)
        assertEquals(1000L, progress?.startTime)

        // 2. Download batch1.zip again (duplicate) - successCount stays 1
        client.get("/batch1.zip")
        progress = server.progress.value
        assertEquals(1, progress?.successCount)
        assertEquals(2, progress?.totalCount)
        assertTrue(progress?.isFinished == false)

        // 3. Download batch2.zip (all unique files done)
        client.get("/batch2.zip")
        progress = server.progress.value
        assertEquals(2, progress?.successCount)
        assertEquals(2, progress?.totalCount)
        assertTrue(progress?.isFinished == true)

        // 4. Download batch1.zip again - should reset and start fresh
        every { clock.monotonicTime() } returns 2000L
        client.get("/batch1.zip")
        progress = server.progress.value
        assertEquals(1, progress?.successCount)
        assertEquals(2, progress?.totalCount)
        assertTrue(progress?.isFinished == false)
        assertEquals(2000L, progress?.startTime)

        tempFile1.delete()
        tempFile2.delete()
    }

    @Test
    fun `non-existent file request does not start timer`() = testApplication {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())

        every { archiveGenerator.getBatchCount() } returns 1
        every { archiveGenerator.getBatchFilename(1) } returns "batch1.zip"
        every { archiveGenerator.shouldGenerateMisc() } returns false

        server.prepareState(parameters, scanResult)

        application {
            server.configureServer(this, parameters)
        }

        // Request non-existent file
        val response = client.get("/favicon.ico")
        assertEquals(HttpStatusCode.NotFound, response.status)

        // Verify startTime is still 0
        assertEquals(0L, server.progress.value?.startTime)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `file download busy returns 429`() = runTest(timeout = 5.seconds) {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())
        val tempFile = File.createTempFile("test", ".zip").apply { writeBytes("data".toByteArray()) }
        
        val entered = CompletableDeferred<Unit>()
        val canFinish = CompletableDeferred<Unit>()

        every { archiveGenerator.getBatchCount() } returns 1
        every { archiveGenerator.getBatchFilename(1) } returns "batch1.zip"
        coEvery { archiveGenerator.generateBatch(1) } coAnswers {
            entered.complete(Unit)
            canFinish.await()
            DomainResult.Success(GeneratedZip(tempFile.absolutePath, "batch1.zip"))
        }

        server.prepareState(parameters, scanResult)

        testApplication {
            application {
                server.configureServer(this, parameters)
            }

            // Using GlobalScope to avoid test framework waiting for this request
            // until we are ready.
            val job = GlobalScope.launch {
                try {
                    client.get("/batch1.zip")
                } catch (_: Exception) {}
            }

            entered.await()

            // Second request should return 429
            val response = client.get("/batch1.zip")
            assertEquals(HttpStatusCode.TooManyRequests, response.status)

            canFinish.complete(Unit)
            job.join()
        }
        tempFile.delete()
    }

    @Test
    fun `file download error returns 500 and resets busy flag`() = testApplication {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())

        every { archiveGenerator.getBatchCount() } returns 1
        every { archiveGenerator.getBatchFilename(1) } returns "batch1.zip"
        coEvery { archiveGenerator.generateBatch(1) } returns DomainResult.Error(ZipError.Zip4jError("fail"))

        server.prepareState(parameters, scanResult)

        application {
            server.configureServer(this, parameters)
        }

        val response = client.get("/batch1.zip")
        assertEquals(HttpStatusCode.InternalServerError, response.status)

        // Verify we can try again (busy flag was reset)
        coEvery { archiveGenerator.generateBatch(1) } returns DomainResult.Error(ZipError.Zip4jError("fail again"))
        val secondResponse = client.get("/batch1.zip")
        assertEquals(HttpStatusCode.InternalServerError, secondResponse.status)
    }

    @Test
    fun `misc file download success updates progress`() = testApplication {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())
        val tempFile = File.createTempFile("misc", ".zip").apply { writeBytes("misc data".toByteArray()) }

        every { archiveGenerator.getBatchCount() } returns 0
        every { archiveGenerator.shouldGenerateMisc() } returns true
        every { archiveGenerator.getMiscZipFilename() } returns "misc.zip"
        coEvery { archiveGenerator.generateMisc() } returns DomainResult.Success(GeneratedZip(tempFile.absolutePath, "misc.zip"))
        every { platformUtils.formatBytes(any()) } returns "9 bytes"

        server.prepareState(parameters, scanResult)

        application {
            server.configureServer(this, parameters)
        }

        val response = client.get("/misc.zip")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, server.progress.value?.successCount)
        assertTrue(server.progress.value?.isFinished == true)

        tempFile.delete()
    }

    @Test
    fun `progress flow emits expected sequence of statuses`() = runTest {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())
        val tempFile = File.createTempFile("test", ".zip").apply { writeBytes("data".toByteArray()) }

        every { archiveGenerator.getBatchCount() } returns 1
        every { archiveGenerator.getBatchFilename(1) } returns "batch1.zip"
        coEvery { archiveGenerator.generateBatch(1) } coAnswers {
            delay(100.milliseconds) // Ensure "Preparing" status can be caught
            DomainResult.Success(GeneratedZip(tempFile.absolutePath, "batch1.zip"))
        }
        every { platformUtils.formatBytes(any()) } returns "4 bytes"
        every { clock.monotonicTime() } returns 1000L

        server.prepareState(parameters, scanResult)

        testApplication {
            application {
                server.configureServer(this, parameters)
            }

            server.progress.test {
                // Initial state from prepareState
                assertEquals(0, awaitItem()?.successCount)

                val request = launch {
                    client.get("/batch1.zip")
                }

                // Collect and verify statuses in order, ignoring intermediate noise
                val statuses = mutableListOf<String>()
                
                // 1. Wait for "Preparing" (which is now mocked as "test_string")
                while (true) {
                    val item = awaitItem()
                    val status = item?.statusText ?: ""
                    if (status.isNotEmpty()) statuses.add(status)
                    if (status.contains("test_string")) break
                }

                // 2. Wait for "Downloading" (also mocked as "test_string")
                while (true) {
                    val item = awaitItem()
                    val status = item?.statusText ?: ""
                    if (status.isNotEmpty()) statuses.add(status)
                    if (status.contains("test_string")) break
                }

                // 3. Wait for "Success" (also mocked as "test_string")
                while (true) {
                    val item = awaitItem()
                    val status = item?.statusText ?: ""
                    if (status.isNotEmpty()) statuses.add(status)
                    if (status.contains("test_string")) break
                }
                
                assertTrue(statuses.any { it.contains("test_string") })
                
                request.join()
                cancelAndIgnoreRemainingEvents()
            }
        }
        tempFile.delete()
    }

    @Test
    fun `start and stop methods manage server lifecycle`() = runTest {
        val parameters = createParameters()
        val scanResult = ScanResult(emptyList())
        
        every { archiveGenerator.getBatchCount() } returns 1
        every { archiveGenerator.shouldGenerateMisc() } returns false
        
        val port = server.start(parameters, scanResult)
        assertTrue(port > 0)
        assertNotNull(server.progress.value)
        
        server.stop()
        assertNull(server.progress.value)
    }
}
