package hu.muzso.android_system_dumper.network.upload

/**
 * Interface for checking if the current network traffic is routed through the Tor network.
 */
interface TorChecker {
    /**
     * Checks if the traffic is routed through the Tor network.
     *
     * @return True if the check confirms traffic is through Tor, false otherwise.
     * @throws Exception If the check fails due to network or other errors.
     */
    suspend fun check(): Boolean
}
