package hu.muzso.android_system_dumper.repository

import hu.muzso.android_system_dumper.model.ZipEncryption

interface SettingsRepository {
    fun getZipEncryption(): ZipEncryption
    fun setZipEncryption(value: ZipEncryption)

    fun getSelectedUploadServiceId(): String
    fun setSelectedUploadServiceId(id: String)
}
