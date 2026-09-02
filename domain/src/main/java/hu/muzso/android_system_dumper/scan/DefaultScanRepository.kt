package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.model.DirEntry
import hu.muzso.android_system_dumper.model.FileEntry
import hu.muzso.android_system_dumper.model.ScanError
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ScanStatus
import hu.muzso.android_system_dumper.model.ScanUpdate
import hu.muzso.android_system_dumper.usecase.GetScanRootUseCase
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ScanRepository"

@Singleton
class DefaultScanRepository @Inject constructor(
    private val fileSystem: FileSystem,
    private val collector: FileCollector,
    private val metadataCollector: MetadataCollector,
    private val logger: FileLogger,
    private val clock: Clock,
    private val loadExcludeListUseCase: LoadExcludeListUseCase,
    private val getSeedPathsUseCase: GetSeedPathsUseCase,
    private val getScanRootUseCase: GetScanRootUseCase,
    private val dispatcherProvider: DispatcherProvider
) : ScanRepository {

    private val _scanResult = MutableStateFlow(ScanResult())
    override val scanResult: StateFlow<ScanResult> = _scanResult.asStateFlow()

    private val _scanUpdate = MutableStateFlow(ScanUpdate(0, 0L))
    override val scanUpdate: StateFlow<ScanUpdate> = _scanUpdate.asStateFlow()

    override fun scan(ignoreExcludeList: Boolean, fileCountLimit: Int): Flow<ScanStatus> = flow {
        val session = ScanSession(ignoreExcludeList, fileCountLimit)
        session.run(this)
    }.flowOn(dispatcherProvider.io())

    override fun updateResult(result: ScanResult) {
        _scanResult.value = result
        _scanUpdate.value =
            ScanUpdate(result.readableFiles.size, result.readableFiles.sumOf { it.size })
    }

    override fun updateProgress(update: ScanUpdate, result: ScanResult?) {
        _scanUpdate.value = update
        if (result != null) {
            _scanResult.value = result
        }
    }

    override fun clear() {
        _scanResult.value = ScanResult()
        _scanUpdate.value = ScanUpdate(0, 0L)
    }

    private inner class ScanSession(
        private val ignoreExcludeList: Boolean,
        private val fileCountLimit: Int
    ) {
        private val dirQueue = ArrayDeque<Pair<String, String>>() // path, source
        private val metadataFiles = ArrayDeque<Pair<String, String>>() // path, source
        private val visitedCanonicalDirs = mutableSetOf<String>()
        private val visitedCanonicalFiles = mutableSetOf<String>()

        private var currentTotalBytes = 0L
        private val excludedFilePathPrefixes = loadExcludeListUseCase.execute()
        private val seedPaths = getSeedPathsUseCase.execute()
        private val verbose = true
        private var lastUiUpdate = 0L

        /**
         * Runs the system scan session.
         *
         * This method initializes the scan by seeding paths, then recursively traverses
         * the filesystem while honoring exclusion lists. It reports progress and final results
         * to the provided flow collector.
         *
         * @param flowCollector The collector to emit [ScanStatus] updates to.
         */
        suspend fun run(flowCollector: FlowCollector<ScanStatus>) {
            flowCollector.emit(ScanStatus.RUNNING)
            collector.clear()

            try {
                logger.i(TAG, "Scanning started: ignoreExcludeList=$ignoreExcludeList")
                for (path in seedPaths) {
                    queuePath(path, "filesystem scan of $path", null,
                        checkExcludeList = false,
                        ensureParents = false
                    )
                }

                lastUiUpdate = clock.now().toEpochMilli()

                while (currentCoroutineContext().isActive && (dirQueue.isNotEmpty() || metadataFiles.isNotEmpty())) {
                    val readableFilesAtPhase1Start = collector.getCollectedResult().readableFiles.size

                    // Phase 1: Filesystem Scan
                    while (currentCoroutineContext().isActive && dirQueue.isNotEmpty()) {
                        if (fileCountLimit > 0) {
                            val readableFilesCountInPhase1 = collector.getCollectedResult().readableFiles.size - readableFilesAtPhase1Start
                            if (readableFilesCountInPhase1 >= fileCountLimit) {
                                // discard what remained
                                dirQueue.clear()
                                logger.d(TAG, "Phase 1: Loop file count limit ($fileCountLimit) reached ($readableFilesCountInPhase1), stopping this loop.")
                                logger.d(TAG, "readableFiles:\n" + collector.getCollectedResult().readableFiles.joinToString(
                                    "\n"
                                ) { item -> "${item.path}, ${item.size}, ${item.source}" })
                                break
                            }
                        }

                        yield()
                        val (currentDir, currentSource) = dirQueue.removeFirst()

                        val entries = try {
                            fileSystem.list(currentDir)
                        } catch (e: Exception) {
                            logger.w(TAG, "Failed to list directory: $currentDir, error: ${e.message}")
                            collector.addUnreadableFile(currentDir)
                            continue
                        }

                        logger.d(TAG, "Scanning directory: $currentDir (${entries.size} entries)")

                        for (entry in entries) {
                            if (!currentCoroutineContext().isActive) break

                            val childRaw = fileSystem.join(currentDir, entry.name)
                            if (verbose) logger.v(TAG, "Processing entry: $childRaw (type: ${entry.type})")

                            if (!fileSystem.exists(childRaw)) {
                                logger.i(TAG, "run(): Missing file at $childRaw, skipping.")
                                collector.addMissingFile(childRaw)
                                continue
                            }

                            val canonical = getCanonicalPathSafe(childRaw) ?: continue

                            when (entry.type) {
                                DirEntry.TYPE_DIR -> {
                                    if (verbose) logger.v(TAG, "run(): Directory: \"${childRaw}\"")
                                    if (isExcluded(childRaw)) {
                                        if (verbose) logger.v(TAG, "run(): Excluded directory: \"${childRaw}\"")
                                        continue
                                    }
                                    if (isExcluded(canonical)) {
                                        if (verbose) logger.v(TAG, "run(): Excluded directory: \"${canonical}\"")
                                        continue
                                    }
                                    if (visitedCanonicalDirs.add(canonical)) {
                                        dirQueue.addLast(childRaw to currentSource)
                                    }
                                }
                                DirEntry.TYPE_FILE -> {
                                    if (verbose) logger.v(TAG, "run(): File: \"${childRaw}\"")
                                    if (isProc(childRaw)) {
                                        if (verbose) logger.v(TAG, "run(): Proc: \"${childRaw}\"")
                                        continue
                                    }
                                    if (isExcluded(childRaw)) {
                                        if (verbose) logger.v(TAG, "run(): Excluded file: \"${childRaw}\"")
                                        continue
                                    }
                                    if (isExcluded(canonical)) {
                                        if (verbose) logger.v(TAG, "run(): Excluded file: \"${canonical}\"")
                                        continue
                                    }
                                    if (visitedCanonicalFiles.add(canonical)) {
                                        if (fileSystem.canRead(childRaw)) {
                                            val len = fileSystem.size(childRaw)
                                            collector.addReadableFile(FileEntry(canonical, len, currentSource))
                                            currentTotalBytes += len

                                            checkUiUpdate()

                                            if (metadataCollector.isMetadataFile(childRaw)) {
                                                if (verbose) logger.v(TAG, "run(): Metadata file: \"${childRaw}\"")
                                                metadataFiles.addLast(childRaw to currentSource)
                                            }
                                        } else {
                                            collector.addUnreadableFile(canonical)
                                        }
                                    }
                                }
                                DirEntry.TYPE_LINK -> {
                                    if (verbose) logger.v(TAG, "run(): Symlink: $childRaw -> $canonical")
                                    collector.addSymlink(childRaw, canonical)
                                    if (fileSystem.isDirectory(canonical)) {
                                        if (visitedCanonicalDirs.add(canonical)) {
                                            dirQueue.addLast(childRaw to currentSource)
                                        }
                                    } else if (fileSystem.isFile(canonical)) {
                                        if (isProc(canonical)) {
                                            if (verbose) logger.v(TAG, "run(): Proc: \"${canonical}\"")
                                            continue
                                        }
                                        if (isExcluded(childRaw)) {
                                            if (verbose) logger.v(TAG, "run(): Excluded symlink: \"${childRaw}\"")
                                            continue
                                        }
                                        if (isExcluded(canonical)) {
                                            if (verbose) logger.v(TAG, "run(): Excluded symlink: \"${canonical}\"")
                                            continue
                                        }
                                        if (visitedCanonicalFiles.add(canonical)) {
                                            if (fileSystem.canRead(canonical)) {
                                                val len = fileSystem.size(canonical)
                                                collector.addReadableFile(FileEntry(canonical, len, currentSource))
                                                currentTotalBytes += len

                                                checkUiUpdate()

                                                if (metadataCollector.isMetadataFile(canonical)) {
                                                    if (verbose) logger.v(TAG, "run(): Metadata file: \"${canonical}\"")
                                                    metadataFiles.addLast(canonical to currentSource)
                                                }
                                            } else {
                                                collector.addUnreadableFile(canonical)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Phase 2: Metadata Processing
                    if (currentCoroutineContext().isActive && metadataFiles.isNotEmpty()) {
                        val currentMetadataBatch = ArrayList<Pair<String, String>>()
                        while (metadataFiles.isNotEmpty()) {
                            currentMetadataBatch.add(metadataFiles.removeFirst())
                        }

                        for ((metadataPath, _) in currentMetadataBatch) {
                            if (!currentCoroutineContext().isActive) break
                            metadataCollector.processMetadata(metadataPath) { discoveredPath, source ->
                                queuePath(discoveredPath, source, null, true)
                            }
                        }
                    }
                }

                val finalResult = collector.getCollectedResult()
                logger.i(TAG, "Scan summary: ${finalResult.readableFiles.size} readable files, $currentTotalBytes bytes total, ${finalResult.unreadableFiles.size} unreadable files, ${finalResult.excludedFiles.size} excluded files, ${finalResult.symlinks.size} symlinks.")

                if (!currentCoroutineContext().isActive) {
                    logger.i(TAG, "Scanning cancelled by user.")
                    reportFinalResult(finalResult)
                    flowCollector.emit(ScanStatus.ABORTED)
                } else {
                    logger.i(TAG, "Scanning finished.")
                    reportFinalResult(finalResult)
                    flowCollector.emit(ScanStatus.FINISHED)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Error during scanning", e)
                reportFinalResult(collector.getCollectedResult())
                val scanError = when (e) {
                    is IOException -> ScanError.IOException(e.message ?: "IO error", e)
                    is SecurityException -> ScanError.PermissionDenied("Unknown path due to security exception")
                    else -> ScanError.Unknown(e.message ?: "Unknown error", e)
                }
                flowCollector.emit(ScanStatus.ERROR(scanError))
            }
        }

        /**
         * Queues a path for scanning if it hasn't been visited yet and isn't excluded.
         *
         * @param rawPath The filesystem path to queue.
         * @param currentSource The current discovery source description.
         * @param sourceOverride Optional override for the file source description.
         * @param checkExcludeList Whether to check the exclusion list for this path.
         * @param ensureParents Whether to ensure parent directories are visited.
         */
        private suspend fun queuePath(
            rawPath: String,
            currentSource: String,
            sourceOverride: String? = null,
            checkExcludeList: Boolean = true,
            ensureParents: Boolean = true
        ) {
            val canonical = getCanonicalPathSafe(rawPath) ?: return

            if (!fileSystem.exists(rawPath)) {
                logger.i(TAG, "queuePath(): Missing file at $rawPath, skipping.")
                collector.addMissingFile(rawPath)
                return
            }

            val effectiveSource = sourceOverride ?: currentSource

            if (fileSystem.isDirectory(rawPath)) {
                if (verbose) logger.v(TAG, "queuePath(): Directory: \"${rawPath}\"")
                if (visitedCanonicalDirs.add(canonical)) {
                    dirQueue.addLast(rawPath to effectiveSource)
                    if (ensureParents) {
                        ensureParentVisited(rawPath, effectiveSource)
                    }
                }
            } else if (fileSystem.isFile(rawPath)) {
                if (verbose) logger.v(TAG, "queuePath(): File: \"${rawPath}\"")
                if (checkExcludeList) {
                    if (isExcluded(rawPath)) {
                        if (verbose) logger.v(TAG, "queuePath(): Excluded file: \"${rawPath}\"")
                        return
                    }
                    if (isExcluded(canonical)) {
                        if (verbose) logger.v(TAG, "queuePath(): Excluded file: \"${canonical}\"")
                        return
                    }
                }
                if (isProc(rawPath)) {
                    if (verbose) logger.v(TAG, "queuePath(): Proc: \"${rawPath}\"")
                    return
                }
                if (visitedCanonicalFiles.add(canonical)) {
                    if (fileSystem.canRead(rawPath)) {
                        val len = fileSystem.size(rawPath)
                        collector.addReadableFile(FileEntry(canonical, len, effectiveSource))
                        currentTotalBytes += len
                        
                        checkUiUpdate()

                        if (metadataCollector.isMetadataFile(rawPath)) {
                            if (verbose) logger.v(TAG, "queuePath(): Metadata file: \"${rawPath}\"")
                            metadataFiles.addLast(rawPath to effectiveSource)
                        }
                    }
                }
            }
        }

        /**
         * Ensures that the parent directories of a given path are marked as visited and added to the work queue.
         *
         * @param path The path whose parents should be processed.
         * @param currentSource The discovery source that led to this discovery.
         */
        private suspend fun ensureParentVisited(path: String, currentSource: String) {
            val scanRoot = getScanRootUseCase.execute()
            var current = path
            while (true) {
                if (current == scanRoot) break
                val parent = fileSystem.getParent(current) ?: break
                if (!isExcluded(parent)) {
                    val parentCanonical = getCanonicalPathSafe(parent)
                    if (parentCanonical != null && visitedCanonicalDirs.add(parentCanonical)) {
                        dirQueue.addLast(parent to currentSource)
                    }
                }
                current = parent
            }
        }

        /**
         * Checks if a given path should be excluded from the scan.
         *
         * This method verifies the path against the loaded exclusion prefixes. If the
         * path is excluded, it is also recorded in the [collector].
         *
         * @param path The path to check for exclusion.
         * @return True if the path is excluded, false otherwise.
         */
        private fun isExcluded(path: String): Boolean {
            if (ignoreExcludeList) return false

            val isExcluded = excludedFilePathPrefixes.any { prefix ->
                path == prefix ||
                        path.startsWith(if (prefix.endsWith("/")) prefix else "$prefix/") ||
                        (prefix.endsWith("/") && path == prefix.dropLast(1))
            }

            if (isExcluded) {
                collector.addExcludedFile(path)
            }
            return isExcluded
        }

        private fun isProc(path: String): Boolean {
            return path.startsWith("/proc/")
        }

        /**
         * Safely resolves the canonical path for a given path.
         *
         * This method wraps the filesystem call in a try-catch block to handle
         * potential I/O or security exceptions, returning null on failure.
         *
         * @param path The path to resolve.
         * @return The canonical path string, or null if resolution fails.
         */
        private suspend fun getCanonicalPathSafe(path: String): String? {
            return try {
                fileSystem.getCanonicalPath(path)
            } catch (_: Exception) {
                null
            }
        }

        private fun checkUiUpdate() {
            val now = clock.now().toEpochMilli()
            if (now - lastUiUpdate >= 100) {
                lastUiUpdate = now
                publishProgress()
            }
        }

        /**
         * Publishes the current scan progress to the UI update streams.
         *
         * This method calculates the current file count and total byte size
         * from the collector and triggers an update via [updateProgress].
         */
        private fun publishProgress() {
            updateProgress(
                ScanUpdate(
                    filesCount = collector.getCollectedResult().readableFiles.size,
                    totalBytes = currentTotalBytes
                ),
                collector.getCollectedResult()
            )
        }

        /**
         * Reports the final scan result after sorting the collected file lists.
         *
         * This method ensures that file paths, unreadable files, excluded files,
         * and missing files are sorted alphabetically before being published
         * as the final [ScanResult].
         *
         * @param result The raw [ScanResult] collected during the session.
         */
        private fun reportFinalResult(result: ScanResult) {
            val sortedResult = ScanResult(
                readableFiles = result.readableFiles.sortedBy { it.path },
                unreadableFiles = result.unreadableFiles.sorted(),
                excludedFiles = result.excludedFiles.sorted(),
                missingFiles = result.missingFiles.sorted(),
                symlinks = result.symlinks
            )
            updateResult(sortedResult)
        }
    }
}
