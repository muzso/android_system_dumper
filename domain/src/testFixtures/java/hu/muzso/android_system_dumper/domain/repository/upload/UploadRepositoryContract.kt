package hu.muzso.android_system_dumper.domain.repository.upload

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.upload.UploadResult
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.nio.file.Files

abstract class UploadRepositoryContract {

    abstract fun createRepository(): UploadRepository

    private suspend fun ReceiveTurbine<UploadResult>.awaitFinalResult(): UploadResult {
        var item = awaitItem()
        while (item is UploadResult.Progress) {
            item = awaitItem()
        }
        return item
    }

    /**
     * Set up the repository's gateway/backend to return a success response for the given file.
     */
    open fun setupSuccessResponse(filePath: String, fileName: String) {}

    /**
     * Set up the repository's gateway/backend to return a network error.
     */
    open fun setupNetworkErrorResponse() {}

    /**
     * Set up the repository's gateway/backend to return an authentication error.
     */
    open fun setupAuthErrorResponse() {}

    @Test
    fun id_and_name_are_not_blank() {
        val repository = createRepository()
        assertThat(repository.id).isNotEmpty()
        assertThat(repository.name).isNotEmpty()
    }

    @Test
    fun upload_reports_progress_and_success() = runTest {
        val repository = createRepository()
        val tempFile = Files.createTempFile("test", ".txt").toFile()
        tempFile.writeText("test content")
        val fileName = "test.txt"

        setupSuccessResponse(tempFile.absolutePath, fileName)

        repository.upload(tempFile.absolutePath, fileName).test {
            var lastProgress = -1L
            while (true) {
                val item = awaitItem()
                if (item is UploadResult.Success) {
                    assertThat(item.url).isNotEmpty()
                    break
                }
                if (item is UploadResult.Progress) {
                    assertThat(item.bytesWritten).isAtLeast(lastProgress)
                    lastProgress = item.bytesWritten
                    assertThat(item.totalBytes).isEqualTo(tempFile.length())
                }
            }
            awaitComplete()
        }
        tempFile.delete()
    }

    @Test
    fun upload_reports_network_error() = runTest {
        val repository = createRepository()
        val tempFile = Files.createTempFile("test_err", ".txt").toFile()
        tempFile.writeText("content")

        setupNetworkErrorResponse()

        repository.upload(tempFile.absolutePath, "test.txt").test {
            val result = awaitFinalResult()
            assertThat(result).isInstanceOf(UploadResult.Error::class.java)
            val error = (result as UploadResult.Error).error
            assertThat(error).isInstanceOf(UploadError.NetworkError::class.java)
            awaitComplete()
        }
        tempFile.delete()
    }

    @Test
    fun upload_reports_auth_error() = runTest {
        val repository = createRepository()
        val tempFile = Files.createTempFile("test_auth_err", ".txt").toFile()
        tempFile.writeText("content")

        setupAuthErrorResponse()

        repository.upload(tempFile.absolutePath, "test.txt").test {
            val result = awaitFinalResult()
            assertThat(result).isInstanceOf(UploadResult.Error::class.java)
            val error = (result as UploadResult.Error).error
            assertThat(error).isInstanceOf(UploadError.AuthenticationError::class.java)
            awaitComplete()
        }
        tempFile.delete()
    }

    @Test
    fun incrementTotalUploadedBytes_updates_totalUploadedBytes() = runTest {
        val repository = createRepository()
        val initial = repository.totalUploadedBytes.value
        repository.incrementTotalUploadedBytes(100L)
        assertThat(repository.totalUploadedBytes.value).isEqualTo(initial + 100L)
    }

    @Test
    fun reset_clears_state() = runTest {
        val repository = createRepository()
        repository.incrementTotalUploadedBytes(100L)

        val tempFile = Files.createTempFile("test_reset", ".txt").toFile()
        tempFile.writeText("content")
        setupSuccessResponse(tempFile.absolutePath, "test.txt")
        repository.upload(tempFile.absolutePath, "test.txt").test {
            awaitFinalResult()
            awaitComplete()
        }

        repository.reset()

        assertThat(repository.totalUploadedBytes.value).isEqualTo(0L)
        assertThat(repository.getUrlListUrl()).isEmpty()
        tempFile.delete()
    }

    @Test
    fun getUrlListUrl_returns_expected_url_after_upload() = runTest {
        val repository = createRepository()
        val tempFile = Files.createTempFile("test_url", ".txt").toFile()
        tempFile.writeText("content")
        setupSuccessResponse(tempFile.absolutePath, "test.txt")

        repository.upload(tempFile.absolutePath, "test.txt").test {
            val success = awaitFinalResult() as UploadResult.Success
            assertThat(repository.getUrlListUrl()).contains(success.url.substringBeforeLast("/"))
            awaitComplete()
        }
        tempFile.delete()
    }
}
