package hu.muzso.android_system_dumper.common

import com.google.common.truth.Truth
import org.junit.Test

class DefaultRandomProviderTest {
    @Test
    fun `DefaultRandomProvider returns random`() {
        val provider = DefaultRandomProvider()
        Truth.assertThat(provider.getRandom()).isNotNull()
    }
}