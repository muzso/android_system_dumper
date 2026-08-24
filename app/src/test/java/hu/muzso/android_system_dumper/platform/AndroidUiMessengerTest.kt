package hu.muzso.android_system_dumper.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidUiMessengerTest {
    @Test
    fun `showShortToast shows toast`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val messenger = AndroidUiMessenger(context)
        messenger.showShortToast("Hello")
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Hello")
    }
}
