package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.platform.TorServiceController
import hu.muzso.android_system_dumper.upload.network.UploadExecutor
import hu.muzso.android_system_dumper.upload.network.UploadRepository
import hu.muzso.android_system_dumper.upload.network.UploadRetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class UploadBatchUseCase(
    private val torServiceController: TorServiceController,
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
            return retryPolicy.withRetry(fileLabel, retries, onStatusUpdate) {
                uploadBlock()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val error = (e as? UploadRetryException)?.error ?: UploadError.Unknown(e.message ?: "Upload failed", e)

            if (shouldUseTor && currentCoroutineContext().isActive) {
                logger.i(tag, "Upload failed after $retries retries with Tor. Rebuilding circuit and retrying...")
                torServiceController.rebuildCircuit()
                if (torServiceController.waitForCircuit(30000L)) {
                    logger.i(tag, "New Tor circuit established, retrying upload")
                } else {
                    logger.w(tag, "Timed out waiting for new Tor circuit, retrying anyway")
                }
                return try {
                    retryPolicy.withRetry(fileLabel, 1, onStatusUpdate) {
                        uploadBlock()
                    }
                } catch (e2: Exception) {
                    if (e2 is CancellationException) throw e2
                    val error2 = (e2 as? UploadRetryException)?.error ?: UploadError.Unknown(e2.message ?: "Upload failed", e2)
                    DomainResult.Error(error2)
                }
            }
            return DomainResult.Error(error)
        }
    }

    private class UploadRetryException(val error: UploadError) : Exception()
}
