package hu.muzso.android_system_dumper.model

enum class CompressionMethod { STORE, DEFLATE }

enum class CompressionLevel { NO_COMPRESSION, FASTEST, FAST, NORMAL, MAXIMUM, ULTRA }

enum class ZipEncryption { NONE, STANDARD, AES }

data class ZipOptions(
    val outputFilePath: String,
    val encryptionMethod: ZipEncryption,
    val password: CharArray? = null,
    val compressionMethod: CompressionMethod = CompressionMethod.DEFLATE,
    val compressionLevel: CompressionLevel = CompressionLevel.FASTEST
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZipOptions

        if (outputFilePath != other.outputFilePath) return false
        if (encryptionMethod != other.encryptionMethod) return false
        if (!password.contentEquals(other.password)) return false
        if (compressionMethod != other.compressionMethod) return false
        if (compressionLevel != other.compressionLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = outputFilePath.hashCode()
        result = 31 * result + encryptionMethod.hashCode()
        result = 31 * result + (password?.contentHashCode() ?: 0)
        result = 31 * result + compressionMethod.hashCode()
        result = 31 * result + compressionLevel.hashCode()
        return result
    }
}
