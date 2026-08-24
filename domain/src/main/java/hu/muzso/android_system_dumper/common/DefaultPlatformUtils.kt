package hu.muzso.android_system_dumper.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.pow

@Singleton
class DefaultPlatformUtils @Inject constructor(
    private val randomProvider: RandomProvider
) : PlatformUtils {

    private val alphabetLowercaseAndDigits: CharArray = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray()

    /**
     * Formats a byte count into a human-readable string (e.g., KiB, MiB).
     *
     * @param bytes The byte count to format.
     * @return A formatted string representing the size.
     */
    override fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1] + "B"
        return String.format(Locale.US, "%.2f %s", bytes / 1024.0.pow(exp.toDouble()), unit)
    }

    /**
     * Generates a secure random string of a specified length.
     *
     * The string consists of lowercase letters and digits.
     *
     * @param length The length of the string to generate.
     * @return A secure random string.
     */
    override fun generateSecureRandomString(length: Int): String {
        if (length <= 0) return ""
        val resultBuffer = CharArray(length) {
            val randomIndex = randomProvider.getRandom().nextInt(alphabetLowercaseAndDigits.size)
            alphabetLowercaseAndDigits[randomIndex]
        }
        return String(resultBuffer)
    }

    override fun makeBinName(): String = generateSecureRandomString(8)

    override fun formatDate2Filename(date: Date): String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)

    /**
     * Generates a standardized filename for a ZIP batch.
     *
     * The filename includes the formatted date, a sequence number, and is padded
     * according to the specified [digits] to ensure proper sorting.
     *
     * @param date The base date for the filename.
     * @param sequence The sequence number of the batch.
     * @param digits The number of digits for padding the sequence number.
     * @return A formatted ZIP filename.
     */
    override fun makeFilename(date: Date, sequence: Int, digits: Int): String {
        val seqStr = String.format(Locale.US, "%0${digits}d", sequence)
        val dateStr = formatDate2Filename(date)
        return "${dateStr}_$seqStr.zip"
    }
}