package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.scan.ArchiveRepository
import java.util.Date
import javax.inject.Inject

class CreateArchiveUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository,
    private val platformUtils: PlatformUtils
) {
    suspend fun execute(
        files: List<ZipFileEntry>,
        options: ZipOptions,
        readIntoMemory: Boolean
    ): DomainResult<String, ZipError> {
        return archiveRepository.createArchive(files, options, readIntoMemory)
    }

    fun generatePassphrase(length: Int): String {
        return platformUtils.generateSecureRandomString(length)
    }

    fun generateBatchFilename(date: Date, sequence: Int, digits: Int): String {
        return platformUtils.makeFilename(date, sequence, digits)
    }

    fun generateMiscZipFilename(date: Date): String {
        return "${platformUtils.formatDate2Filename(date)}_misc.zip"
    }
}
