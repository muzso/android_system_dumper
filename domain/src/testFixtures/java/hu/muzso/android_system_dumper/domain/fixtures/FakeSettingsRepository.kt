package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.repository.SettingsRepository

class FakeSettingsRepository(
    private var zipEncryption: ZipEncryption = ZipEncryption.NONE,
    private var uploadServiceId: String = "dummy"
) : SettingsRepository {
    override fun getZipEncryption(): ZipEncryption = zipEncryption

    override fun setZipEncryption(value: ZipEncryption) {
        zipEncryption = value
    }

    override fun getSelectedUploadServiceId(): String = uploadServiceId

    override fun setSelectedUploadServiceId(id: String) {
        uploadServiceId = id
    }
}
