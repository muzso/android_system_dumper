package hu.muzso.android_system_dumper.platform

interface SystemInfo {
    fun getSdkVersion(): Int
    fun getSystemProperties(): String
}
