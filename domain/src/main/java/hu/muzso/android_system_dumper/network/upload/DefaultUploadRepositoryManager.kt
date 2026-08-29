package hu.muzso.android_system_dumper.network.upload

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUploadRepositoryManager @Inject constructor(
    private val selector: UploadSelector
) : UploadRepositoryManager {
    
    override fun getRepositories(): List<UploadRepository> = selector.getRepositories()
    
    override fun getSelectedRepository(): UploadRepository = selector.getSelectedRepository()
    
    override fun selectRepository(id: String) {
        selector.selectRepository(id)
    }
}
