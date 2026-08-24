package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.upload.network.UploadProgressTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "UploadArchiveUseCase"
class UploadArchiveUseCase @Inject constructor(
    private val fileSystem: FileSystem,
    private val clock: Clock,
    private val logger: FileLogger,
    private val systemInfo: SystemInfo,
    private val batchFilesUseCase: BatchFilesUseCase,
    private val createArchiveUseCase: CreateArchiveUseCase,
    private val uploadBatchUseCase: UploadBatchUseCase,
    private val cleanupUseCase: CleanupUseCase,
    private val resourceProvider: ResourceProvider,
    private val progressTracker: UploadProgressTracker,
    private val dispatcherProvider: DispatcherProvider
) {
    private val zipEncryptionPasswordLength = 16

    /**
     * Executes the archive and upload workflow.
     * 
     * This function prepares the upload environment, partitions the files into batches,
     * creates encrypted ZIP archives, and uploads them to the selected service.
     * It also handles the creation and upload of a miscellaneous ZIP containing system info and logs.
     *
     * @param parameters Parameters configuration for the upload process.
     * @param scanResult The results from the system scan to be uploaded.
     * @return A flow of [UploadWorkflowStatus] representing the progress of the workflow.
     */
    fun execute(parameters: UploadParameters, scanResult: ScanResult): Flow<UploadWorkflowStatus> = flow {
        val cacheDir = fileSystem.getCacheDir()
        var totalUploads = 0
        var succeededUploads = 0
        val filesToCleanUp = mutableListOf<String>()
        val startTime = clock.monotonicTime()
        val startDate = Date.from(clock.now())

        try {
            emit(UploadWorkflowStatus.Preparing)
            parameters.selectedService.logConfiguration()
            progressTracker.reset()
            
            val maxUploadRetries = resourceProvider.getMaxUploadRetries()
            val batchSizeMb = parameters.customBatchSizeMb.toLongOrNull() ?: 0L
            val batchSizeInBytes = batchSizeMb * 1024L * 1024L
            val activeFilesList = scanResult.readableFiles.map { it.path }

            val shouldUploadMiscZip = shouldUploadMiscZip(parameters)

            if (shouldUploadMiscZip) {
                totalUploads++
                emit(UploadWorkflowStatus.TotalPlannedUploads(totalUploads))
            }

            val passwordString = if (parameters.zipEncryption != ZipEncryption.NONE) {
                createArchiveUseCase.generatePassword(zipEncryptionPasswordLength)
            } else null
            val zipEncryptionPassword = passwordString?.toCharArray()

            if (parameters.shouldUploadZips) {
                var batches: List<List<String>>
                emit(UploadWorkflowStatus.PartitioningBatches)
                val currentFileSizes = scanResult.readableFiles.associate { it.path to it.size }
                batches = batchFilesUseCase.execute(activeFilesList, currentFileSizes, batchSizeInBytes, parameters.maxBatches)
                totalUploads += batches.size
                emit(UploadWorkflowStatus.TotalPlannedUploads(totalUploads))

                val sequenceLength = batches.lastIndex.toString().length
                for (i in batches.indices) {
                    if (!currentCoroutineContext().isActive) break

                    emit(UploadWorkflowStatus.ArchivingBatch(i + 1, batches.size))
                    val tempBatchName = "temp_batch_${i + 1}.zip"
                    val tempPath = fileSystem.join(cacheDir, tempBatchName)
                    
                    val zipFiles = batches[i].map { ZipFileEntry(it, it) }
                    try {
                        val zipOptions = ZipOptions(
                            outputFilePath = tempPath,
                            encryptionMethod = parameters.zipEncryption,
                            password = zipEncryptionPassword
                        )
                        when (val zipResult = createArchiveUseCase.execute(
                            zipFiles, zipOptions, false
                        )) {
                            is DomainResult.Success -> {
                                filesToCleanUp.add(tempPath)
                            }
                            is DomainResult.Error -> {
                                logger.e(TAG, "Failed to create ZIP: ${zipResult.error}")
                                if (zipResult.error is ZipError.InsufficientSpace) {
                                    val runtimeInSeconds = (clock.monotonicTime() - startTime) / 1_000_000_000L
                                    emit(UploadWorkflowStatus.Error(
                                        UploadError.InsufficientStorage(zipResult.error.requiredBytes),
                                        progressTracker.totalUploadedBytes.value,
                                        runtimeInSeconds
                                    ))
                                    return@flow
                                }
                                throw Exception("Failed to create ZIP: ${zipResult.error}")
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logger.e(TAG, "Failed to create ZIP", e)
                        throw e
                    }

                    if (!currentCoroutineContext().isActive) break

                    val zipFilename = createArchiveUseCase.generateBatchFilename(startDate, i + 1, sequenceLength)
                    try {
                        when (val uploadResult = uploadBatchUseCase.execute(
                            parameters.selectedService, zipFilename, tempPath, maxUploadRetries, zipFilename, parameters.shouldUseTor,
                            { written, total ->
                                emit(UploadWorkflowStatus.Progress(written, total))
                            },
                            { label, attempt, totalRetries ->
                                emit(UploadWorkflowStatus.UploadingBatch(label, attempt, totalRetries))
                            }
                        )) {
                            is DomainResult.Success -> {
                                succeededUploads++
                                emit(UploadWorkflowStatus.SuccessfulUploads(succeededUploads))
                            }
                            is DomainResult.Error -> {
                                logger.e(TAG, "Failed to upload $zipFilename: ${uploadResult.error}")
                                delay(1000.milliseconds)
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logger.e(TAG, "Failed to upload $zipFilename", e)
                        delay(1000.milliseconds)
                    } finally {
                        fileSystem.delete(tempPath)
                        filesToCleanUp.remove(tempPath)
                    }
                }
            }

            if (currentCoroutineContext().isActive && shouldUploadMiscZip) {
                val miscFiles = mutableListOf<String>()

                if (parameters.shouldUploadReadableList && scanResult.readableFiles.isNotEmpty()) {
                    emit(UploadWorkflowStatus.CreatingReadableList)
                    val path = fileSystem.join(cacheDir, "readable_list.txt")
                    fileSystem.writeText(path, scanResult.readableFiles.joinToString("\n") { "${it.path}, ${it.size}, ${it.source}" })
                    miscFiles.add(fileSystem.getCanonicalPath(path))
                    filesToCleanUp.add(path)
                }
                if (!currentCoroutineContext().isActive) return@flow

                if (parameters.shouldUploadUnreadableList && scanResult.unreadableFiles.isNotEmpty()) {
                    emit(UploadWorkflowStatus.CreatingUnreadableList)
                    val path = fileSystem.join(cacheDir, "unreadable_list.txt")
                    fileSystem.writeText(path, scanResult.unreadableFiles.joinToString("\n"))
                    miscFiles.add(fileSystem.getCanonicalPath(path))
                    filesToCleanUp.add(path)
                }
                if (!currentCoroutineContext().isActive) return@flow

                if (parameters.shouldUploadExcludedList && scanResult.excludedFiles.isNotEmpty()) {
                    emit(UploadWorkflowStatus.CreatingExcludedList)
                    val path = fileSystem.join(cacheDir, "excluded_list.txt")
                    fileSystem.writeText(path, scanResult.excludedFiles.joinToString("\n"))
                    miscFiles.add(fileSystem.getCanonicalPath(path))
                    filesToCleanUp.add(path)
                }
                if (!currentCoroutineContext().isActive) return@flow

                if (parameters.shouldUploadMissingList && scanResult.missingFiles.isNotEmpty()) {
                    emit(UploadWorkflowStatus.CreatingMissingList)
                    val path = fileSystem.join(cacheDir, "missing_list.txt")
                    fileSystem.writeText(path, scanResult.missingFiles.joinToString("\n"))
                    miscFiles.add(fileSystem.getCanonicalPath(path))
                    filesToCleanUp.add(path)
                }
                if (!currentCoroutineContext().isActive) return@flow

                if (parameters.shouldUploadSymlinkList && scanResult.symlinks.isNotEmpty()) {
                    emit(UploadWorkflowStatus.CreatingSymlinkList)
                    val path = fileSystem.join(cacheDir, "symlink_list.txt")
                    fileSystem.writeText(path, scanResult.symlinks.map { "${it.key} -> ${it.value}" }.sorted().joinToString("\n"))
                    miscFiles.add(fileSystem.getCanonicalPath(path))
                    filesToCleanUp.add(path)
                }
                if (!currentCoroutineContext().isActive) return@flow

                if (parameters.shouldUploadGetprop) {
                    emit(UploadWorkflowStatus.ExecutingCommand("getprop"))
                    val path = fileSystem.join(cacheDir, "getprop.txt")
                    try {
                        val properties = systemInfo.getSystemProperties()
                        fileSystem.writeText(path, properties)
                    } catch (e: Exception) { logger.e(TAG, "getprop failed", e) }
                    if (fileSystem.exists(path) && fileSystem.size(path) > 0) {
                        miscFiles.add(fileSystem.getCanonicalPath(path))
                        filesToCleanUp.add(path)
                    }
                }
                if (!currentCoroutineContext().isActive) return@flow

                if (parameters.shouldUploadAppLogs) {
                    logger.flush()
                    logger.getLogFilePath()?.let { logPath ->
                        if (fileSystem.exists(logPath) && fileSystem.size(logPath) > 0) {
                            miscFiles.add(fileSystem.getCanonicalPath(logPath))
                        }
                    }
                }

                val miscZipFileName = createArchiveUseCase.generateMiscZipFilename(startDate)
                val tempPath = fileSystem.join(cacheDir, miscZipFileName)
                try {
                    val filesForZip = miscFiles.map { ZipFileEntry(it, fileSystem.getFileName(it)) }
                    val zipOptions = ZipOptions(
                        outputFilePath = tempPath,
                        encryptionMethod = parameters.zipEncryption,
                        password = zipEncryptionPassword
                    )
                    when (val zipResult = createArchiveUseCase.execute(
                        filesForZip, zipOptions, true
                    )) {
                        is DomainResult.Success -> {
                            filesToCleanUp.add(tempPath)

                            when (val uploadResult = uploadBatchUseCase.execute(
                                parameters.selectedService, miscZipFileName, tempPath, maxUploadRetries, miscZipFileName, parameters.shouldUseTor,
                                { written, total ->
                                    emit(UploadWorkflowStatus.Progress(written, total))
                                },
                                { label, attempt, totalRetries ->
                                    emit(UploadWorkflowStatus.UploadingBatch(label, attempt, totalRetries))
                                }
                            )) {
                                is DomainResult.Success -> {
                                    succeededUploads++
                                    emit(UploadWorkflowStatus.SuccessfulUploads(succeededUploads))
                                }
                                is DomainResult.Error -> {
                                    logger.e(TAG, "Failed to upload misc ZIP: ${uploadResult.error}")
                                }
                            }
                        }
                        is DomainResult.Error -> {
                            logger.e(TAG, "Failed to create misc ZIP: ${zipResult.error}")
                            if (zipResult.error is ZipError.InsufficientSpace) {
                                val runtimeInSeconds = (clock.monotonicTime() - startTime) / 1_000_000_000L
                                emit(UploadWorkflowStatus.Error(
                                    UploadError.InsufficientStorage(zipResult.error.requiredBytes),
                                    progressTracker.totalUploadedBytes.value,
                                    runtimeInSeconds
                                ))
                                return@flow
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.e(TAG, "Failed to create/upload misc ZIP", e)
                } finally {
                    fileSystem.delete(tempPath)
                    filesToCleanUp.remove(tempPath)
                }
            }

            val urlListUrl = parameters.selectedService.getUrlListUrl()
            val runtimeInSeconds = (clock.monotonicTime() - startTime) / 1_000_000_000L
            logger.i(TAG, "succeededUploads: $succeededUploads, totalUploads: $totalUploads, totalBytes: ${progressTracker.totalUploadedBytes.value}, runtimeInSeconds: $runtimeInSeconds, urlListUrl: $urlListUrl")
            if (urlListUrl.isNotEmpty()) {
                if (succeededUploads > 0) {
                    if (succeededUploads >= totalUploads) {
                        emit(UploadWorkflowStatus.Success(
                            downloadUrl = urlListUrl,
                            uploadedZips = succeededUploads,
                            totalZips = totalUploads,
                            totalBytes = progressTracker.totalUploadedBytes.value,
                            runtimeSeconds = runtimeInSeconds,
                            password = passwordString
                        ))
                    } else {
                        emit(UploadWorkflowStatus.PartialSuccess(
                            downloadUrl = urlListUrl,
                            uploadedZips = succeededUploads,
                            totalZips = totalUploads,
                            totalBytes = progressTracker.totalUploadedBytes.value,
                            runtimeSeconds = runtimeInSeconds,
                            failedZips = totalUploads - succeededUploads,
                            password = passwordString
                        ))
                    }
                } else {
                    emit(UploadWorkflowStatus.Error(UploadError.ZeroSuccessfulUploads("No successful uploads"), progressTracker.totalUploadedBytes.value, runtimeInSeconds))
                }
            } else {
                emit(UploadWorkflowStatus.Error(UploadError.MissingDownloadURL("Empty URL"), progressTracker.totalUploadedBytes.value, runtimeInSeconds))
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                emit(UploadWorkflowStatus.Aborted)
            } else {
                logger.e(TAG, "Upload process crashed", e)
                val runtimeInSeconds = (clock.monotonicTime() - startTime) / 1_000_000_000L
                emit(UploadWorkflowStatus.Error(UploadError.Unknown(e.message ?: "Unknown error", e), progressTracker.totalUploadedBytes.value, runtimeInSeconds))
            }
        } finally {
            withContext(NonCancellable) {
                cleanupUseCase.execute(filesToCleanUp)
            }
        }
    }.flowOn(dispatcherProvider.io())

    /**
     * Determines whether a miscellaneous ZIP archive should be created and uploaded.
     * 
     * The decision is based on whether any of the supplementary metadata or system 
     * information (like logs, property lists, or file manifests) have been 
     * selected for upload in the [uploadParameters].
     *
     * @param uploadParameters The parameters defining the current upload job.
     * @return True if at least one supplementary data item is selected.
     */
    fun shouldUploadMiscZip(uploadParameters: UploadParameters): Boolean {
        return uploadParameters.shouldUploadReadableList || uploadParameters.shouldUploadUnreadableList ||
                uploadParameters.shouldUploadExcludedList || uploadParameters.shouldUploadMissingList ||
                uploadParameters.shouldUploadSymlinkList || uploadParameters.shouldUploadGetprop ||
                uploadParameters.shouldUploadAppLogs

    }
}
