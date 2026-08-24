package hu.muzso.android_system_dumper.common

import com.google.common.truth.Truth
import org.junit.Test

class DefaultClockTest {
    @Test
    fun `DefaultClock returns current time`() {
        val clock = DefaultClock()
        val now = clock.now()
        Truth.assertThat(now).isNotNull()
        Truth.assertThat(clock.monotonicTime()).isGreaterThan(0L)
    }
}