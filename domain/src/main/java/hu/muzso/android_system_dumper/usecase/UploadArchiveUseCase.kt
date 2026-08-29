package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.model.upload.UploadWorkflowStatus
import hu.muzso.android_system_dumper.network.ArchiveGenerator
import hu.muzso.android_system_dumper.network.upload.UploadProgressTracker
import hu.muzso.android_system_dumper.platform.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "UploadArchiveUseCase"

class UploadArchiveUseCase @Inject constructor(
    private val clock: Clock,
    private val logger: FileLogger,
    private val uploadBatchUseCase: UploadBatchUseCase,
    private val cleanupUseCase: CleanupUseCase,
    private val resourceProvider: ResourceProvider,
    private val progressTracker: UploadProgressTracker,
    private val dispatcherProvider: DispatcherProvider,
    private val archiveGenerator: ArchiveGenerator
) {
    /**
     * Executes the archive and upload workflow.
     * 
     * This function prepares the upload environment using [ArchiveGenerator],
     * and uploads the generated ZIP archives to the selected service.
     *
     * @param parameters Parameters configuration for the upload process.
     * @param scanResult The results from the system scan to be uploaded.
     * @return A flow of [UploadWorkflowStatus] representing the progress of the workflow.
     */
    fun execute(parameters: UploadParameters, scanResult: ScanResult): Flow<UploadWorkflowStatus> = flow {
        var totalUploads = 0
        var succeededUploads = 0
        val filesToCleanUp = mutableListOf<String>()
        val startTime = clock.monotonicTime()

        try {
            emit(UploadWorkflowStatus.Preparing)
            parameters.selectedService.logConfiguration()
            progressTracker.reset()
            
            val maxUploadRetries = resourceProvider.getMaxUploadRetries()

            archiveGenerator.prepare(parameters, scanResult)
            val batchCount = archiveGenerator.getBatchCount()
            val shouldUploadMiscZip = archiveGenerator.shouldGenerateMisc()

            totalUploads = batchCount + if (shouldUploadMiscZip) 1 else 0
            emit(UploadWorkflowStatus.TotalPlannedUploads(totalUploads))

            val passphraseString = archiveGenerator.getEncryptionPassphrase()

            for (i in 1..batchCount) {
                if (!currentCoroutineContext().isActive) break

                emit(UploadWorkflowStatus.ArchivingBatch(i, batchCount))
                
                when (val zipResult = archiveGenerator.generateBatch(i)) {
                    is DomainResult.Success -> {
                        val generatedZip = zipResult.data
                        filesToCleanUp.add(generatedZip.path)
                        
                        if (!currentCoroutineContext().isActive) break

                        try {
                            when (val uploadResult = uploadBatchUseCase.execute(
                                parameters.selectedService, generatedZip.filename, generatedZip.path, maxUploadRetries, generatedZip.filename, parameters.shouldUseTor,
                                { written, total -> emit(UploadWorkflowStatus.Progress(written, total)) },
                                { label, attempt, totalRetries -> emit(UploadWorkflowStatus.UploadingBatch(label, attempt, totalRetries)) }
                            )) {
                                is DomainResult.Success -> {
                                    succeededUploads++
                                    emit(UploadWorkflowStatus.SuccessfulUploads(succeededUploads))
                                }
                                is DomainResult.Error -> {
                                    logger.e(TAG, "Failed to upload ${generatedZip.filename}: ${uploadResult.error}")
                                    delay(1000.milliseconds)
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            logger.e(TAG, "Failed to upload ${generatedZip.filename}", e)
                            delay(1000.milliseconds)
                        } finally {
                            archiveGenerator.cleanup(generatedZip.path)
                            filesToCleanUp.remove(generatedZip.path)
                        }
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
            }

            if (currentCoroutineContext().isActive && shouldUploadMiscZip) {
                emit(UploadWorkflowStatus.PartitioningBatches) // Repurposing this for misc preparation status
                
                when (val zipResult = archiveGenerator.generateMisc()) {
                    is DomainResult.Success -> {
                        val generatedZip = zipResult.data
                        filesToCleanUp.add(generatedZip.path)

                        try {
                            when (val uploadResult = uploadBatchUseCase.execute(
                                parameters.selectedService, generatedZip.filename, generatedZip.path, maxUploadRetries, generatedZip.filename, parameters.shouldUseTor,
                                { written, total -> emit(UploadWorkflowStatus.Progress(written, total)) },
                                { label, attempt, totalRetries -> emit(UploadWorkflowStatus.UploadingBatch(label, attempt, totalRetries)) }
                            )) {
                                is DomainResult.Success -> {
                                    succeededUploads++
                                    emit(UploadWorkflowStatus.SuccessfulUploads(succeededUploads))
                                }
                                is DomainResult.Error -> {
                                    logger.e(TAG, "Failed to upload misc ZIP: ${uploadResult.error}")
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            logger.e(TAG, "Failed to create/upload misc ZIP", e)
                        } finally {
                            archiveGenerator.cleanup(generatedZip.path)
                            filesToCleanUp.remove(generatedZip.path)
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
            }

            logger.i(TAG, "succeededUploads: $succeededUploads, totalUploads: $totalUploads")
            val urlListUrl = parameters.selectedService.getUrlListUrl()
            val runtimeInSeconds = (clock.monotonicTime() - startTime) / 1_000_000_000L
            if (urlListUrl.isNotEmpty()) {
                if (succeededUploads > 0) {
                    if (succeededUploads >= totalUploads) {
                        emit(UploadWorkflowStatus.Success(
                            downloadUrl = urlListUrl,
                            uploadedZips = succeededUploads,
                            totalZips = totalUploads,
                            totalBytes = progressTracker.totalUploadedBytes.value,
                            runtimeSeconds = runtimeInSeconds,
                            passphrase = passphraseString
                        ))
                    } else {
                        emit(UploadWorkflowStatus.PartialSuccess(
                            downloadUrl = urlListUrl,
                            uploadedZips = succeededUploads,
                            totalZips = totalUploads,
                            totalBytes = progressTracker.totalUploadedBytes.value,
                            runtimeSeconds = runtimeInSeconds,
                            failedZips = totalUploads - succeededUploads,
                            passphrase = passphraseString
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
}
