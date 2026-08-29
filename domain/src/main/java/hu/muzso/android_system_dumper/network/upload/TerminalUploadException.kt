package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.model.UploadError

/**
 * Exception used to signal an immediate abort of the upload process.
 * 
 * Unlike standard exceptions encountered during upload, this exception signals that 
 * retries should be skipped entirely, usually due to a security violation or 
 * a confirmed terminal failure state.
 */
class TerminalUploadException(val error: UploadError) : Exception(
    when (error) {
        is UploadError.TorVerificationFailed -> error.message
        is UploadError.NetworkError -> error.message
        is UploadError.ServerError -> error.message
        is UploadError.AuthenticationError -> error.message
        is UploadError.FileNotFoundError -> "File not found: ${error.path}"
        is UploadError.Cancelled -> error.message
        is UploadError.ZeroSuccessfulUploads -> error.message
        is UploadError.MissingDownloadURL -> error.message
        is UploadError.InsufficientStorage -> "Insufficient storage: ${error.requiredBytes} bytes required"
        is UploadError.Unknown -> error.message
    }
)
