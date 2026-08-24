package hu.muzso.android_system_dumper.model.upload

import hu.muzso.android_system_dumper.model.UploadError

sealed class UploadWorkflowStatus {
    object Preparing : UploadWorkflowStatus()
    object PartitioningBatches : UploadWorkflowStatus()
    data class ArchivingBatch(val current: Int, val total: Int) : UploadWorkflowStatus()
    data class UploadingBatch(val label: String, val attempt: Int, val totalRetries: Int) : UploadWorkflowStatus()
    object CreatingReadableList : UploadWorkflowStatus()
    object CreatingUnreadableList : UploadWorkflowStatus()
    object CreatingExcludedList : UploadWorkflowStatus()
    object CreatingMissingList : UploadWorkflowStatus()
    object CreatingSymlinkList : UploadWorkflowStatus()
    data class ExecutingCommand(val command: String) : UploadWorkflowStatus()
    data class TotalPlannedUploads(val count: Int) : UploadWorkflowStatus()
    data class SuccessfulUploads(val count: Int) : UploadWorkflowStatus()
    data class Progress(val currentZipBytes: Long, val totalZipBytes: Long) : UploadWorkflowStatus()
    data class Success(
        val downloadUrl: String,
        val uploadedZips: Int,
        val totalZips: Int,
        val totalBytes: Long,
        val runtimeSeconds: Long,
        val password: String? = null
    ) : UploadWorkflowStatus()
    data class PartialSuccess(
        val downloadUrl: String,
        val uploadedZips: Int,
        val totalZips: Int,
        val totalBytes: Long,
        val runtimeSeconds: Long,
        val failedZips: Int,
        val password: String? = null
    ) : UploadWorkflowStatus()
    data class Error(val error: UploadError, val totalBytes: Long, val runtimeSeconds: Long) : UploadWorkflowStatus()
    object Aborted : UploadWorkflowStatus()
}
