package hu.muzso.android_system_dumper.platform

interface AppServiceManager {
    fun startTorService(action: String? = null)
    fun stopTorService()
}
