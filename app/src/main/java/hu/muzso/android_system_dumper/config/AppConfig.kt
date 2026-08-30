package hu.muzso.android_system_dumper.config

/**
 * Interface for application configuration, wrapping BuildConfig fields to allow mocking in tests.
 */
interface AppConfig {
    val httpServerIpAddress: String
    val httpServerTcpPort: Int
    val batchLimit: Int
    val fileCountLimit: Int
    val logToSystem: Boolean
}
