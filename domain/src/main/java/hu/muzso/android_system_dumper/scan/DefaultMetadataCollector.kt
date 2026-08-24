package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.platform.XmlParser
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DefaultMetadataCollector"

@Singleton
class DefaultMetadataCollector @Inject constructor(
    private val fileSystem: FileSystem,
    private val xmlParser: XmlParser,
    private val logger: FileLogger,
    private val dispatcherProvider: DispatcherProvider,
    private val selinuxAnalyzer: SelinuxContextAnalyzer,
    getSeedPathsUseCase: GetSeedPathsUseCase
) : MetadataCollector {

    private val rcFileSeedPaths = getSeedPathsUseCase.execute().filter { it.length > 1 && it.first() == '/' && it.indexOf('/', 1) == -1 }
    private val rcFileAllowedChars = ('a'..'z').toSet() + ('A'..'Z').toSet() + ('0'..'9').toSet() +
            setOf('#', '+', '-', '.', '/', ':', '=', '>', '@', '_', '|', '~')

    private val permissionsRegex = Regex(".*/(sysconfig|permissions)/[^/]*\\.xml$")

    override fun isMetadataFile(path: String): Boolean {
        val fileName = path.substringAfterLast('/')
        return path.endsWith("/etc/notice.xml", ignoreCase = true) ||
                path.endsWith("/etc/notice.xml.gz", ignoreCase = true) ||
                (path.contains("/etc/selinux/") && path.endsWith("_file_contexts")) ||
                path.endsWith(".rc", ignoreCase = true) ||
                fileName.startsWith("fstab.") ||
                fileName.endsWith(".fstab") ||
                (fileName.startsWith("public.libraries") && fileName.endsWith(".txt")) ||
                fileName == "linker.config.pb" ||
                fileName.endsWith("classpath.pb") ||
                fileName == "modules.dep" ||
                fileName == "modules.load" ||
                permissionsRegex.matches(path)
    }

    /**
     * Processes metadata files found during the scan to discover additional files.
     *
     * Specifically, it looks for `notice.xml` files (optionally GZIP compressed),
     * parses them using [XmlParser], and reports any discovered file paths back
     * via the [onNewPathFound] callback.
     *
     * @param path The path to the potential metadata file.
     * @param onNewPathFound A callback invoked when a new file path is found in the metadata.
     */
    override suspend fun processMetadata(path: String, onNewPathFound: suspend (path: String, source: String) -> Unit) =
        withContext(dispatcherProvider.io()) {
            if (!isMetadataFile(path)) return@withContext

            if (path.contains("/etc/selinux/") && path.endsWith("_file_contexts")) {
                processSelinuxFile(path, onNewPathFound)
                return@withContext
            }

            if (path.endsWith(".rc", ignoreCase = true)) {
                processRcFile(path, onNewPathFound)
                return@withContext
            }

            val fileName = path.substringAfterLast('/')
            if (fileName.startsWith("fstab.") || fileName.endsWith(".fstab")) {
                processFstabFile(path, onNewPathFound)
                return@withContext
            }

            if (fileName.startsWith("public.libraries") && fileName.endsWith(".txt")) {
                processPublicLibrariesFile(path, onNewPathFound)
                return@withContext
            }

            if (fileName == "linker.config.pb" || fileName.endsWith("classpath.pb")) {
                processProtobufFile(path, onNewPathFound)
                return@withContext
            }

            if (fileName == "modules.dep" || fileName == "modules.load") {
                processModulesFile(path, onNewPathFound)
                return@withContext
            }

            if (permissionsRegex.matches(path)) {
                processPermissionsXmlFile(path, onNewPathFound)
                return@withContext
            }

            val isGz = path.endsWith(".gz", ignoreCase = true)
            val sourceDescription = "notice.xml analysis of $path"

            try {
                val inputStream = fileSystem.openInputStream(path)
                val finalStream = if (isGz) GZIPInputStream(inputStream) else inputStream
                val allCandidates = mutableListOf<String>()
                finalStream.use { stream ->
                    xmlParser.parseNoticeXml(stream) { innerPath ->
                        allCandidates.add(innerPath)
                    }
                }

                val uniqueCandidates = allCandidates.distinct()

                if (uniqueCandidates.isNotEmpty()) {
                    logger.d(TAG, "notice.xml analysis candidates in \"$path\": $uniqueCandidates")
                    uniqueCandidates.forEach { candidate ->
                        onNewPathFound(candidate, sourceDescription)
                    }
                }
            } catch (e: Exception) {
                logger.d(TAG, "An exception occurred during parsing of \"$path\": ${e.message}")
            }
        }

    private suspend fun processRcFile(
        path: String,
        onNewPathFound: suspend (path: String, source: String) -> Unit
    ) {
        val sourceDescription = "RC analysis of $path"
        val allCandidates = mutableListOf<String>()

        try {
            fileSystem.openInputStream(path).bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    val lineCandidates = tokens.map { token ->
                        token.filter { it in rcFileAllowedChars }
                    }.filter { filteredToken ->
                        rcFileSeedPaths.any { seed -> filteredToken.startsWith(seed) }
                    }

                    allCandidates.addAll(lineCandidates)
                    line = reader.readLine()
                }
            }

            val uniqueCandidates = allCandidates.distinct()

            if (uniqueCandidates.isNotEmpty()) {
                logger.d(TAG, "RC analysis candidates in \"$path\": $uniqueCandidates")
                uniqueCandidates.forEach { candidate ->
                    onNewPathFound(candidate, sourceDescription)
                }
            }
        } catch (e: Exception) {
            logger.d(TAG, "An exception occurred during parsing of RC file \"$path\": ${e.message}")
        }
    }

    private suspend fun processSelinuxFile(
        path: String,
        onNewPathFound: suspend (path: String, source: String) -> Unit
    ) {
        val sourceDescription = "selinux file context analysis of $path"
        try {
            val allCandidates = mutableListOf<String>()
            fileSystem.openInputStream(path).bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val regex = trimmed.split(Regex("\\s+")).firstOrNull()
                        if (regex != null) {
                            val candidates = selinuxAnalyzer.extractPathCandidates(regex).distinct()
                            if (candidates.isNotEmpty()) {
                                logger.d(TAG, "SELinux analysis of \"$regex\" in \"$path\" -> $candidates")
                                allCandidates.addAll(candidates)
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }

            val uniqueCandidates = allCandidates.distinct()

            if (uniqueCandidates.isNotEmpty()) {
                uniqueCandidates.forEach { candidate ->
                    onNewPathFound(candidate, sourceDescription)
                }
            }
        } catch (e: Exception) {
            logger.d(TAG, "An exception occurred during parsing of SELinux file \"$path\": ${e.message}")
        }
    }

    private suspend fun processFstabFile(
        path: String,
        onNewPathFound: suspend (path: String, source: String) -> Unit
    ) {
        val sourceDescription = "fstab analysis of $path"
        val allCandidates = mutableListOf<String>()

        try {
            fileSystem.openInputStream(path).bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    if (tokens.size >= 2) {
                        val candidate = tokens.drop(1).firstOrNull { it.startsWith("/") }
                        if (candidate != null) {
                            allCandidates.add(candidate)
                        }
                    }
                    line = reader.readLine()
                }
            }

            val uniqueCandidates = allCandidates.distinct()

            if (uniqueCandidates.isNotEmpty()) {
                logger.d(TAG, "fstab analysis candidates in \"$path\": $uniqueCandidates")
                uniqueCandidates.forEach { candidate ->
                    onNewPathFound(candidate, sourceDescription)
                }
            }
        } catch (e: Exception) {
            logger.d(TAG, "An exception occurred during parsing of fstab file \"$path\": ${e.message}")
        }
    }

    private suspend fun processPublicLibrariesFile(
        path: String,
        onNewPathFound: suspend (path: String, source: String) -> Unit
    ) {
        val sourceDescription = "public.libraries.txt analysis of $path"
        val allCandidates = mutableListOf<String>()
        val prefixes = listOf("/system/lib/", "/system/lib64/", "/vendor/lib/", "/vendor/lib64/")

        try {
            fileSystem.openInputStream(path).bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        prefixes.forEach { prefix ->
                            allCandidates.add(prefix + trimmed)
                        }
                    }
                    line = reader.readLine()
                }
            }

            val uniqueCandidates = allCandidates.distinct()

            if (uniqueCandidates.isNotEmpty()) {
                logger.d(TAG, "public.libraries.txt analysis candidates in \"$path\": $uniqueCandidates")
                uniqueCandidates.forEach { candidate ->
                    onNewPathFound(candidate, sourceDescription)
                }
            }
        } catch (e: Exception) {
            logger.d(
                TAG,
                "An exception occurred during parsing of public libraries file \"$path\": ${e.message}"
            )
        }
    }

    private suspend fun processProtobufFile(
        path: String,
        onNewPathFound: suspend (path: String, source: String) -> Unit
    ) {
        val sourceDescription = "classpath analysis of $path"
        val allCandidates = mutableListOf<String>()
        val prefixes = listOf("/system/lib/", "/system/lib64/", "/vendor/lib/", "/vendor/lib64/")

        try {
            val bytes = fileSystem.openInputStream(path).use { it.readBytes() }
            val extractedStrings = mutableListOf<String>()
            var current = StringBuilder()

            for (byte in bytes) {
                if (byte in 33..126) {
                    current.append(byte.toInt().toChar())
                } else {
                    if (current.isNotEmpty()) {
                        extractedStrings.add(current.toString())
                        current = StringBuilder()
                    }
                }
            }
            if (current.isNotEmpty()) {
                extractedStrings.add(current.toString())
            }

            extractedStrings.forEach { str ->
                val startsWithSlash = str.startsWith("/")
                val startsWithLetter = str.firstOrNull()?.let { it in 'a'..'z' || it in 'A'..'Z' } ?: false
                val hasNoSlash = !str.contains("/")

                if (startsWithSlash) {
                    allCandidates.add(str)
                } else if (startsWithLetter && hasNoSlash) {
                    prefixes.forEach { prefix ->
                        allCandidates.add(prefix + str)
                    }
                }
            }

            val uniqueCandidates = allCandidates.distinct()

            if (uniqueCandidates.isNotEmpty()) {
                logger.d(TAG, "classpath analysis candidates in \"$path\": $uniqueCandidates")
                uniqueCandidates.forEach { candidate ->
                    onNewPathFound(candidate, sourceDescription)
                }
            }
        } catch (e: Exception) {
            logger.d(
                TAG,
                "An exception occurred during parsing of protobuf file \"$path\": ${e.message}"
            )
        }
    }

    private suspend fun processPermissionsXmlFile(
        path: String,
        onNewPathFound: suspend (path: String, source: String) -> Unit
    ) {
        val sourceDescription = "permissions xml analysis of $path"

        try {
            val allCandidates = mutableListOf<String>()
            fileSystem.openInputStream(path).use { inputStream ->
                xmlParser.parsePermissionsXml(inputStream) { candidate ->
                    allCandidates.add(candidate)
                }
            }

            val uniqueCandidates = allCandidates.distinct()

            if (uniqueCandidates.isNotEmpty()) {
                logger.d(TAG, "permissions xml analysis candidates in \"$path\": $uniqueCandidates")
                uniqueCandidates.forEach { candidate ->
                    onNewPathFound(candidate, sourceDescription)
                }
            }
        } catch (e: Exception) {
            logger.d(
                TAG,
                "An exception occurred during parsing of permissions XML file \"$path\": ${e.message}"
            )
        }
    }

    private suspend fun processModulesFile(
        path: String,
        onNewPathFound: suspend (path: String, source: String) -> Unit
    ) {
        val fileName = path.substringAfterLast('/')
        val sourceDescription = "module analysis of $path"
        val allCandidates = mutableListOf<String>()

        try {
            fileSystem.openInputStream(path).bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        if (fileName == "modules.load") {
                            allCandidates.add("/vendor/lib/modules/$trimmed")
                        } else if (fileName == "modules.dep") {
                            // Split by colon or whitespace
                            val tokens = trimmed.split(Regex("[:\\s]+")).filter { it.isNotEmpty() }
                            allCandidates.addAll(tokens)
                        }
                    }
                    line = reader.readLine()
                }
            }

            val uniqueCandidates = allCandidates.distinct()

            if (uniqueCandidates.isNotEmpty()) {
                logger.d(TAG, "module analysis candidates in \"$path\": $uniqueCandidates")
                uniqueCandidates.forEach { candidate ->
                    onNewPathFound(candidate, sourceDescription)
                }
            }
        } catch (e: Exception) {
            logger.d(TAG, "An exception occurred during parsing of module file \"$path\": ${e.message}")
        }
    }
}
