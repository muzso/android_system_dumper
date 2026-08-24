package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeTorServiceController
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.upload.network.UploadExecutor
import hu.muzso.android_system_dumper.upload.network.UploadRepository
import hu.muzso.android_system_dumper.upload.network.UploadRetryPolicy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UploadBatchUseCaseTest {

    private val torService = FakeTorServiceController()
    private val logger = FakeFileLogger()
    private val repository = mockk<UploadRepository>(relaxed = true)
    private val executor = mockk<UploadExecutor>()
    private val retryPolicy = mockk<UploadRetryPolicy>()
    private lateinit var uploadBatchUseCase: UploadBatchUseCase

    @Before
    fun setup() {
        uploadBatchUseCase = UploadBatchUseCase(torService, logger, executor, retryPolicy)
    }

    @Test
    fun `successful upload returns url`() = runTest {
        val file = File("test.zip")
        val expectedUrl = "https://success.url"
        
        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        coEvery { retryPolicy.withRetry(any(), any(), any(), capture(blockSlot)) } coAnswers {
            blockSlot.captured.invoke()
        }
        
        coEvery { executor.executeUpload(repository, any(), file.absolutePath, any()) } returns DomainResult.Success(expectedUrl)

        val result = uploadBatchUseCase.execute(
            repository = repository,
            fileName = "test.zip",
            filePath = file.absolutePath,
            retries = 3,
            fileLabel = "Test Label",
            shouldUseTor = false,
            onProgress = { _, _ -> },
            onStatusUpdate = { _, _, _ -> }
        )

        assertThat(result).isEqualTo(DomainResult.Success(expectedUrl))
    }

    @Test
    fun `executor failure returns error`() = runTest {
        val file = File("test.zip")
        val expectedError = UploadError.Unknown("Fail")
        
        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        coEvery { retryPolicy.withRetry(any(), any(), any(), capture(blockSlot)) } coAnswers {
            blockSlot.captured.invoke()
        }
        
        coEvery { executor.executeUpload(repository, any(), file.absolutePath, any()) } returns DomainResult.Error(expectedError)

        val result = uploadBatchUseCase.execute(
            repository = repository,
            fileName = "test.zip",
            filePath = file.absolutePath,
            retries = 2,
            fileLabel = "Test Label",
            shouldUseTor = false,
            onProgress = { _, _ -> },
            onStatusUpdate = { _, _, _ -> }
        )

        assertThat(result).isEqualTo(DomainResult.Error(expectedError))
    }

    @Test
    fun `DomainResult Error triggers Tor circuit rebuild when using Tor`() = runTest {
        val file = File("test.zip")
        val expectedError = UploadError.Unknown("Bad Gateway")

        // Mock retryPolicy to just invoke the block and propagate exceptions
        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        coEvery {
            retryPolicy.withRetry(any(), any(), any(), capture(blockSlot))
        } coAnswers {
            blockSlot.captured.invoke()
        }

        coEvery { executor.executeUpload(repository, any(), file.absolutePath, any()) } returns DomainResult.Error(expectedError)

        val result = uploadBatchUseCase.execute(
            repository = repository,
            fileName = "test.zip",
            filePath = file.absolutePath,
            retries = 3,
            fileLabel = "Test Label",
            shouldUseTor = true,
            onProgress = { _, _ -> },
            onStatusUpdate = { _, _, _ -> }
        )

        assertThat(result).isInstanceOf(DomainResult.Error::class.java)
        assertThat((result as DomainResult.Error).error).isEqualTo(expectedError)

        // Verify Tor rebuild was triggered
        assertThat(torService.rebuildCircuitCalls.get()).isEqualTo(1)
        // Verify executor was called twice: once in first withRetry, once in second withRetry after rebuild
        coVerify(exactly = 2) { executor.executeUpload(repository, any(), file.absolutePath, any()) }
    }

    @Test
    fun `Generic exception triggers Tor circuit rebuild when using Tor`() = runTest {
        val file = File("test.zip")
        val expectedError = "Some IO Error"

        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        coEvery {
            retryPolicy.withRetry(any(), any(), any(), capture(blockSlot))
        } coAnswers {
            blockSlot.captured.invoke()
        }

        coEvery { executor.executeUpload(repository, any(), file.absolutePath, any()) } throws RuntimeException(expectedError)

        val result = uploadBatchUseCase.execute(
            repository = repository,
            fileName = "test.zip",
            filePath = file.absolutePath,
            retries = 1,
            fileLabel = "Test Label",
            shouldUseTor = true,
            onProgress = { _, _ -> },
            onStatusUpdate = { _, _, _ -> }
        )

        assertThat(result).isInstanceOf(DomainResult.Error::class.java)
        assertThat((result as DomainResult.Error).error).isInstanceOf(UploadError.Unknown::class.java)

        assertThat(torService.rebuildCircuitCalls.get()).isEqualTo(1)
        coVerify(exactly = 2) { executor.executeUpload(repository, any(), file.absolutePath, any()) }
    }

    @Test
    fun `Tor wait circuit timeout still attempts retry`() = runTest {
        val file = File("test.zip")
        torService.setCircuitReady(false) // Simulate timeout

        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        coEvery {
            retryPolicy.withRetry(any(), any(), any(), capture(blockSlot))
        } coAnswers {
            blockSlot.captured.invoke()
        }

        coEvery { executor.executeUpload(repository, any(), file.absolutePath, any()) } returns DomainResult.Error(UploadError.NetworkError("Fail", null))

        uploadBatchUseCase.execute(
            repository = repository,
            fileName = "test.zip",
            filePath = file.absolutePath,
            retries = 1,
            fileLabel = "Test Label",
            shouldUseTor = true,
            onProgress = { _, _ -> },
            onStatusUpdate = { _, _, _ -> }
        )

        assertThat(torService.rebuildCircuitCalls.get()).isEqualTo(1)
        // One from initial withRetry, one from retry after Tor rebuild (even if timeout)
        coVerify(exactly = 2) { executor.executeUpload(repository, any(), file.absolutePath, any()) }
    }

    @Test
    fun `Failure without Tor does not trigger Tor rebuild`() = runTest {
        val file = File("test.zip")

        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        coEvery {
            retryPolicy.withRetry(any(), any(), any(), capture(blockSlot))
        } coAnswers {
            blockSlot.captured.invoke()
        }

        coEvery { executor.executeUpload(repository, any(), file.absolutePath, any()) } returns DomainResult.Error(UploadError.NetworkError("Fail", null))

        uploadBatchUseCase.execute(
            repository = repository,
            fileName = "test.zip",
            filePath = file.absolutePath,
            retries = 1,
            fileLabel = "Test Label",
            shouldUseTor = false,
            onProgress = { _, _ -> },
            onStatusUpdate = { _, _, _ -> }
        )

        assertThat(torService.rebuildCircuitCalls.get()).isEqualTo(0)
        coVerify(exactly = 1) { executor.executeUpload(repository, any(), file.absolutePath, any()) }
    }
}
