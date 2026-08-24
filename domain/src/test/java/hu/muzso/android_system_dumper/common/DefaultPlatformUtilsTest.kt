package hu.muzso.android_system_dumper.common

import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.random.Random

class DefaultPlatformUtilsTest {

    private val randomProvider = mockk<RandomProvider> {
        every { getRandom() } returns Random(42) // Seed for determinism
    }
    private val platformUtils = DefaultPlatformUtils(randomProvider)

    @Test
    fun `formatBytes formats correctly`() {
        Truth.assertThat(platformUtils.formatBytes(0)).isEqualTo("0 B")
        Truth.assertThat(platformUtils.formatBytes(512)).isEqualTo("512 B")
        Truth.assertThat(platformUtils.formatBytes(1024)).isEqualTo("1.00 KB")
        Truth.assertThat(platformUtils.formatBytes(1024 * 1024)).isEqualTo("1.00 MB")
        Truth.assertThat(platformUtils.formatBytes(1024 * 1024 * 1024)).isEqualTo("1.00 GB")
    }

    @Test
    fun `generateSecureRandomString length and characters`() {
        val length = 16
        val result = platformUtils.generateSecureRandomString(length)
        Truth.assertThat(result).hasLength(length)
        Truth.assertThat(result).matches("[abcdefghjkmnpqrstuvwxyz23456789]+")
    }

    @Test
    fun `makeBinName returns 8 char string`() {
        Truth.assertThat(platformUtils.makeBinName()).hasLength(8)
    }

    @Test
    fun `formatDate2Filename returns correct format`() {
        val date = GregorianCalendar(2024, Calendar.NOVEMBER, 29, 15, 34, 13).time
        Truth.assertThat(platformUtils.formatDate2Filename(date)).isEqualTo("2024-11-29_15-34-13")
    }

    @Test
    fun `makeFilename returns correct format`() {
        val date = GregorianCalendar(2024, Calendar.NOVEMBER, 29, 15, 34, 13).time
        Truth.assertThat(platformUtils.makeFilename(date, 1, 3)).isEqualTo("2024-11-29_15-34-13_001.zip")
    }
}