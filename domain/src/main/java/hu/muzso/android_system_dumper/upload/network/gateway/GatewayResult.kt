package hu.muzso.android_system_dumper.upload.network.gateway

sealed class GatewayResult<out T> {
    data class Progress(val bytesWritten: Long, val totalBytes: Long) : GatewayResult<Nothing>()
    data class Success<out T>(val data: T) : GatewayResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : GatewayResult<Nothing>()
}
