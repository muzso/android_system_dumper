package hu.muzso.android_system_dumper.platform

interface TorServiceController {
    suspend fun rebuildCircuit()
    suspend fun waitForCircuit(timeoutMs: Long): Boolean
    suspend fun restartTorService(timeoutMs: Long): Boolean
}
