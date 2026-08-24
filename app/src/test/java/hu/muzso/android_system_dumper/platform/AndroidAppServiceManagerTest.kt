package hu.muzso.android_system_dumper.platform

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidAppServiceManagerTest {
    private lateinit var context: Context
    private lateinit var manager: AndroidAppServiceManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = AndroidAppServiceManager(context)
    }

    @Test
    fun `startTorService starts CustomTorService with correct action`() {
        manager.startTorService("test_action")
        val nextStartedService = shadowOf(context as Application).nextStartedService
        assertThat(nextStartedService?.action).isEqualTo("test_action")
        assertThat(nextStartedService?.component?.className).contains("CustomTorService")
    }

    @Test
    fun `stopTorService stops CustomTorService`() {
        manager.stopTorService()
        val nextStoppedService = shadowOf(context as Application).nextStoppedService
        assertThat(nextStoppedService?.action).isEqualTo("org.torproject.jni.TorServiceController.ACTION_STOP")
    }
}
