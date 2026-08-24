package hu.muzso.android_system_dumper.upload.network

interface UploadSelector {
    fun getRepositories(): List<UploadRepository>
    fun getSelectedRepository(): UploadRepository
    fun selectRepository(id: String)
}
