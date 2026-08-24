package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFileCollector @Inject constructor() : FileCollector {
    private val readableFiles = mutableListOf<FileEntry>()
    private val unreadableFiles = mutableListOf<String>()
    private val excludedFiles = mutableListOf<String>()
    private val missingFiles = mutableListOf<String>()
    private val symlinks = mutableMapOf<String, String>()

    override fun addReadableFile(file: FileEntry) {
        readableFiles.add(file)
    }

    override fun addUnreadableFile(path: String) {
        if (!unreadableFiles.contains(path)) unreadableFiles.add(path)
    }

    override fun addExcludedFile(path: String) {
        if (!excludedFiles.contains(path)) excludedFiles.add(path)
    }

    override fun addMissingFile(path: String) {
        if (!missingFiles.contains(path)) missingFiles.add(path)
    }

    override fun addSymlink(path: String, target: String) {
        if (!symlinks.containsKey(path) && path != target) symlinks[path] = target
    }

    /**
     * Aggregates all collected file information into a single [ScanResult].
     *
     * This method creates defensive copies of the internal collections to ensure
     * the result is stable and disconnected from further collector updates.
     *
     * @return A [ScanResult] containing all discovered, unreadable, excluded, and missing files, as well as symlinks.
     */
    override fun getCollectedResult(): ScanResult {
        return ScanResult(
            readableFiles = ArrayList(readableFiles),
            unreadableFiles = ArrayList(unreadableFiles),
            excludedFiles = ArrayList(excludedFiles),
            missingFiles = ArrayList(missingFiles),
            symlinks = HashMap(symlinks)
        )
    }

    override fun clear() {
        readableFiles.clear()
        unreadableFiles.clear()
        excludedFiles.clear()
        missingFiles.clear()
        symlinks.clear()
    }
}