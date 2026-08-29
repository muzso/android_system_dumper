package hu.muzso.android_system_dumper.network.upload.gateway

sealed class GatewayResult<out T> {
    data class Progress(val bytesWritten: Long, val totalBytes: Long) : GatewayResult<Nothing>()
    data class Success<out T>(val data: T) : GatewayResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : GatewayResult<Nothing>()
}
