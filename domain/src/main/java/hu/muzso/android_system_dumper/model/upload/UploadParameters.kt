package hu.muzso.android_system_dumper.model.upload

import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.upload.network.UploadRepository

data class UploadParameters(
    val customBatchSizeMb: String,
    val proxySpecification: String,
    val shouldUseTor: Boolean,
    val shouldUploadZips: Boolean,
    val shouldUploadReadableList: Boolean,
    val shouldUploadUnreadableList: Boolean,
    val shouldUploadExcludedList: Boolean,
    val shouldUploadMissingList: Boolean,
    val shouldUploadSymlinkList: Boolean,
    val shouldUploadGetprop: Boolean,
    val shouldUploadAppLogs: Boolean,
    val zipEncryption: ZipEncryption,
    val selectedService: UploadRepository,
    val maxBatches: Int
)
