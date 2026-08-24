package hu.muzso.android_system_dumper.upload.network

import hu.muzso.android_system_dumper.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUploadSelector @Inject constructor(
    private val repositories: Map<String, @JvmSuppressWildcards UploadRepository>,
    private val settingsRepository: SettingsRepository
) : UploadSelector {
    
    override fun getRepositories(): List<UploadRepository> = repositories.values.toList()
    
    override fun getSelectedRepository(): UploadRepository {
        val selectedId = settingsRepository.getSelectedUploadServiceId()
        return repositories[selectedId] ?: repositories.values.first()
    }
    
    override fun selectRepository(id: String) {
        settingsRepository.setSelectedUploadServiceId(id)
    }
}
