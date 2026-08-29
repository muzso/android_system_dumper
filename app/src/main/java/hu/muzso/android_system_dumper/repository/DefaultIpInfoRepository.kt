package hu.muzso.android_system_dumper.repository

import com.squareup.moshi.Moshi
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.model.IpInfo
import hu.muzso.android_system_dumper.network.upload.HttpClientProvider
import hu.muzso.android_system_dumper.network.upload.TorChecker
import kotlinx.coroutines.withContext
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IpInfoRepository] that fetches IP information from external sources
 * and formats it according to specific rules.
 *
 * It iterates through a list of URLs until a successful response is received.
 * After a successful fetch, it also performs a Tor check and embeds the result.
 */
@Singleton
class DefaultIpInfoRepository @Inject constructor(
    private val httpClientProvider: HttpClientProvider,
    private val moshi: Moshi,
    private val torChecker: TorChecker,
    private val networkUtils: NetworkUtils,
    private val dispatcherProvider: DispatcherProvider
) : IpInfoRepository {

    private val availableSources = listOf("https://json.geoiplookup.io/", "https://ipwho.is/")

    override fun getAvailableSources(): List<String> = availableSources

    override suspend fun fetchIpInfo(sourceUrl: String?): Result<IpInfo> = withContext(dispatcherProvider.io()) {
        val client = httpClientProvider.getClient()
        val sourcesToTry = if (sourceUrl != null) listOf(sourceUrl) else availableSources
        val failures = mutableListOf<String>()

        for (url in sourcesToTry) {
            val failurePrefix = "- $url: "
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()

                        @Suppress("UNCHECKED_CAST")
                        val jsonMap = moshi.adapter(Map::class.java).fromJson(body) as? Map<String, Any>
                        if (isValid(jsonMap)) {
                            val isTor = try {
                                torChecker.check()
                            } catch (_: Exception) {
                                false
                            }
                            val mutableMap = jsonMap!!.toMutableMap()
                            mutableMap["is_tor_node"] = isTor
                            val preparedData = prepareData(mutableMap)
                            return@withContext Result.success(IpInfo(url, preparedData))
                        } else {
                            failures.add("${failurePrefix}HTTP response did not contain valid JSON")
                        }
                    } else {
                        val httpErrorMessage = response.message.ifBlank { networkUtils.httpErrorMessage(response.code) }
                        failures.add("${failurePrefix}response was HTTP ${response.code} $httpErrorMessage")
                    }
                }
            } catch (e: Exception) {
                failures.add("${failurePrefix}${e.message}")
            }
        }

        val errorMsg = "Failed to collect IP information:\n" +
            failures.joinToString("\n")
        Result.failure(Exception(errorMsg))
    }

    private fun isValid(map: Map<String, Any>?): Boolean {
        if (map == null) return false
        val ip = map["ip"] as? String
        val success = map["success"]
        return !ip.isNullOrEmpty() && (success == true || success == "true")
    }

    private fun prepareData(map: Map<String, Any>): Map<String, Any> {
        val preparedMap = mutableMapOf<String, Any>()
        val sortedKeys = map.keys.sortedBy { it.lowercase() }

        for (key in sortedKeys) {
            val value = map[key] ?: continue
            if (value is String && value.isEmpty()) continue

            val formattedKey = formatKey(key)
            val processedValue = when (value) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    prepareData(value as Map<String, Any>)
                }

                is Double if value % 1 == 0.0 -> {
                    value.toLong()
                }

                else -> {
                    value
                }
            }
            preparedMap[formattedKey] = processedValue
        }
        return preparedMap
    }

    private fun formatKey(key: String): String {
        val words = mutableListOf<String>()
        val currentWord = StringBuilder()

        for (i in key.indices) {
            val char = key[i]
            if (!char.isLetterOrDigit()) {
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
            } else if (i > 0 && char.isUpperCase() && (key[i - 1].isLowerCase() || key[i - 1].isDigit())) {
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
                currentWord.append(char)
            } else {
                currentWord.append(char)
            }
        }
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }

        return words.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
