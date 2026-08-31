package hu.muzso.android_system_dumper.config

import hu.muzso.android_system_dumper.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppConfig @Inject constructor() : AppConfig {
    override val networkTimeoutMs: Long = BuildConfig.NETWORK_TIMEOUT_MS
    override val httpServerIpAddress: String = BuildConfig.HTTP_SERVER_IP_ADDRESS
    override val httpServerTcpPort: Int = BuildConfig.HTTP_SERVER_TCP_PORT
    override val batchLimit: Int = BuildConfig.BATCH_LIMIT
    override val fileCountLimit: Int = BuildConfig.FILE_COUNT_LIMIT
    override val logToSystem: Boolean = BuildConfig.LOG_TO_SYSTEM
}
