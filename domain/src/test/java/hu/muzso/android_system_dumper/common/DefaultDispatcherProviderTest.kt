package hu.muzso.android_system_dumper.common

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import org.junit.Test

class DefaultDispatcherProviderTest {
    @Test
    fun `DefaultDispatcherProvider returns standard dispatchers`() {
        val provider = DefaultDispatcherProvider()
        Truth.assertThat(provider.main()).isEqualTo(Dispatchers.Main)
        Truth.assertThat(provider.io()).isEqualTo(Dispatchers.IO)
        Truth.assertThat(provider.default()).isEqualTo(Dispatchers.Default)
        Truth.assertThat(provider.unconfined()).isEqualTo(Dispatchers.Unconfined)
    }
}