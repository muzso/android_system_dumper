package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.network.upload.TerminalUploadException
import hu.muzso.android_system_dumper.network.upload.TorChecker
import hu.muzso.android_system_dumper.network.upload.UploadExecutor
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.network.upload.UploadRetryPolicy
import hu.muzso.android_system_dumper.platform.TorServiceController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class UploadBatchUseCase(
    private val torServiceController: TorServiceController,
    private val torChecker: TorChecker,
    private val logger: FileLogger,
    private val executor: UploadExecutor,
    private val retryPolicy: UploadRetryPolicy
) {
    private val tag = "UploadBatchUseCase"

    /**
     * Executes the upload of a single batch of files.
     * 
     * This function handles the upload process with optional Tor support and retry logic.
     * 
     * @param repository The repository to use for the upload.
     * @param fileName The name of the file to be uploaded.
     * @param filePath The local path to the file to be uploaded.
     * @param retries The number of retries allowed.
     * @param fileLabel A label for the file, used for logging and progress updates.
     * @param shouldUseTor Whether to use Tor for the upload.
     * @param onProgress A callback for reporting upload progress.
     * @param onStatusUpdate A callback for reporting status updates and retry attempts.
     * @return A [DomainResult] containing the upload result URL or an error.
     */
    suspend fun execute(
        repository: UploadRepository,
        fileName: String,
        filePath: String,
        retries: Int,
        fileLabel: String,
        shouldUseTor: Boolean,
        onProgress: suspend (written: Long, total: Long) -> Unit,
        onStatusUpdate: suspend (label: String, attempt: Int, totalRetries: Int) -> Unit
    ): DomainResult<String, UploadError> {
        val uploadBlock = suspend {
            val result = executor.executeUpload(repository, fileName, filePath, onProgress)
            if (result is DomainResult.Error) {
                throw UploadRetryException(result.error)
            }
            result
        }

        try {
            return retryPolicy.withRetry(
                label = fileLabel,
                retries = retries,
                onStatusUpdate = onStatusUpdate,
                onFailure = { attempt, _ ->
                    if (shouldUseTor && currentCoroutineContext().isActive && attempt < retries) {
                        logger.i(tag, "Upload attempt $attempt failed with Tor. Restarting Tor service...")
                        if (torServiceController.restartTorService(60000L)) {
                            logger.i(tag, "Tor service restarted successfully. Verifying Tor connection...")
                            try {
                                if (torChecker.check(retries)) {
                                    logger.i(tag, "Tor verification successful")
                                } else {
                                    val error = UploadError.TorVerificationFailed("Tor verification failed: requests not going through Tor")
                                    logger.e(tag, error.message)
                                    throw TerminalUploadException(error)
                                }
                            } catch (e: Exception) {
                                if (e is TerminalUploadException) throw e
                                val error = UploadError.TorVerificationFailed("Tor verification failed with error: ${e.message}")
                                logger.e(tag, error.message, e)
                                throw TerminalUploadException(error)
                            }
                        } else {
                            logger.w(tag, "Timed out waiting for new Tor circuit, will retry anyway")
                        }
                    }
                }
            ) {
                uploadBlock()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val error = when (e) {
                is UploadRetryException -> e.error
                is TerminalUploadException -> e.error
                else -> UploadError.Unknown(e.message ?: "Upload failed", e)
            }
            return DomainResult.Error(error)
        }
    }

    private class UploadRetryException(val error: UploadError) : Exception()
}
