package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions

interface ArchiveRepository {
    suspend fun createArchive(files: List<ZipFileEntry>, options: ZipOptions, readIntoMemory: Boolean): DomainResult<String, ZipError>
    suspend fun cleanupArchives(paths: List<String>)
}
