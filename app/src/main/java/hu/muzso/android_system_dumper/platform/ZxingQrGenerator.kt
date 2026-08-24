package hu.muzso.android_system_dumper.platform

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import hu.muzso.android_system_dumper.logging.FileLogger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DefaultQrGenerator"

@Singleton
class DefaultQrGenerator @Inject constructor(
    private val logger: FileLogger
) : QrGenerator {

    /**
     * Generates a QR code bitmap for the given text.
     *
     * This implementation uses ZXing to encode the text into a [com.google.zxing.common.BitMatrix]
     * and then converts it into a grayscale Android [android.graphics.Bitmap].
     *
     * @param text The string to be encoded into the QR code.
     * @param size The width and height of the generated QR code bitmap.
     * @return A [android.graphics.Bitmap] of the QR code, or null if generation fails.
     */
    override fun generateQrCode(text: String, size: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = createBitmap(width, height)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            logger.e(TAG, "Exception while generating QR code.", e)
            null
        }
    }
}