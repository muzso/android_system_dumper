package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.zip.ZipCreator

class FakeZipCreator(var fileSystem: FileSystem) : ZipCreator {
    var lastFiles: List<ZipFileEntry>? = null
    var lastOptions: ZipOptions? = null
    var createCalledCount = 0

    override suspend fun create(files: List<ZipFileEntry>, options: ZipOptions, readIntoMemory: Boolean): DomainResult<String, ZipError> {
        lastFiles = files
        lastOptions = options
        createCalledCount++

        if (!fileSystem.exists(options.outputFilePath)) {
            fileSystem.writeText(options.outputFilePath, "")
        }
        return DomainResult.Success(options.outputFilePath)
    }
}
