package hu.muzso.android_system_dumper.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.logging.FileLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class CustomTorServiceController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appServiceManager: AppServiceManager,
    private val logger: FileLogger,
) : TorServiceController {
    /**
     * Requests a rebuild of the Tor circuit.
     * 
     * This method sends a NEWNYM signal to the Tor service via [AppServiceManager].
     */
    override suspend fun rebuildCircuit() {
        try {
            appServiceManager.startTorService(CustomTorService.ACTION_NEWNYM)
        } catch (e: Exception) {
            logger.e("CustomTorServiceController", "Failed to send NEWNYM", e)
        }
    }

    /**
     * Waits for a Tor circuit to be established, up to a specified timeout.
     * 
     * This method registers a [BroadcastReceiver] to listen for the circuit established
     * event from the Tor service. It uses [suspendCancellableCoroutine] to bridge
     * the asynchronous broadcast to a suspending function.
     *
     * @param timeoutMs The maximum time to wait in milliseconds.
     * @return True if the circuit was established within the timeout, false otherwise.
     */
    override suspend fun waitForCircuit(timeoutMs: Long): Boolean {
        return withTimeoutOrNull(timeoutMs.milliseconds) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == CustomTorService.ACTION_CIRCUIT_ESTABLISHED) {
                            try {
                                context?.unregisterReceiver(this)
                            } catch (_: Exception) {
                            }
                            if (cont.isActive) cont.resume(value = true)
                        }
                    }
                }
                val filter = IntentFilter(CustomTorService.ACTION_CIRCUIT_ESTABLISHED)
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                cont.invokeOnCancellation {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: Exception) {
                    }
                }
            }
        } ?: false
    }
}
