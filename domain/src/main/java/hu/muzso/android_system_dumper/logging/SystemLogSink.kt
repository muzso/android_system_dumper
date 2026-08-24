package hu.muzso.android_system_dumper.logging

interface SystemLogSink {
    fun v(tag: String?, msg: String): Int
    fun d(tag: String?, msg: String): Int
    fun i(tag: String?, msg: String): Int
    fun w(tag: String?, msg: String): Int
    fun e(tag: String?, msg: String): Int
    fun v(tag: String?, msg: String?, tr: Throwable?): Int
    fun d(tag: String?, msg: String?, tr: Throwable?): Int
    fun i(tag: String?, msg: String?, tr: Throwable?): Int
    fun w(tag: String?, msg: String?, tr: Throwable?): Int
    fun e(tag: String?, msg: String?, tr: Throwable?): Int
    fun getStackTraceString(tr: Throwable?): String
}