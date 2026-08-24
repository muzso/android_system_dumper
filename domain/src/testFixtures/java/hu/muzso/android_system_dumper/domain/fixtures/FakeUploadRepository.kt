package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.model.upload.UploadResult
import hu.muzso.android_system_dumper.upload.network.UploadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File

class FakeUploadRepository : UploadRepository {
    override val id: String = "dummy"
    override val name: String = "Dummy"

    private val _totalUploadedBytes = MutableStateFlow(0L)
    override val totalUploadedBytes: StateFlow<Long> = _totalUploadedBytes.asStateFlow()

    var nextUploadResult: UploadResult? = null
    var urlListUrl: String = "https://dummy.url/list"

    override fun incrementTotalUploadedBytes(bytes: Long) {
        _totalUploadedBytes.value += bytes
    }

    override fun upload(filePath: String, fileName: String): Flow<UploadResult> = flow {
        val total = File(filePath).length()
        emit(UploadResult.Progress(0, total))
        val result = nextUploadResult ?: UploadResult.Success("https://dummy.url/$fileName")
        if (result is UploadResult.Success) {
             urlListUrl = "https://dummy.url/list"
        }
        emit(result)
    }

    override suspend fun reset() {
        _totalUploadedBytes.value = 0L
        urlListUrl = ""
    }
    override suspend fun getUrlListUrl(): String = urlListUrl
    override suspend fun torCheck(): Boolean = true
    override suspend fun logConfiguration() {}
}
