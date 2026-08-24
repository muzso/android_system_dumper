package hu.muzso.android_system_dumper.platform

import android.os.Build
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidSystemInfoTest {

    private val systemInfo = AndroidSystemInfo()

    @Test
    fun `getSdkVersion returns current SDK version`() {
        assertThat(systemInfo.getSdkVersion()).isEqualTo(Build.VERSION.SDK_INT)
    }

    @Test
    fun `getSystemProperties returns properties when process succeeds`() {
        mockkConstructor(ProcessBuilder::class)
        val mockProcess = mockk<Process>()
        val mockInputStream = "prop1=val1\nprop2=val2".byteInputStream()

        every { anyConstructed<ProcessBuilder>().start() } returns mockProcess
        every { mockProcess.inputStream } returns mockInputStream

        val properties = systemInfo.getSystemProperties()

        assertThat(properties).contains("prop1=val1")
        assertThat(properties).contains("prop2=val2")

        unmockkConstructor(ProcessBuilder::class)
    }

    @Test
    fun `getSystemProperties returns error message when process fails`() {
        mockkConstructor(ProcessBuilder::class)
        every { anyConstructed<ProcessBuilder>().start() } throws RuntimeException("Process failed")

        val properties = systemInfo.getSystemProperties()

        assertThat(properties).contains("Failed to get system properties: Process failed")

        unmockkConstructor(ProcessBuilder::class)
    }
}
