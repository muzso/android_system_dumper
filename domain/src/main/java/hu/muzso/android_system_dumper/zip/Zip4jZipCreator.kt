package hu.muzso.android_system_dumper.zip

import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.common.NonClosingOutputStream
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.CompressionLevel
import hu.muzso.android_system_dumper.model.CompressionMethod
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Zip4jZipCreator"

@Singleton
class Zip4jZipCreator @Inject constructor(
    private val logger: FileLogger,
    private val fileSystem: FileSystem,
    private val dispatcherProvider: DispatcherProvider
) : ZipCreator {

    /**
     * Creates an encrypted ZIP archive containing the specified files.
     *
     * This implementation uses the Zip4j library to handle ZIP creation and encryption.
     * It ensures that existing files at the output path are replaced and handles
     * common errors such as missing source files or insufficient storage space.
     *
     * @param files A list of [ZipFileEntry] objects describing the files to include.
     * @param options Configuration options for the ZIP archive, including encryption and output path.
     * @return A [DomainResult] containing the path to the created ZIP or a [ZipError].
     */
    override suspend fun create(
        files: List<ZipFileEntry>,
        options: ZipOptions,
        readIntoMemory: Boolean
    ): DomainResult<String, ZipError> = withContext(dispatcherProvider.io()) {
        if (fileSystem.exists(options.outputFilePath)) {
            fileSystem.delete(options.outputFilePath)
        }

        try {
            val uniqueFiles = files.associateBy { it.zipPath }.values
            fileSystem.openOutputStream(options.outputFilePath).use { outputStream ->
                ZipOutputStream(outputStream, options.passphrase).use { outerZipStream ->
                    if (options.useDoubleZipping) {
                        createDoubleZippedArchive(outerZipStream, uniqueFiles, options)
                    } else {
                        createStandardArchive(outerZipStream, uniqueFiles, options, false)
                    }
                }
            }
            DomainResult.Success(options.outputFilePath)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to create ZIP: ${e.message}", e)
            val message = e.message ?: ""
            if (message.contains(
                    "No space left on device",
                    ignoreCase = true
                ) || message.contains("ENOSPC", ignoreCase = true)
            ) {
                DomainResult.Error(ZipError.IOException("No space left on device", e))
            } else {
                DomainResult.Error(ZipError.Zip4jError(e.message ?: "Zip4j failed", e))
            }
        }
    }

    private suspend fun createStandardArchive(
        zipOutputStream: ZipOutputStream,
        uniqueFiles: Collection<ZipFileEntry>,
        options: ZipOptions,
        readIntoMemory: Boolean
    ) {
        for (file in uniqueFiles) {
            val path = file.sourcePath
            if (!fileSystem.exists(path)) {
                continue
            }
            if (!fileSystem.canRead(path)) {
                continue
            }

            val fileSize = fileSystem.size(path)
            val zipParameters = ZipParameters().apply {
                this.compressionMethod = mapCompressionMethod(options.compressionMethod)
                this.compressionLevel = mapCompressionLevel(options.compressionLevel)
                this.encryptionMethod = mapEncryptionMethod(options.encryptionMethod)
                if (options.encryptionMethod != ZipEncryption.NONE) {
                    isEncryptFiles = true
                    if (options.encryptionMethod == ZipEncryption.AES) {
                        aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    }
                } else {
                    isEncryptFiles = false
                }
                fileNameInZip = file.zipPath
                lastModifiedFileTime = fileSystem.lastModified(path)
                entrySize = fileSize
            }
            // Virtual files in /proc are usually reported with zero length.
            // If this happens, we've to read it fully to get the real length.
            // Otherwise, we'd add it to the ZIP with zero length as metadata
            // and non-zero actual length (in file data).
            val fileInputStream: InputStream = if (fileSize == 0L || readIntoMemory) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                fileSystem.openInputStream(path).use { inputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        byteArrayOutputStream.write(buffer, 0, read)
                    }
                }
                byteArrayOutputStream.close()
                val byteArray = byteArrayOutputStream.toByteArray()
                zipParameters.entrySize = byteArray.size.toLong()
                byteArray.inputStream()
            } else {
                fileSystem.openInputStream(path)
            }

            try {
                logger.v(TAG, "Adding $path to entry ${file.zipPath}.")
                fileInputStream.use { inputStream ->
                    zipOutputStream.putNextEntry(zipParameters)
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        zipOutputStream.write(buffer, 0, read)
                    }
                    zipOutputStream.closeEntry()
                }
            } catch (e: Exception) {
                val message = e.message ?: ""
                when {
                    message.contains(
                        "open failed",
                        ignoreCase = true
                    ) -> logger.i(TAG, "Failed to open $path, skipping.")
                    message.contains(
                        "read failed",
                        ignoreCase = true
                    ) -> logger.i(TAG, "Failed to read $path, skipping.")
                    else -> throw e
                }
            }
        }
    }

    private suspend fun createDoubleZippedArchive(
        outerZipStream: ZipOutputStream,
        uniqueFiles: Collection<ZipFileEntry>,
        options: ZipOptions
    ) {
        val outputFilename = fileSystem.getFileName(options.outputFilePath)
        val innerZipFilename = if (outputFilename.endsWith(".zip", ignoreCase = true)) {
            outputFilename.substringBeforeLast(".zip", "") + ".plain.zip"
        } else {
            "$outputFilename.plain.zip"
        }

        val outerParameters = ZipParameters().apply {
            this.compressionMethod = net.lingala.zip4j.model.enums.CompressionMethod.DEFLATE
            this.compressionLevel = net.lingala.zip4j.model.enums.CompressionLevel.NO_COMPRESSION
            this.encryptionMethod = mapEncryptionMethod(options.encryptionMethod)
            if (options.encryptionMethod != ZipEncryption.NONE) {
                isEncryptFiles = true
                if (options.encryptionMethod == ZipEncryption.AES) {
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }
            fileNameInZip = innerZipFilename
        }

        outerZipStream.putNextEntry(outerParameters)

        val innerOptions = options.copy(
            encryptionMethod = ZipEncryption.NONE,
            compressionMethod = CompressionMethod.DEFLATE,
            compressionLevel = CompressionLevel.FASTEST
        )

        ZipOutputStream(NonClosingOutputStream(outerZipStream)).use { innerZipStream ->
            createStandardArchive(innerZipStream, uniqueFiles, innerOptions, false)
        }

        outerZipStream.closeEntry()
    }

    private fun mapCompressionMethod(method: CompressionMethod): net.lingala.zip4j.model.enums.CompressionMethod = when (method) {
        CompressionMethod.STORE -> net.lingala.zip4j.model.enums.CompressionMethod.STORE
        CompressionMethod.DEFLATE -> net.lingala.zip4j.model.enums.CompressionMethod.DEFLATE
    }

    private fun mapCompressionLevel(level: CompressionLevel): net.lingala.zip4j.model.enums.CompressionLevel = when (level) {
        CompressionLevel.NO_COMPRESSION -> net.lingala.zip4j.model.enums.CompressionLevel.NO_COMPRESSION
        CompressionLevel.FASTEST -> net.lingala.zip4j.model.enums.CompressionLevel.FASTEST
        CompressionLevel.FAST -> net.lingala.zip4j.model.enums.CompressionLevel.FAST
        CompressionLevel.NORMAL -> net.lingala.zip4j.model.enums.CompressionLevel.NORMAL
        CompressionLevel.MAXIMUM -> net.lingala.zip4j.model.enums.CompressionLevel.MAXIMUM
        CompressionLevel.ULTRA -> net.lingala.zip4j.model.enums.CompressionLevel.ULTRA
    }

    private fun mapEncryptionMethod(method: ZipEncryption): EncryptionMethod = when (method) {
        ZipEncryption.NONE -> EncryptionMethod.NONE
        ZipEncryption.STANDARD -> EncryptionMethod.ZIP_STANDARD
        ZipEncryption.AES -> EncryptionMethod.AES
    }
}