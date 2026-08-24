package hu.muzso.android_system_dumper.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.model.ZipEncryption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override fun getZipEncryption(): ZipEncryption {
        val name = prefs.getString("zip_encryption", ZipEncryption.STANDARD.name)
        return try {
            ZipEncryption.valueOf(name ?: ZipEncryption.STANDARD.name)
        } catch (_: Exception) {
            ZipEncryption.STANDARD
        }
    }

    override fun setZipEncryption(value: ZipEncryption) {
        prefs.edit { putString("zip_encryption", value.name) }
    }

    override fun getSelectedUploadServiceId(): String {
        return prefs.getString("selected_upload_service", "gofile.io") ?: "gofile.io"
    }

    override fun setSelectedUploadServiceId(id: String) {
        prefs.edit { putString("selected_upload_service", id) }
    }
}