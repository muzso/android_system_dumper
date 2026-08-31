package hu.muzso.android_system_dumper.network

import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import java.util.Date
import javax.inject.Inject

class DefaultArchiveGenerator @Inject constructor(
    private val fileSystem: FileSystem,
    private val clock: Clock,
    private val logger: FileLogger,
    private val systemInfo: SystemInfo,
    private val batchFilesUseCase: BatchFilesUseCase,
    private val createArchiveUseCase: CreateArchiveUseCase,
    private val cleanupUseCase: CleanupUseCase
) : ArchiveGenerator {
    private var parameters: UploadParameters? = null
    private var scanResult: ScanResult? = null
    private var batches: List<List<String>> = emptyList()
    private var passphraseString: String? = null
    private var startDate: Date? = null
    private val zipEncryptionPassphraseLength = 16

    override fun prepare(parameters: UploadParameters, scanResult: ScanResult) {
        this.parameters = parameters
        this.scanResult = scanResult
        this.startDate = Date.from(clock.now())

        val batchSizeMb = parameters.customBatchSizeMb.toLongOrNull() ?: 0L
        val batchSizeInBytes = batchSizeMb * 1024L * 1024L
        val activeFilesList = scanResult.readableFiles.map { it.path }
        val currentFileSizes = scanResult.readableFiles.associate { it.path to it.size }
        
        this.batches = if (parameters.shouldUploadZips) {
            batchFilesUseCase.execute(activeFilesList, currentFileSizes, batchSizeInBytes, parameters.maxBatches)
        } else emptyList()

        this.passphraseString = if (parameters.zipEncryption != ZipEncryption.NONE) {
            createArchiveUseCase.generatePassphrase(zipEncryptionPassphraseLength)
        } else null
    }

    override fun getBatchCount(): Int = batches.size

    override fun getEncryptionPassphrase(): String? = passphraseString

    override fun getBatchFilename(index: Int): String {
        val sd = startDate ?: throw IllegalStateException("Generator not prepared")
        val sequenceLength = batches.lastIndex.toString().length
        return createArchiveUseCase.generateBatchFilename(sd, index, sequenceLength)
    }

    override fun getMiscZipFilename(): String {
        val sd = startDate ?: throw IllegalStateException("Generator not prepared")
        return createArchiveUseCase.generateMiscZipFilename(sd)
    }

    override fun shouldGenerateMisc(): Boolean {
        val p = parameters ?: return false
        return p.shouldUploadFileLists || p.shouldUploadGetprop ||
                p.shouldUploadAppLogs
    }

    override suspend fun generateBatch(index: Int): DomainResult<GeneratedZip, ZipError> {
        val p = parameters ?: throw IllegalStateException("Generator not prepared")
        val sd = startDate ?: throw IllegalStateException("Generator not prepared")
        
        if (index < 1 || index > batches.size) return DomainResult.Error(ZipError.IOException("Invalid batch index"))

        val cacheDir = fileSystem.getCacheDir()
        val sequenceLength = batches.lastIndex.toString().length
        val filename = createArchiveUseCase.generateBatchFilename(sd, index, sequenceLength)
        val tempPath = fileSystem.join(cacheDir, filename)
        
        val zipFiles = batches[index - 1].map { ZipFileEntry(it, it) }
        val zipOptions = ZipOptions(
            outputFilePath = tempPath,
            encryptionMethod = p.zipEncryption,
            passphrase = passphraseString?.toCharArray(),
            useDoubleZipping = p.useDoubleZipping
        )
        
        return when (val zipResult = createArchiveUseCase.execute(zipFiles, zipOptions, false)) {
            is DomainResult.Success -> {
                DomainResult.Success(GeneratedZip(tempPath, filename))
            }
            is DomainResult.Error -> DomainResult.Error(zipResult.error)
        }
    }

    override suspend fun generateMisc(): DomainResult<GeneratedZip, ZipError> {
        val p = parameters ?: throw IllegalStateException("Generator not prepared")
        val sr = scanResult ?: throw IllegalStateException("Generator not prepared")
        val sd = startDate ?: throw IllegalStateException("Generator not prepared")

        val cacheDir = fileSystem.getCacheDir()
        val miscFiles = mutableListOf<String>()
        val filesToCleanUp = mutableListOf<String>()

        try {
            if (p.shouldUploadFileLists && sr.readableFiles.isNotEmpty()) {
                val path = fileSystem.join(cacheDir, "readable_list.txt")
                fileSystem.writeText(path, sr.readableFiles.joinToString("\n") { "${it.path}, ${it.size}, ${it.source}" })
                miscFiles.add(fileSystem.getCanonicalPath(path))
                filesToCleanUp.add(path)
            }
            if (p.shouldUploadFileLists && sr.unreadableFiles.isNotEmpty()) {
                val path = fileSystem.join(cacheDir, "unreadable_list.txt")
                fileSystem.writeText(path, sr.unreadableFiles.joinToString("\n"))
                miscFiles.add(fileSystem.getCanonicalPath(path))
                filesToCleanUp.add(path)
            }
            if (p.shouldUploadFileLists && sr.excludedFiles.isNotEmpty()) {
                val path = fileSystem.join(cacheDir, "excluded_list.txt")
                fileSystem.writeText(path, sr.excludedFiles.joinToString("\n"))
                miscFiles.add(fileSystem.getCanonicalPath(path))
                filesToCleanUp.add(path)
            }
            if (p.shouldUploadFileLists && sr.missingFiles.isNotEmpty()) {
                val path = fileSystem.join(cacheDir, "missing_list.txt")
                fileSystem.writeText(path, sr.missingFiles.joinToString("\n"))
                miscFiles.add(fileSystem.getCanonicalPath(path))
                filesToCleanUp.add(path)
            }
            if (p.shouldUploadFileLists && sr.symlinks.isNotEmpty()) {
                val path = fileSystem.join(cacheDir, "symlink_list.txt")
                fileSystem.writeText(path, sr.symlinks.map { "${it.key} -> ${it.value}" }.sorted().joinToString("\n"))
                miscFiles.add(fileSystem.getCanonicalPath(path))
                filesToCleanUp.add(path)
            }
            if (p.shouldUploadGetprop) {
                val path = fileSystem.join(cacheDir, "getprop.txt")
                try {
                    val properties = systemInfo.getSystemProperties()
                    fileSystem.writeText(path, properties)
                } catch (e: Exception) { logger.e("ArchiveGenerator", "getprop failed", e) }
                if (fileSystem.exists(path) && fileSystem.size(path) > 0) {
                    miscFiles.add(fileSystem.getCanonicalPath(path))
                    filesToCleanUp.add(path)
                }
            }
            if (p.shouldUploadAppLogs) {
                logger.flush()
                logger.getLogFilePath()?.let { logPath ->
                    if (fileSystem.exists(logPath) && fileSystem.size(logPath) > 0) {
                        miscFiles.add(fileSystem.getCanonicalPath(logPath))
                    }
                }
            }

            val miscZipFileName = createArchiveUseCase.generateMiscZipFilename(sd)
            val tempPath = fileSystem.join(cacheDir, miscZipFileName)
            val filesForZip = miscFiles.map { ZipFileEntry(it, fileSystem.getFileName(it)) }
            val zipOptions = ZipOptions(
                outputFilePath = tempPath,
                encryptionMethod = p.zipEncryption,
                passphrase = passphraseString?.toCharArray(),
                useDoubleZipping = p.useDoubleZipping
            )

            return when (val zipResult = createArchiveUseCase.execute(filesForZip, zipOptions, true)) {
                is DomainResult.Success -> DomainResult.Success(GeneratedZip(tempPath, miscZipFileName))
                is DomainResult.Error -> DomainResult.Error(zipResult.error)
            }
        } finally {
            cleanupUseCase.execute(filesToCleanUp)
        }
    }

    override suspend fun cleanup(path: String) {
        fileSystem.delete(path)
    }
}
