package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.logging.FileLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "UploadRetryPolicy"

@Singleton
class DefaultUploadRetryPolicy @Inject constructor(
    private val logger: FileLogger
) : UploadRetryPolicy {

    /**
     * Executes a suspending block of code with a retry mechanism.
     * 
     * This method will attempt to run the [block] up to [retries] times. If an attempt
     * fails with an exception (other than [CancellationException]), it will log the 
     * error, call the [onFailure] hook, wait for 1 second, and retry. It reports the 
     * current status via the [onStatusUpdate] callback.
     *
     * @param label A descriptive label for the operation being retried.
     * @param retries The maximum number of attempts allowed.
     * @param onStatusUpdate A callback to report the current attempt and total retries.
     * @param onFailure A callback to invoke when an attempt fails.
     * @param block The suspending block of code to execute.
     * @return The result of the successful execution of the block.
     * @throws Exception The last encountered exception if all retry attempts fail.
     */
    override suspend fun <T> withRetry(
        label: String,
        retries: Int,
        onStatusUpdate: suspend (label: String, attempt: Int, totalRetries: Int) -> Unit,
        onFailure: suspend (attempt: Int, ex: Exception) -> Unit,
        block: suspend () -> T
    ): T {
        val maxAttempts = if (retries < 1) Int.MAX_VALUE else retries
        var ex: Exception? = null
        for (attempt in 1..maxAttempts) {
            onStatusUpdate(label, attempt, retries)
            logger.d(TAG, "withRetry: attempt $attempt for $label")
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: TerminalUploadException) {
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Attempt $attempt of $maxAttempts failed for $label: ${e.message}", e)
                ex = e
                onFailure(attempt, e)
                if (attempt < maxAttempts) delay(1000.milliseconds)
            }
        }
        throw ex!!
    }
}
