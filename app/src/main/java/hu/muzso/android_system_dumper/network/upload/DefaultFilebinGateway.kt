package hu.muzso.android_system_dumper.network.upload

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.network.upload.gateway.FilebinGateway
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject

class DefaultFilebinGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retrofitBuilder: Retrofit.Builder,
    private val httpClientProvider: HttpClientProvider,
    private val clock: Clock,
    private val logger: FileLogger,
    private val fileSystem: FileSystem,
    private val torChecker: TorChecker,
    private val networkUtils: NetworkUtils
) : FilebinGateway {

    companion object {
        private const val TAG = "DefaultFilebinGateway"
    }

    private var baseUrl = "https://filebin.net/"
    private var cachedApi: FilebinApi? = null
    private var lastUsedClient: OkHttpClient? = null

    private fun getApi(): FilebinApi {
        val client = httpClientProvider.getClient()
        if (client === lastUsedClient && cachedApi != null) return cachedApi!!

        val newApi = retrofitBuilder
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(FilebinApi::class.java)

        cachedApi = newApi
        lastUsedClient = client
        return newApi
    }

    /**
     * Uploads a file to Filebin using the Retrofit API.
     * 
     * This method creates a [CountingRequestBody] to stream the file while tracking progress.
     * It handles HTTP responses, including success and redirects, and logs any errors 
     * encountered during the process. Cooperative cancellation is supported via the 
     * current coroutine context's job.
     *
     * @param bin The name of the bin to upload to.
     * @param fileName The name to give the file in the bin.
     * @param filePath The local path to the file to be uploaded.
     * @return A flow of [GatewayResult] emitting progress updates and the final outcome.
     */
    override fun upload(bin: String, fileName: String, filePath: String): Flow<GatewayResult<Unit>> = callbackFlow {
        val countingRequestBody = CountingRequestBody(
            filePath,
            fileSystem,
            clock,
            "application/octet-stream",
            { written, total ->
                trySend(GatewayResult.Progress(written, total))
            },
            currentCoroutineContext()[Job]
        )

        try {
            val response = getApi().upload(
                bin = bin,
                filename = fileName,
                filenameHeader = fileName,
                body = countingRequestBody
            )
            if (response.isSuccessful) {
                trySend(GatewayResult.Success(Unit))
                close()
            } else {
                val httpErrorMessage = response.message().ifBlank { networkUtils.httpErrorMessage(response.code()) }
                if (response.code() == 301) {
                    val msg = context.getString(R.string.http_redirect_from, response.code(), httpErrorMessage)
                    logger.e(TAG, msg)
                    trySend(GatewayResult.Error(msg))
                    close()
                } else {
                    val msg = context.getString(R.string.http_error_from, response.code(), httpErrorMessage)
                    logger.e(TAG, msg)
                    trySend(GatewayResult.Error(msg))
                    close()
                }
            }
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
