package hu.muzso.android_system_dumper.platform

import android.content.Context
import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.DefaultPlatformUtils
import hu.muzso.android_system_dumper.common.DefaultRandomProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidSystemInfoTest {

    private lateinit var context: Context
    private val logger = FakeFileLogger()
    private val platformUtils = DefaultPlatformUtils(DefaultRandomProvider())
    private lateinit var systemInfo: AndroidSystemInfo

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        systemInfo = AndroidSystemInfo(context, logger, platformUtils)
    }

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

    @Test
    fun `getPlatformInfo returns formatted platform, hardware, build, build version, and system features`() {
        val info = systemInfo.getPlatformInfo()

        assertThat(info).contains("Platform: Android (standard)")
        assertThat(info).contains("Hardware:\n\n- Memory size:")
        assertThat(info).contains("- CPU core count:")
        assertThat(info).contains("- Screen resolution:")
        assertThat(info).contains("- Display density:")

        assertThat(info).contains("Build:\n\n- BOARD:")
        assertThat(info).contains("- BRAND:")
        assertThat(info).contains("- DEVICE:")
        assertThat(info).contains("- FINGERPRINT:")
        assertThat(info).contains("- MODEL:")
        assertThat(info).contains("- TIME:")
        assertThat(info).contains(" GMT)")

        assertThat(info).contains("Build.VERSION:\n\n- BASE_OS:")
        assertThat(info).contains("- RELEASE:")
        assertThat(info).contains("- SDK_INT:")

        assertThat(info).contains("System Features:")

        // Check exclusions
        assertThat(info).doesNotContain("- HOST:")
        assertThat(info).doesNotContain("- USER:")
        assertThat(info).doesNotContain("- PREVIEW_SDK_INT:")
    }

    @Test
    fun `getPlatformInfo detects automotive platform when feature is present`() {
        val shadowPm = Shadows.shadowOf(context.packageManager)
        shadowPm.setSystemFeature(PackageManager.FEATURE_AUTOMOTIVE, true)
        shadowPm.addSystemAvailableFeature(FeatureInfo().apply { name = PackageManager.FEATURE_AUTOMOTIVE })

        val info = systemInfo.getPlatformInfo()

        assertThat(info).contains("Platform: Android Automotive OS")
        assertThat(info).contains("- ${PackageManager.FEATURE_AUTOMOTIVE}")
    }

    @Test
    fun `getPlatformInfo handles exceptions gracefully`() {
        val mockContext = mockk<Context>()
        every { mockContext.packageManager } throws RuntimeException("PM failed")
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } throws RuntimeException("AM failed")
        every { mockContext.resources } throws RuntimeException("Resources failed")

        val errorSystemInfo = AndroidSystemInfo(mockContext, logger, platformUtils)
        val info = errorSystemInfo.getPlatformInfo()

        assertThat(info).contains("Platform: Android (standard)")
        assertThat(info).contains("Hardware:")
        assertThat(info).contains("Build:")
    }
}
