package hu.muzso.android_system_dumper.model

/**
 * Domain model representing IP information and its source.
 */
data class IpInfo(
    val sourceUrl: String,
    val data: Map<String, Any>
)
