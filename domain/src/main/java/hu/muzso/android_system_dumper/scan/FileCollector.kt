package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanResult

interface FileCollector {
    fun addReadableFile(file: FileEntry)
    fun addUnreadableFile(path: String)
    fun addExcludedFile(path: String)
    fun addMissingFile(path: String)
    fun addSymlink(path: String, target: String)
    fun getCollectedResult(): ScanResult
    fun clear()
}
