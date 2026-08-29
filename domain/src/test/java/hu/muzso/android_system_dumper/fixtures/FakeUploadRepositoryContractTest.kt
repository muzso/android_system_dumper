package hu.muzso.android_system_dumper.fixtures

import hu.muzso.android_system_dumper.domain.fixtures.FakeUploadRepository
import hu.muzso.android_system_dumper.domain.repository.upload.UploadRepositoryContract
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.upload.UploadResult
import hu.muzso.android_system_dumper.network.upload.UploadRepository

class FakeUploadRepositoryContractTest : UploadRepositoryContract() {
    private val repository = FakeUploadRepository()

    override fun createRepository(): UploadRepository = repository

    override fun setupSuccessResponse(filePath: String, fileName: String) {
        repository.nextUploadResult = UploadResult.Success("https://dummy.url/$fileName")
    }

    override fun setupNetworkErrorResponse() {
        repository.nextUploadResult = UploadResult.Error(UploadError.NetworkError("Network error"))
    }

    override fun setupAuthErrorResponse() {
        repository.nextUploadResult = UploadResult.Error(UploadError.AuthenticationError("Auth error"))
    }
}
