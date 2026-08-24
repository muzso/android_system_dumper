package hu.muzso.android_system_dumper.zip

import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions

interface ZipCreator {
    suspend fun create(
        files: List<ZipFileEntry>,
        options: ZipOptions,
        readIntoMemory: Boolean
    ): DomainResult<String, ZipError>
}
