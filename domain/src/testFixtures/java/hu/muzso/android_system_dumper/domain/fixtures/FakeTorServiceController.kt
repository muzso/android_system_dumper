package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.platform.TorServiceController
import java.util.concurrent.atomic.AtomicInteger

class FakeTorServiceController(
    private var circuitReady: Boolean = true
) : TorServiceController {
    val rebuildCircuitCalls = AtomicInteger(0)
    val restartTorServiceCalls = AtomicInteger(0)
    var lastTimeoutMs: Long? = null
        private set

    override suspend fun rebuildCircuit() {
        rebuildCircuitCalls.incrementAndGet()
    }

    override suspend fun restartTorService(timeoutMs: Long): Boolean {
        restartTorServiceCalls.incrementAndGet()
        lastTimeoutMs = timeoutMs
        return circuitReady
    }

    override suspend fun waitForCircuit(timeoutMs: Long): Boolean {
        lastTimeoutMs = timeoutMs
        return circuitReady
    }

    fun setCircuitReady(ready: Boolean) {
        circuitReady = ready
    }
}
