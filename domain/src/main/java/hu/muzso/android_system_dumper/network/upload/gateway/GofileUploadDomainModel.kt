package hu.muzso.android_system_dumper.network.upload.gateway

data class GofileUploadDomainModel(
    val downloadPage: String,
    val guestToken: String?,
    val parentFolder: String?
)
