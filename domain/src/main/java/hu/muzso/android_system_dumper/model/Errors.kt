package hu.muzso.android_system_dumper.model

sealed interface ScanError {
    data class PermissionDenied(val path: String) : ScanError
    data class IOException(val message: String, val cause: Throwable? = null) : ScanError
    data class Unknown(val message: String, val cause: Throwable? = null) : ScanError
}

sealed interface ZipError {
    data class Zip4jError(val message: String, val cause: Throwable? = null) : ZipError
    data class IOException(val message: String, val cause: Throwable? = null) : ZipError
    data class InsufficientSpace(val requiredBytes: Long) : ZipError
}

sealed interface UploadError {
    data class NetworkError(val message: String, val cause: Throwable? = null) : UploadError
    data class ServerError(val code: Int, val message: String) : UploadError
    data class AuthenticationError(val message: String) : UploadError
    data class FileNotFoundError(val path: String) : UploadError
    data class Cancelled(val message: String) : UploadError
    data class ZeroSuccessfulUploads(val message: String) : UploadError
    data class MissingDownloadURL(val message: String) : UploadError
    data class InsufficientStorage(val requiredBytes: Long) : UploadError
    data class TorVerificationFailed(val message: String) : UploadError
    data class Unknown(val message: String, val cause: Throwable? = null) : UploadError
}

sealed class DomainResult<out T, out E> {
    data class Success<T>(val data: T) : DomainResult<T, Nothing>()
    data class Error<E>(val error: E) : DomainResult<Nothing, E>()
}
