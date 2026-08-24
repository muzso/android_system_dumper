package hu.muzso.android_system_dumper.logging

import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.di.LogFilePath
import hu.muzso.android_system_dumper.di.LogToSystem
import hu.muzso.android_system_dumper.filesystem.FileSystem
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidFileLogger @Inject constructor(
    @LogFilePath private val filePath: String,
    @LogToSystem private val logToSystem: Boolean,
    private val systemLogSink: SystemLogSink,
    private val clock: Clock,
    private val fileSystem: FileSystem
) : FileLogger {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * Initializes the logger by ensuring any existing log file at [filePath] 
     * is deleted to start a fresh log session.
     */
    init {
        runBlocking {
            try {
                if (fileSystem.exists(filePath)) {
                    fileSystem.delete(filePath)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun v(tag: String?, msg: String): Int {
        if (logToSystem) systemLogSink.v(tag, msg)
        logToFile("V", tag, msg)
        return 0
    }

    override fun d(tag: String?, msg: String): Int {
        if (logToSystem) systemLogSink.d(tag, msg)
        logToFile("D", tag, msg)
        return 0
    }

    override fun i(tag: String?, msg: String): Int {
        if (logToSystem) systemLogSink.i(tag, msg)
        logToFile("I", tag, msg)
        return 0
    }

    override fun w(tag: String?, msg: String): Int {
        if (logToSystem) systemLogSink.w(tag, msg)
        logToFile("W", tag, msg)
        return 0
    }

    override fun e(tag: String?, msg: String): Int {
        if (logToSystem) systemLogSink.e(tag, msg)
        logToFile("E", tag, msg)
        return 0
    }

    override fun v(tag: String?, msg: String?, tr: Throwable?): Int {
        if (logToSystem) systemLogSink.v(tag, msg, tr)
        logToFile("V", tag, "$msg\n${systemLogSink.getStackTraceString(tr)}")
        return 0
    }

    override fun d(tag: String?, msg: String?, tr: Throwable?): Int {
        if (logToSystem) systemLogSink.d(tag, msg, tr)
        logToFile("D", tag, "$msg\n${systemLogSink.getStackTraceString(tr)}")
        return 0
    }

    override fun i(tag: String?, msg: String?, tr: Throwable?): Int {
        if (logToSystem) systemLogSink.i(tag, msg, tr)
        logToFile("I", tag, "$msg\n${systemLogSink.getStackTraceString(tr)}")
        return 0
    }

    override fun w(tag: String?, msg: String?, tr: Throwable?): Int {
        if (logToSystem) systemLogSink.w(tag, msg, tr)
        logToFile("W", tag, "$msg\n${systemLogSink.getStackTraceString(tr)}")
        return 0
    }

    override fun e(tag: String?, msg: String?, tr: Throwable?): Int {
        if (logToSystem) systemLogSink.e(tag, msg, tr)
        logToFile("E", tag, "$msg\n${systemLogSink.getStackTraceString(tr)}")
        return 0
    }

    override fun flush() {
        // No-op for now, as we write synchronously
    }

    override fun getLogFilePath(): String = filePath

    /**
     * Logs the contents of a directory to the log file.
     * 
     * @param path The path to the directory to log.
     */
    override fun logDirectoryContents(path: String) {
        runBlocking {
            if (!fileSystem.exists(path)) {
                e("Logger", "logDirectoryContents(): missing: ${fileSystem.getCanonicalPath(path)}")
                return@runBlocking
            }
            v("Logger", "logDirectoryContents(): starting to recurse into \"${fileSystem.getCanonicalPath(path)}\" ...")
            logPathContents(path)
            v("Logger", "logDirectoryContents(): finished")
        }
    }

    /**
     * Logs the details of a specific path (file or directory) to the log file.
     * 
     * @param path The path to log details for.
     */
    private suspend fun logPathContents(path: String) {
        val lastModified = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(fileSystem.lastModified(path)))
        if (!fileSystem.isDirectory(path)) {
            v("Logger", "logPathContents(): file: ${fileSystem.getCanonicalPath(path)} (${fileSystem.size(path)}, $lastModified)")
            return
        }
        v("Logger", "logPathContents(): directory: ${fileSystem.getCanonicalPath(path)} ($lastModified)")
        val entries = fileSystem.list(path)
        for (entry in entries) {
            logPathContents(fileSystem.join(path, entry.name))
        }
    }

    /**
     * Writes a log message to the log file.
     * 
     * This method formats the log entry with a timestamp and severity level,
     * and appends it to the file at [filePath].
     *
     * @param level The log level string (e.g., "V", "D", "I").
     * @param tag The tag for the log message.
     * @param msg The message to log.
     */
    private fun logToFile(level: String, tag: String?, msg: String) {
        try {
            val timestamp = dateFormat.format(Date.from(clock.now()))
            runBlocking {
                fileSystem.appendText(filePath, "$timestamp $level/$tag: $msg\n")
            }
        } catch (_: Exception) {
        }
    }
}
