package hu.muzso.android_system_dumper.platform

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.platform.CustomTorService.Companion.ACTION_CIRCUIT_ESTABLISHED
import hu.muzso.android_system_dumper.platform.CustomTorService.Companion.ACTION_NEWNYM
import net.freehaven.tor.control.TorControlCommands
import org.torproject.jni.TorService
import javax.inject.Inject

@AndroidEntryPoint
class CustomTorService : TorService() {

    @Inject
    lateinit var logger: FileLogger

    @Inject
    lateinit var systemInfo: SystemInfo

    companion object {
        private const val TAG = "CustomTorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tor_service_channel"

        const val ACTION_NEWNYM = "hu.muzso.android_system_dumper.intent.action.NEWNYM"
        const val ACTION_CIRCUIT_ESTABLISHED = "hu.muzso.android_system_dumper.intent.action.CIRCUIT_ESTABLISHED"
    }

    @Volatile
    private var waitingForCircuit = false
    private var isListenerAdded = false

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
    }

    /**
     * Handles incoming intents, specifically looking for [ACTION_NEWNYM] to rebuild the circuit.
     * 
     * If [ACTION_NEWNYM] is received, it ensures a listener is attached to the Tor control
     * connection and sends a NEWNYM signal in a background thread. Otherwise, it ensures
     * the service is running in the foreground.
     *
     * @param intent The intent supplied to startService.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates how the system should continue the service.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_NEWNYM == intent.action) {
            ensureListenerAdded()
            val controlConnection = torControlConnection
            if (controlConnection != null) {
                Thread {
                    try {
                        logger.i(TAG, "Sending NEWNYM signal to Tor")
                        controlConnection.signal(TorControlCommands.SIGNAL_NEWNYM)
                        waitingForCircuit = true
                    } catch (e: Exception) {
                        logger.e(TAG, "Failed to send NEWNYM signal", e)
                    }
                }.start()
            } else {
                logger.w(TAG, "Tor control connection is not available")
            }
            return START_NOT_STICKY
        } else {
            startAsForeground()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Ensures that a listener is added to the Tor control connection to monitor circuit events.
     * 
     * This method adds a listener to the Tor control connection that listens for 
     * `CIRCUIT_ESTABLISHED` status events. When a new circuit is established following 
     * a NEWNYM request, it logs the event and sends a local broadcast [ACTION_CIRCUIT_ESTABLISHED].
     */
    private fun ensureListenerAdded() {
        if (isListenerAdded) return
        val conn = torControlConnection ?: return
        conn.addRawEventListener { keyword, data ->
            if (keyword == TorControlCommands.EVENT_STATUS_CLIENT && data?.contains("CIRCUIT_ESTABLISHED") == true) {
                if (waitingForCircuit) {
                    waitingForCircuit = false
                    logger.i(TAG, "New Tor circuit established after NEWNYM")
                    sendBroadcast(Intent(ACTION_CIRCUIT_ESTABLISHED).apply {
                        setPackage(packageName)
                    })
                }
            }
        }
        isListenerAdded = true
    }

    /**
     * Starts the service in the foreground with an appropriate notification.
     * 
     * On Android 10 (API 29) and above, it specifies the foreground service type
     * as [ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC].
     */
    @SuppressLint("NewApi")
    private fun startAsForeground() {
        val notification = createNotification()
        if (systemInfo.getSdkVersion() >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Creates the notification to be displayed while the Tor service is running in the foreground.
     * 
     * @return A [Notification] object configured for the Tor service.
     */
    private fun createNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tor_service_name))
            .setContentText(getString(R.string.tor_is_running))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Creates the notification channel for the Tor service if it doesn't already exist.
     * 
     * This is required for notifications on Android 8.0 (API 26) and above.
     */
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tor_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }
}
