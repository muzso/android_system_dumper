package hu.muzso.android_system_dumper.model

data class FileEntry(val path: String, val size: Long, val source: String)

data class ScanResult(
    val readableFiles: List<FileEntry> = emptyList(),
    val unreadableFiles: List<String> = emptyList(),
    val excludedFiles: List<String> = emptyList(),
    val missingFiles: List<String> = emptyList(),
    val symlinks: Map<String, String> = emptyMap()
)
