package hu.muzso.android_system_dumper.platform

import android.graphics.Color
import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.logging.FileLogger
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZxingQrGeneratorTest {

    private val logger = mockk<FileLogger>(relaxed = true)
    private val qrGenerator = DefaultQrGenerator(logger)

    @Test
    fun `generateQrCode returns valid bitmap`() {
        val size = 100
        val bitmap = qrGenerator.generateQrCode("test", size)

        Truth.assertThat(bitmap).isNotNull()
        Truth.assertThat(bitmap?.width).isEqualTo(size)
        Truth.assertThat(bitmap?.height).isEqualTo(size)
        
        // QR codes typically have white background and black patterns.
        // Check at least one pixel is black and one is white.
        var hasBlack = false
        var hasWhite = false
        for (x in 0 until size) {
            for (y in 0 until size) {
                val pixel = bitmap!!.getPixel(x, y)
                if (pixel == Color.BLACK) hasBlack = true
                if (pixel == Color.WHITE) hasWhite = true
            }
        }
        Truth.assertThat(hasBlack).isTrue()
        Truth.assertThat(hasWhite).isTrue()
    }
}
