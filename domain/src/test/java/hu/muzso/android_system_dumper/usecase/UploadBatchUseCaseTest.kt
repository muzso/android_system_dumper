package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeTorServiceController
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.network.upload.TorChecker
import hu.muzso.android_system_dumper.network.upload.UploadExecutor
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.network.upload.UploadRetryPolicy
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
    private val torChecker = mockk<TorChecker>(relaxed = true)
    private lateinit var uploadBatchUseCase: UploadBatchUseCase

    @Before
    fun setup() {
        uploadBatchUseCase = UploadBatchUseCase(torService, torChecker, logger, executor, retryPolicy)
    }

    @Test
    fun `successful upload returns url`() = runTest {
        val file = File("test.zip")
        val expectedUrl = "https://success.url"
        
        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        coEvery { retryPolicy.withRetry(any(), any(), any(), any(), capture(blockSlot)) } coAnswers {
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
        coEvery { retryPolicy.withRetry(any(), any(), any(), any(), capture(blockSlot)) } coAnswers {
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
    fun `Tor service restart triggered on failure when using Tor`() = runTest {
        val file = File("test.zip")
        val expectedError = UploadError.Unknown("Bad Gateway")

        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        val onFailureSlot = slot<suspend (Int, Exception) -> Unit>()
        coEvery {
            retryPolicy.withRetry(any(), any(), any(), capture(onFailureSlot), capture(blockSlot))
        } coAnswers {
            try {
                blockSlot.captured.invoke()
            } catch (e: Exception) {
                onFailureSlot.captured.invoke(1, e)
                throw e
            }
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
        
        // Verify Tor restart was triggered via onFailure
        assertThat(torService.restartTorServiceCalls.get()).isEqualTo(1)
        coVerify(exactly = 1) { torChecker.check() }
        coVerify(exactly = 1) { executor.executeUpload(repository, any(), file.absolutePath, any()) }
    }

    @Test
    fun `Failure without Tor does not trigger Tor restart`() = runTest {
        val file = File("test.zip")

        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        val onFailureSlot = slot<suspend (Int, Exception) -> Unit>()
        coEvery {
            retryPolicy.withRetry(any(), any(), any(), capture(onFailureSlot), capture(blockSlot))
        } coAnswers {
            try {
                blockSlot.captured.invoke()
            } catch (e: Exception) {
                onFailureSlot.captured.invoke(1, e)
                throw e
            }
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

        assertThat(torService.restartTorServiceCalls.get()).isEqualTo(0)
        coVerify(exactly = 1) { executor.executeUpload(repository, any(), file.absolutePath, any()) }
    }

    @Test
    fun `Tor verification failure after restart aborts upload`() = runTest {
        val file = File("test.zip")
        val expectedError = UploadError.Unknown("Initial Fail")

        val blockSlot = slot<suspend () -> DomainResult<String, UploadError>>()
        val onFailureSlot = slot<suspend (Int, Exception) -> Unit>()
        
        // Mocking retryPolicy to behave like DefaultUploadRetryPolicy regarding TerminalUploadException
        coEvery {
            retryPolicy.withRetry(any(), any(), any(), capture(onFailureSlot), capture(blockSlot))
        } coAnswers {
            try {
                blockSlot.captured.invoke()
            } catch (e: Exception) {
                onFailureSlot.captured.invoke(1, e)
                throw e
            }
        }

        coEvery { executor.executeUpload(repository, any(), file.absolutePath, any()) } returns DomainResult.Error(expectedError)
        coEvery { torChecker.check() } returns false // Confirming a leak/verification failure

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
        val finalError = (result as DomainResult.Error).error
        assertThat(finalError).isInstanceOf(UploadError.TorVerificationFailed::class.java)
        
        // Verify it stopped after 1 attempt because of TerminalUploadException
        coVerify(exactly = 1) { executor.executeUpload(repository, any(), file.absolutePath, any()) }
        assertThat(torService.restartTorServiceCalls.get()).isEqualTo(1)
        coVerify(exactly = 1) { torChecker.check() }
    }
}
