package hu.muzso.android_system_dumper.model.upload

import hu.muzso.android_system_dumper.model.UploadError

sealed class UploadResult {
    data class Progress(val bytesWritten: Long, val totalBytes: Long) : UploadResult()
    data class Success(val url: String) : UploadResult()
    data class Error(val error: UploadError) : UploadResult()
}
