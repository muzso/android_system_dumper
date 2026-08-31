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

    override suspend fun restartTorService(timeoutMs: Long): Boolean {
        logger.i("CustomTorServiceController", "Restarting Tor service...")
        
        val stopSuccessful = withTimeoutOrNull(timeoutMs.milliseconds) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == CustomTorService.ACTION_SERVICE_STOPPED) {
                            try {
                                context?.unregisterReceiver(this)
                            } catch (e: Exception) {
                                logger.w("CustomTorServiceController", "Failed to unregister stop receiver", e)
                            }
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                }
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(CustomTorService.ACTION_SERVICE_STOPPED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                
                appServiceManager.stopTorService()
                
                cont.invokeOnCancellation {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (e: Exception) {
                        logger.w("CustomTorServiceController", "Failed to unregister stop receiver on cancellation", e)
                    }
                }
            }
        } ?: false

        if (!stopSuccessful) {
            logger.w("CustomTorServiceController", "Timed out waiting for Tor service to stop")
            // Try to start it anyway
        }

        appServiceManager.startTorService()
        return waitForCircuit(timeoutMs)
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
                            } catch (e: Exception) {
                                logger.w("CustomTorServiceController", "Failed to unregister circuit receiver", e)
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
                    } catch (e: Exception) {
                        logger.w("CustomTorServiceController", "Failed to unregister circuit receiver on cancellation", e)
                    }
                }
            }
        } ?: false
    }
}
