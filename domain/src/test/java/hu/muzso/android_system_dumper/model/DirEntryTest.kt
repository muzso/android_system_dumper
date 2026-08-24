package hu.muzso.android_system_dumper.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DirEntryTest {

    @Test
    fun `decodeType returns correct string for all types`() {
        assertThat(DirEntry.decodeType(4)).isEqualTo("DIR")
        assertThat(DirEntry.decodeType(8)).isEqualTo("FILE")
        assertThat(DirEntry.decodeType(10)).isEqualTo("LINK")
        assertThat(DirEntry.decodeType(0)).isEqualTo("UNKNOWN(0)")
        assertThat(DirEntry.decodeType(99)).isEqualTo("UNKNOWN(99)")
    }
}
