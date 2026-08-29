package hu.muzso.android_system_dumper.network.upload

interface UploadRepositoryManager {
    fun getRepositories(): List<UploadRepository>
    fun getSelectedRepository(): UploadRepository
    fun selectRepository(id: String)
}
