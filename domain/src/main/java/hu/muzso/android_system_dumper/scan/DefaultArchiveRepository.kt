package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.zip.ZipCreator
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class DefaultArchiveRepository @Inject constructor(
    private val zipCreator: ZipCreator,
    private val fileSystem: FileSystem
) : ArchiveRepository {
    /**
     * Creates a ZIP archive containing the specified files.
     *
     * This method delegates the archive creation to the [zipCreator]. If the creation
     * fails due to an "out of space" error, it calculates an improved estimation of
     * the required space and returns an [ZipError.InsufficientSpace] error.
     *
     * @param files The list of files to be included in the archive.
     * @param options The configuration options for the ZIP creation.
     * @return A [DomainResult] indicating success with the archive path, or failure.
     */
    override suspend fun createArchive(files: List<ZipFileEntry>, options: ZipOptions, readIntoMemory: Boolean): DomainResult<String, ZipError> {
        val result = zipCreator.create(files, options, readIntoMemory)
        if (result is DomainResult.Error && result.error is ZipError.IOException) {
            val error = result.error
            if (error.message.contains("No space left on device", ignoreCase = true)) {
                val estimatedSize = estimateRequiredSpace(files)
                return DomainResult.Error(ZipError.InsufficientSpace(estimatedSize))
            }
        }
        return result
    }

    /**
     * Calculates a rough estimation for the storage space (number of bytes) required
     * by the contents of a ZIP file that is stored without any compression.
     *
     * The overhead of the ZIP file format comes from three sources:
     * 1. Stored file data.
     *    The DEFLATE + compression-level zero method
     *    stores file data in 65535 byte blocks and uses
     *    a 5 byte header for each block.
     *    The overhead is 5 / 65535 = 0.000076295 of the original file size.
     *    We roughly round this to 0.00008.
     * 2. Global file metadata (1 KB is generous)
     * 3. Metadata per file (1 KB is generous)
     */
    private suspend fun estimateRequiredSpace(files: List<ZipFileEntry>): Long {
        var filesTotalSize = 0L
        for (file in files) {
            try {
                if (fileSystem.exists(file.sourcePath)) {
                    filesTotalSize += fileSystem.size(file.sourcePath)
                }
            } catch (_: Exception) {
                // Ignore file I/O exceptions
            }
        }
        return ceil(filesTotalSize * 1.00008).toLong() + 1024L * (1L + files.size)
    }

    /**
     * Deletes the archive files at the specified paths if they exist.
     *
     * This is used to clean up temporary archives after a successful upload
     * or when resetting the application state.
     *
     * @param paths The list of file paths to be deleted.
     */
    override suspend fun cleanupArchives(paths: List<String>) {
        paths.forEach { path ->
            if (fileSystem.exists(path)) {
                fileSystem.delete(path)
            }
        }
    }
}