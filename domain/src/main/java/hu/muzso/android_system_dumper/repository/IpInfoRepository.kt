package hu.muzso.android_system_dumper.repository

import hu.muzso.android_system_dumper.model.IpInfo

/**
 * Interface for fetching and formatting IP-related information.
 */
interface IpInfoRepository {
    /**
     * Returns a list of available IP information sources (URLs).
     */
    fun getAvailableSources(): List<String>

    /**
     * Fetches IP information from the specified source or uses the first available if none specified.
     * Validates the response and returns a domain model with source and formatted info.
     *
     * @param sourceUrl The URL to fetch from. If null, iterates through available sources.
     * @return A [Result] containing the [IpInfo] or an error.
     */
    suspend fun fetchIpInfo(sourceUrl: String? = null): Result<IpInfo>
}
