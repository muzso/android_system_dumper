package hu.muzso.android_system_dumper.network.upload

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import hu.muzso.android_system_dumper.network.upload.gateway.GofileGateway
import hu.muzso.android_system_dumper.network.upload.gateway.GofileUploadDomainModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Retrofit
import javax.inject.Inject

class DefaultGofileGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retrofitBuilder: Retrofit.Builder,
    private val httpClientProvider: HttpClientProvider,
    private val clock: Clock,
    private val logger: FileLogger,
    private val fileSystem: FileSystem,
    private val torChecker: TorChecker,
    private val networkUtils: NetworkUtils
) : GofileGateway {

    companion object {
        private const val TAG = "DefaultGofileGateway"
    }

    private var baseUrl = "https://upload.gofile.io/"
    private var cachedApi: GofileApi? = null
    private var lastUsedClient: OkHttpClient? = null

    private fun getApi(): GofileApi {
        val client = httpClientProvider.getClient()
        if (client === lastUsedClient && cachedApi != null) return cachedApi!!

        val newApi = retrofitBuilder
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(GofileApi::class.java)
        
        cachedApi = newApi
        lastUsedClient = client
        return newApi
    }

    /**
     * Uploads a file to Gofile using the Retrofit API.
     * 
     * This method handles the creation of a multipart request body, monitors upload
     * progress. It handles HTTP responses, including success and redirects, and logs
     * any errors encountered during the process. Cooperative cancellation is supported
     * via the current coroutine context's job.
     *
     * @param fileName The name of the file to be uploaded.
     * @param filePath The local path to the file.
     * @param token An optional authentication token for Gofile.
     * @param folderId An optional folder ID to upload to.
     * @return A flow of [GatewayResult] containing the upload result or errors.
     */
    override fun upload(
        filePath: String,
        fileName: String,
        folderId: String?,
        token: String?
    ): Flow<GatewayResult<GofileUploadDomainModel>> = callbackFlow {
        val fileRequestBody = object : RequestBody() {
            override fun contentType(): MediaType? = "application/octet-stream".toMediaTypeOrNull()
            override fun contentLength(): Long = runBlocking {
                try {
                    fileSystem.size(filePath)
                } catch (_: Exception) {
                    -1L
                }
            }
            override fun writeTo(sink: BufferedSink) {
                runBlocking {
                    fileSystem.openInputStream(filePath).use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                        }
                    }
                }
            }
        }
        val multipartBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileRequestBody)

        folderId?.let {
            multipartBodyBuilder.addFormDataPart("folderId", it)
        }

        val countingRequestBody = CountingMultipartBody(
            multipartBodyBuilder.build(),
            clock,
            { written, total ->
                trySend(GatewayResult.Progress(written, total))
            },
            currentCoroutineContext()[Job]
        )

        try {
            val response = getApi().uploadFile(
                body = countingRequestBody,
                token = token?.let { "Bearer $it" }
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status?.lowercase() == "ok" && body.data != null) {
                    val domainModel = GofileUploadDomainModel(
                        downloadPage = body.data.downloadPage ?: "",
                        guestToken = body.data.guestToken,
                        parentFolder = body.data.parentFolder
                    )
                    trySend(GatewayResult.Success(domainModel))
                } else {
                    val msg = context.getString(R.string.http_error_from, response.code(), response.message())
                    logger.e(TAG, msg)
                    trySend(GatewayResult.Error(msg))
                }
            } else {
                val httpErrorMessage = response.message().ifBlank { networkUtils.httpErrorMessage(response.code()) }
                if (response.code() == 301) {
                    val msg = context.getString(R.string.http_redirect_from,response.code(), httpErrorMessage)
                    logger.e(TAG, msg)
                    trySend(GatewayResult.Error(msg))
                } else {
                    val msg = context.getString(R.string.http_error_from, response.code(), httpErrorMessage)
                    logger.e(TAG, msg)
                    trySend(GatewayResult.Error(msg))
                }
            }
            close()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            val msg = context.getString(R.string.error_processing_response, e.message)
            logger.e(TAG, msg)
            trySend(GatewayResult.Error(msg, e))
            close()
        }
        awaitClose()
    }

    override fun setBaseUrl(url: String) {
        baseUrl = url
        cachedApi = null
        lastUsedClient = null
    }

    override suspend fun torCheck(): Boolean = torChecker.check()

    override fun logConfiguration() {
        logger.i(TAG, httpClientProvider.getClient().configurationToString())
    }
}
