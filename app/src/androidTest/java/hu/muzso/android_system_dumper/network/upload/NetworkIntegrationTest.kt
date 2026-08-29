package hu.muzso.android_system_dumper.network.upload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.DefaultNetworkUtils
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.upload.UploadResult
import hu.muzso.android_system_dumper.platform.TorServiceController
import hu.muzso.android_system_dumper.usecase.UploadBatchUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NetworkIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: GofileUploadRepository
    private lateinit var useCase: UploadBatchUseCase
    private val okHttpClient = OkHttpClient.Builder().build()
    private val fileSystem = FakeMemoryFileSystem()
    private lateinit var networkUtils: NetworkUtils

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        networkUtils = DefaultNetworkUtils()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
        val clock = mockk<Clock>(relaxed = true)
        val logger = mockk<FileLogger>(relaxed = true)
        val torChecker = mockk<TorChecker>(relaxed = true)
        val httpClientProvider = mockk<HttpClientProvider>(relaxed = true)
        every { httpClientProvider.getClient() } returns okHttpClient

        val gateway = DefaultGofileGateway(context, retrofitBuilder, httpClientProvider, clock, logger, fileSystem, torChecker, networkUtils)
        repository = GofileUploadRepository(gateway)
        repository.setBaseUrl(server.url("/").toString())

        val progressTracker = mockk<UploadProgressTracker>(relaxed = true)
        every { progressTracker.totalUploadedBytes } returns MutableStateFlow(0L)
        val executor = DefaultUploadExecutor(progressTracker, logger)
        val retryPolicy = DefaultUploadRetryPolicy(logger)
        val torServiceController = mockk<TorServiceController>(relaxed = true)

        useCase = UploadBatchUseCase(torServiceController, torChecker, logger, executor, retryPolicy)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun upload_sendsCorrectMultipartRequest() = runTest {
        // Arrange
        val filePath = "/upload_test.txt"
        fileSystem.addFileWithText(filePath, text = "test file content")

        val jsonResponse = """
            {
                "status": "ok",
                "data": {
                    "downloadPage": "https://gofile.io/d/test123",
                    "code": "test123",
                    "parentFolder": "folder_abc",
                    "fileId": "file_xyz",
                    "fileName": "upload_test.txt",
                    "md5": "...",
                    "guestToken": "token_123"
                }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setBody(jsonResponse))

        // Act
        val results = repository.upload(filePath, "upload_test.txt").toList()

        // Assert
        Truth.assertThat(results).isNotEmpty()
        val successResult = results.last() as UploadResult.Success
        Truth.assertThat(successResult.url).isEqualTo("https://gofile.io/d/test123")

        val recordedRequest = server.takeRequest()
        Truth.assertThat(recordedRequest.method).isEqualTo("POST")
        Truth.assertThat(recordedRequest.path).isEqualTo("/uploadfile")

        val contentType = recordedRequest.getHeader("Content-Type")
        Truth.assertThat(contentType).contains("multipart/form-data")

        val bodyText = recordedRequest.body.readUtf8()
        Truth.assertThat(bodyText).contains("test file content")
        Truth.assertThat(bodyText).contains("filename=\"upload_test.txt\"")
    }

    @Test
    fun upload_handlesErrorResponse() = runTest {
        // Arrange
        val filePath = "/error_test.txt"
        fileSystem.addFileWithText(filePath, text = "content")

        server.enqueue(MockResponse().setResponseCode(500))

        // Act
        val results = repository.upload(filePath, "error_test.txt").toList()

        // Assert
        Truth.assertThat(results.last()).isInstanceOf(UploadResult.Error::class.java)
        val errorResult = results.last() as UploadResult.Error
        val errorMessage = when (val error = errorResult.error) {
            is UploadError.NetworkError -> error.message
            is UploadError.ServerError -> error.message
            is UploadError.AuthenticationError -> error.message
            is UploadError.FileNotFoundError -> "File not found: ${error.path}"
            is UploadError.Cancelled -> error.message
            is UploadError.ZeroSuccessfulUploads -> error.message
            is UploadError.MissingDownloadURL -> error.message
            is UploadError.InsufficientStorage -> "Insufficient storage: ${error.requiredBytes} bytes required"
            is UploadError.TorVerificationFailed -> error.message
            is UploadError.Unknown -> error.message
        }
        Truth.assertThat(errorMessage).contains("500")
    }

    @Test
    fun upload_retriesOnFailureAndSucceeds() = runTest {
        // Arrange
        val filePath = "/retry_test.txt"
        fileSystem.addFileWithText(filePath, text = "retry content")

        // 1st attempt: 500 Error
        server.enqueue(MockResponse().setResponseCode(500))
        // 2nd attempt: Success
        val jsonResponse =
            """{"status": "ok", "data": {"downloadPage": "https://gofile.io/d/retry"}}"""
        server.enqueue(MockResponse().setBody(jsonResponse))

        // Act
        val result = useCase.execute(
            repository = repository,
            fileName = "retry_test.txt",
            filePath = filePath,
            retries = 3,
            fileLabel = "retry_test.txt",
            shouldUseTor = false,
            onProgress = { _, _ -> },
        ) { _, _, _ -> }

        // Assert
        Truth.assertThat(result).isInstanceOf(DomainResult.Success::class.java)
        Truth.assertThat((result as DomainResult.Success).data)
            .isEqualTo("https://gofile.io/d/retry")

        Truth.assertThat(server.requestCount).isEqualTo(2)
    }
}