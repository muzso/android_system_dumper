package hu.muzso.android_system_dumper.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidSystemLogSinkTest {
    @Test
    fun `logs are delegated to ShadowLog`() {
        val sink = AndroidSystemLogSink()
        
        sink.v("Tag", "Verbose")
        sink.d("Tag", "Debug")
        sink.i("Tag", "Info")
        sink.w("Tag", "Warn")
        sink.e("Tag", "Error")

        val logs = ShadowLog.getLogsForTag("Tag")
        assertThat(logs).hasSize(5)
        assertThat(logs.map { it.msg }).containsExactly("Verbose", "Debug", "Info", "Warn", "Error")
    }

    @Test
    fun `logs with throwables are delegated to ShadowLog`() {
        val sink = AndroidSystemLogSink()
        val ex = RuntimeException("test")
        
        sink.v("Tag", "Verbose", ex)
        sink.d("Tag", "Debug", ex)
        sink.i("Tag", "Info", ex)
        sink.w("Tag", "Warn", ex)
        sink.e("Tag", "Error", ex)

        val logs = ShadowLog.getLogsForTag("Tag")
        assertThat(logs).hasSize(5)
        assertThat(logs.all { it.throwable == ex }).isTrue()
    }

    @Test
    fun `getStackTraceString delegates to Log`() {
        val sink = AndroidSystemLogSink()
        val ex = RuntimeException("test")
        // ShadowLog.getStackTraceString just returns empty or null by default in some versions? 
        // Actually Robolectric usually handles Log.getStackTraceString
        assertThat(sink.getStackTraceString(ex)).contains("java.lang.RuntimeException: test")
    }
}
