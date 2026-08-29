package hu.muzso.android_system_dumper.network.upload

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUploadProgressTracker @Inject constructor(
    private val uploadSelector: UploadSelector
) : UploadProgressTracker {
    override val totalUploadedBytes: StateFlow<Long>
        get() = uploadSelector.getSelectedRepository().totalUploadedBytes

    override fun incrementTotalUploadedBytes(bytes: Long) {
        uploadSelector.getSelectedRepository().incrementTotalUploadedBytes(bytes)
    }

    override suspend fun reset() {
        uploadSelector.getSelectedRepository().reset()
    }
}
