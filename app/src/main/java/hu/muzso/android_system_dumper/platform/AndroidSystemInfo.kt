package hu.muzso.android_system_dumper.platform

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSystemInfo @Inject constructor() : SystemInfo {
    override fun getSdkVersion(): Int = Build.VERSION.SDK_INT

    /**
     * Retrieves system properties using the `getprop` command.
     * 
     * @return A string containing all system properties.
     */
    override fun getSystemProperties(): String {
        return try {
            val process = ProcessBuilder("getprop").start()
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Failed to get system properties: ${e.message}"
        }
    }
}
