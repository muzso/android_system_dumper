package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.UploadError

interface UploadExecutor {
    suspend fun executeUpload(
        repository: UploadRepository,
        fileName: String,
        filePath: String,
        onProgress: suspend (written: Long, total: Long) -> Unit
    ): DomainResult<String, UploadError>
}
