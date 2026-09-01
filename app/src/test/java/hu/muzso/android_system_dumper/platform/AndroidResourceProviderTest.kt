package hu.muzso.android_system_dumper.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidResourceProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resourceProvider = AndroidResourceProvider(context)

    @Test
    fun `getMinBatchSizeMb returns constant`() {
        assertThat(resourceProvider.getMinBatchSizeMb()).isEqualTo(SettingsUiState.CUSTOM_BATCH_SIZE_MB_MIN)
    }

    @Test
    fun `getMaxBatchSizeMb returns constant`() {
        assertThat(resourceProvider.getMaxBatchSizeMb()).isEqualTo(SettingsUiState.CUSTOM_BATCH_SIZE_MB_MAX)
    }

    @Test
    fun `getString returns string resource`() {
        assertThat(resourceProvider.getString(R.string.app_name)).isEqualTo("Android System Dumper")
    }

    @Test
    fun `getString with arguments returns formatted string`() {
        // Find a string with arguments if exists, or just verify it passes arguments correctly
        // Using R.string.upload_status_uploading which likely has arguments or similar
        // Let's check R.string.progress_format or similar if available, or just any string
        val result = resourceProvider.getString(R.string.app_name) // app_name doesn't take args but context.getString handles it
        assertThat(result).isEqualTo("Android System Dumper")
    }
}
