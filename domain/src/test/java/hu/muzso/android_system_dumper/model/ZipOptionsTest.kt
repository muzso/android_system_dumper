package hu.muzso.android_system_dumper.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ZipOptionsTest {

    @Test
    fun `equals returns true for same values`() {
        val options1 = ZipOptions(
            outputFilePath = "/path/to/zip",
            encryptionMethod = ZipEncryption.AES,
            password = "password".toCharArray(),
            compressionMethod = CompressionMethod.DEFLATE,
            compressionLevel = CompressionLevel.NORMAL
        )
        val options2 = ZipOptions(
            outputFilePath = "/path/to/zip",
            encryptionMethod = ZipEncryption.AES,
            password = "password".toCharArray(),
            compressionMethod = CompressionMethod.DEFLATE,
            compressionLevel = CompressionLevel.NORMAL
        )

        assertThat(options1).isEqualTo(options2)
        assertThat(options1.hashCode()).isEqualTo(options2.hashCode())
    }

    @Test
    fun `equals returns false for different passwords`() {
        val options1 = ZipOptions(
            outputFilePath = "a",
            encryptionMethod = ZipEncryption.AES,
            password = "p1".toCharArray()
        )
        val options2 = ZipOptions(
            outputFilePath = "a",
            encryptionMethod = ZipEncryption.AES,
            password = "p2".toCharArray()
        )

        assertThat(options1).isNotEqualTo(options2)
    }

    @Test
    fun `equals returns false for different encryption`() {
        val options1 = ZipOptions(outputFilePath = "a", encryptionMethod = ZipEncryption.AES)
        val options2 = ZipOptions(outputFilePath = "a", encryptionMethod = ZipEncryption.NONE)

        assertThat(options1).isNotEqualTo(options2)
    }

    @Test
    fun `equals returns true for same reference`() {
        val options = ZipOptions(outputFilePath = "a", encryptionMethod = ZipEncryption.NONE)
        assertThat(options).isEqualTo(options)
    }

    @Test
    fun `equals returns false for null or different class`() {
        val options = ZipOptions(outputFilePath = "a", encryptionMethod = ZipEncryption.NONE)
        assertThat(options).isNotEqualTo(null)
        assertThat(options).isNotEqualTo("not an option")
    }
}
