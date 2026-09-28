package hu.muzso.android_system_dumper.platform

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.logging.FileLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSystemInfo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: FileLogger,
    private val platformUtils: PlatformUtils
) : SystemInfo {
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

    override fun getPlatformInfo(): String {
        val sb = StringBuilder()

        // 1. Platform Section
        val platform = try {
            val pm = context.packageManager
            when {
                pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) -> "Android Automotive OS"
                pm.hasSystemFeature(PackageManager.FEATURE_WATCH) -> "Wear OS"
                pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                        pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION) -> "Android TV"
                else -> "Android (standard)"
            }
        } catch (e: Exception) {
            logger.e("AndroidSystemInfo", "Failed to determine platform", e)
            "Android (standard)"
        }
        sb.appendLine("Platform: $platform")
        sb.appendLine()

        // 2. Hardware Section
        sb.appendLine("Hardware:")
        sb.appendLine()
        appendField(sb, "Memory size") {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            if (am != null) {
                am.getMemoryInfo(memInfo)
                platformUtils.formatBytes(memInfo.totalMem)
            } else null
        }
        appendField(sb, "CPU core count") {
            Runtime.getRuntime().availableProcessors()
        }
        appendField(sb, "Screen resolution") {
            val metrics = context.resources.displayMetrics
            "${metrics.widthPixels}x${metrics.heightPixels}"
        }
        appendField(sb, "Display density") {
            val metrics = context.resources.displayMetrics
            "${metrics.densityDpi} dpi"
        }
        sb.appendLine()

        // 3. Build Section
        sb.appendLine("Build:")
        sb.appendLine()
        appendField(sb, "BOARD") { Build.BOARD }
        appendField(sb, "BOOTLOADER") { Build.BOOTLOADER }
        appendField(sb, "BRAND") { Build.BRAND }
        appendField(sb, "DEVICE") { Build.DEVICE }
        appendField(sb, "DISPLAY") { Build.DISPLAY }
        appendField(sb, "FINGERPRINT") { Build.FINGERPRINT }
        appendField(sb, "HARDWARE") { Build.HARDWARE }
        appendField(sb, "ID") { Build.ID }
        appendField(sb, "MANUFACTURER") { Build.MANUFACTURER }
        appendField(sb, "MODEL") { Build.MODEL }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appendField(sb, "ODM_SKU") { Build.ODM_SKU }
        }
        appendField(sb, "PRODUCT") { Build.PRODUCT }
        appendField(sb, "RADIO") { Build.RADIO }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appendField(sb, "SKU") { Build.SKU }
            appendField(sb, "SOC_MANUFACTURER") { Build.SOC_MANUFACTURER }
            appendField(sb, "SOC_MODEL") { Build.SOC_MODEL }
        }
        if (Build.VERSION.SDK_INT >= 36) {
            appendField(sb, "STRONGBOX_MANUFACTURER") {
                try {
                    val field = Build::class.java.getField("STRONGBOX_MANUFACTURER")
                    field.get(null)
                } catch (_: Exception) { null }
            }
            appendField(sb, "STRONGBOX_MODEL") {
                try {
                    val field = Build::class.java.getField("STRONGBOX_MODEL")
                    field.get(null)
                } catch (_: Exception) { null }
            }
        }
        appendField(sb, "SUPPORTED_ABIS") { Build.SUPPORTED_ABIS }
        appendField(sb, "SUPPORTED_32_BIT_ABIS") { Build.SUPPORTED_32_BIT_ABIS }
        appendField(sb, "SUPPORTED_64_BIT_ABIS") { Build.SUPPORTED_64_BIT_ABIS }
        appendField(sb, "TAGS") { Build.TAGS }
        appendField(sb, "TIME") {
            val timeMs = Build.TIME
            val gmtStr = try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT")
                }
                sdf.format(Date(timeMs))
            } catch (e: Exception) {
                logger.e("AndroidSystemInfo", "Failed to format Build.TIME", e)
                null
            }
            if (gmtStr != null) "$timeMs ($gmtStr)" else "$timeMs"
        }
        appendField(sb, "TYPE") { Build.TYPE }
        sb.appendLine()

        // 4. Build.VERSION Section
        sb.appendLine("Build.VERSION:")
        sb.appendLine()
        appendField(sb, "BASE_OS") { Build.VERSION.BASE_OS }
        appendField(sb, "CODENAME") { Build.VERSION.CODENAME }
        appendField(sb, "INCREMENTAL") { Build.VERSION.INCREMENTAL }
        appendField(sb, "RELEASE") { Build.VERSION.RELEASE }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appendField(sb, "RELEASE_OR_CODENAME") { Build.VERSION.RELEASE_OR_CODENAME }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appendField(sb, "RELEASE_OR_PREVIEW_DISPLAY") { Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY }
        }
        appendField(sb, "SDK_INT") { Build.VERSION.SDK_INT }
        if (Build.VERSION.SDK_INT >= 35) {
            appendField(sb, "SDK_INT_FULL") {
                try {
                    val field = Build.VERSION::class.java.getField("SDK_INT_FULL")
                    field.get(null)
                } catch (_: Exception) { null }
            }
        }
        appendField(sb, "SECURITY_PATCH") { Build.VERSION.SECURITY_PATCH }
        sb.appendLine()

        // 5. System Features Section
        sb.appendLine("System Features:")
        sb.appendLine()
        try {
            val pm = context.packageManager
            val features = pm.systemAvailableFeatures
                ?.mapNotNull { it.name }
                ?.filter { it.isNotEmpty() }
                ?.sorted()
            if (!features.isNullOrEmpty()) {
                features.forEach { feature ->
                    sb.appendLine("- $feature")
                }
            }
        } catch (e: Exception) {
            logger.e("AndroidSystemInfo", "Failed to get system available features", e)
        }

        return sb.toString().trimEnd()
    }

    private fun appendField(sb: StringBuilder, label: String, supplier: () -> Any?) {
        try {
            val value = supplier()
            if (value != null) {
                val formatted = when (value) {
                    is Array<*> -> value.filterNotNull().joinToString(", ")
                    else -> value.toString()
                }
                sb.appendLine("- $label: $formatted")
            }
        } catch (e: Exception) {
            logger.e("AndroidSystemInfo", "Failed to read property $label", e)
        }
    }
}
