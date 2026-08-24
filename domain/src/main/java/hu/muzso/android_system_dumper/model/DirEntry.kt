package hu.muzso.android_system_dumper.model

data class DirEntry(
    val name: String,
    val type: Int
) {
    companion object {
        const val TYPE_DIR = 4
        const val TYPE_FILE = 8
        const val TYPE_LINK = 10

        fun decodeType(type: Int): String = when (type) {
            TYPE_DIR -> "DIR"
            TYPE_FILE -> "FILE"
            TYPE_LINK -> "LINK"
            else -> "UNKNOWN($type)"
        }
    }
}
