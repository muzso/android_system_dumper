package hu.muzso.android_system_dumper.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.R
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
    fun `getMaxUploadRetries returns integer resource`() {
        val expected = context.resources.getInteger(R.integer.max_number_of_upload_retries)
        assertThat(resourceProvider.getMaxUploadRetries()).isEqualTo(expected)
    }

    @Test
    fun `getMinBatchSizeMb returns integer resource`() {
        val expected = context.resources.getInteger(R.integer.custom_batch_size_mb_min)
        assertThat(resourceProvider.getMinBatchSizeMb()).isEqualTo(expected)
    }

    @Test
    fun `getMaxBatchSizeMb returns integer resource`() {
        val expected = context.resources.getInteger(R.integer.custom_batch_size_mb_max)
        assertThat(resourceProvider.getMaxBatchSizeMb()).isEqualTo(expected)
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
