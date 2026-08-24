package hu.muzso.android_system_dumper.upload.network

import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.upload.UploadResult
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UploadExecutor"

@Singleton
class DefaultUploadExecutor @Inject constructor(
    private val progressTracker: UploadProgressTracker,
    private val logger: FileLogger
) : UploadExecutor {

    /**
     * Executes the upload of a file using the provided [repository].
     * 
     * This method collects the [UploadResult] flow from the repository, updates the 
     * [progressTracker] with byte increments, and reports progress via the [onProgress] 
     * callback. It returns a terminal [DomainResult] representing success or failure.
     *
     * @param repository The repository to use for the upload.
     * @param fileName The name of the file to be uploaded.
     * @param filePath The local path to the file.
     * @param onProgress A suspending callback for reporting progress updates.
     * @return A [DomainResult] containing the upload URL or an error.
     */
    override suspend fun executeUpload(
        repository: UploadRepository,
        fileName: String,
        filePath: String,
        onProgress: suspend (written: Long, total: Long) -> Unit
    ): DomainResult<String, UploadError> {
        var finalResult: UploadResult? = null
        var lastWritten = 0L
        repository.upload(filePath, fileName).collect { result ->
            when (result) {
                is UploadResult.Progress -> {
                    val delta = result.bytesWritten - lastWritten
                    if (delta > 0) {
                        progressTracker.incrementTotalUploadedBytes(delta)
                        lastWritten = result.bytesWritten
                    }
                    onProgress(result.bytesWritten, result.totalBytes)
                }
                is UploadResult.Success, is UploadResult.Error -> {
                    logger.d(TAG, "Final result received: $result")
                    finalResult = result
                }
            }
        }

        return when (val result = finalResult) {
            is UploadResult.Success -> DomainResult.Success(result.url)
            is UploadResult.Error -> DomainResult.Error(result.error)
            else -> DomainResult.Error(UploadError.Unknown("Upload failed without explicit terminal result"))
        }
    }
}
