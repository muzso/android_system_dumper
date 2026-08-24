package hu.muzso.android_system_dumper.logging

data class LogEvent(
    val level: String,
    val tag: String?,
    val message: String?,
    val throwable: Throwable? = null
)
