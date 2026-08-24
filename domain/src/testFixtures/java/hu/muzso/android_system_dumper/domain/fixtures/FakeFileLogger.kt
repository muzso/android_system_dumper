package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.logging.LogEvent

class FakeFileLogger : FileLogger {
    private val _events = mutableListOf<LogEvent>()
    val events: List<LogEvent> get() = _events

    val logs: List<String> get() = _events.map { event ->
        "[${event.level}] ${event.tag ?: ""}: ${event.message}" + (event.throwable?.let { "\n$it" } ?: "")
    }

    override fun v(tag: String?, msg: String): Int = log("V", tag, msg)
    override fun d(tag: String?, msg: String): Int = log("D", tag, msg)
    override fun i(tag: String?, msg: String): Int = log("I", tag, msg)
    override fun w(tag: String?, msg: String): Int = log("W", tag, msg)
    override fun e(tag: String?, msg: String): Int = log("E", tag, msg)

    override fun v(tag: String?, msg: String?, tr: Throwable?): Int = log("V", tag, msg, tr)
    override fun d(tag: String?, msg: String?, tr: Throwable?): Int = log("D", tag, msg, tr)
    override fun i(tag: String?, msg: String?, tr: Throwable?): Int = log("I", tag, msg, tr)
    override fun w(tag: String?, msg: String?, tr: Throwable?): Int = log("W", tag, msg, tr)
    override fun e(tag: String?, msg: String?, tr: Throwable?): Int = log("E", tag, msg, tr)

    private fun log(level: String, tag: String?, msg: String?, tr: Throwable? = null): Int {
        _events.add(LogEvent(level, tag, msg, tr))
        return 0
    }

    fun assertLogExists(level: String, tag: String? = null, messageContains: String) {
        val found = _events.any {
            it.level == level &&
                    (tag == null || it.tag == tag) &&
                    (it.message?.contains(messageContains) == true)
        }
        if (!found) {
            throw AssertionError("Expected log not found: level=$level, tag=$tag, messageContains='$messageContains'. Found logs:\n${logs.joinToString("\n")}")
        }
    }

    fun assertErrorLogExists(tag: String? = null, messageContains: String, throwableClass: Class<out Throwable>? = null) {
        val found = _events.any {
            it.level == "E" &&
                    (tag == null || it.tag == tag) &&
                    (it.message?.contains(messageContains) == true) &&
                    (throwableClass == null || throwableClass.isInstance(it.throwable))
        }
        if (!found) {
            throw AssertionError("Expected error log not found: tag=$tag, messageContains='$messageContains', throwableClass=$throwableClass. Found logs:\n${logs.joinToString("\n")}")
        }
    }

    fun assertWarningLogExists(tag: String? = null, messageContains: String) {
        assertLogExists("W", tag, messageContains)
    }

    override fun logDirectoryContents(path: String) {}

    override fun flush() {}
    override fun getLogFilePath(): String? = null
}
