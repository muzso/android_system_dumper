package hu.muzso.android_system_dumper.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AppServiceManager {
    override fun startTorService(action: String?) {
        val intent = Intent(context, CustomTorService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopTorService() {
        val intent = Intent(context, CustomTorService::class.java).apply {
            action = "org.torproject.jni.TorServiceController.ACTION_STOP"
        }
        context.stopService(intent)
    }
}
