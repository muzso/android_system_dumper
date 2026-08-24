package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.zip.ZipCreator

class CreateZipUseCase(
    private val zipCreator: ZipCreator
) {
    suspend fun execute(
        files: List<ZipFileEntry>,
        options: ZipOptions,
        readIntoMemory: Boolean
    ): DomainResult<String, ZipError> = zipCreator.create(files, options, readIntoMemory)
}
