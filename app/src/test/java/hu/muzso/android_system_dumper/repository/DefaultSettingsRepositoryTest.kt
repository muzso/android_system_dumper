package hu.muzso.android_system_dumper.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.model.ZipEncryption
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultSettingsRepositoryTest {
    @Test
    fun `persistence works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var settings = DefaultSettingsRepository(context)

        settings.setSelectedUploadServiceId("filebin")
        settings.setZipEncryption(ZipEncryption.AES)

        // Recreate to test persistence
        settings = DefaultSettingsRepository(context)
        assertThat(settings.getSelectedUploadServiceId()).isEqualTo("filebin")
        assertThat(settings.getZipEncryption()).isEqualTo(ZipEncryption.AES)
    }

    @Test
    fun `defaults work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = DefaultSettingsRepository(context)
        assertThat(settings.getSelectedUploadServiceId()).isEqualTo("gofile.io")
        assertThat(settings.getZipEncryption()).isEqualTo(ZipEncryption.STANDARD)
    }
}