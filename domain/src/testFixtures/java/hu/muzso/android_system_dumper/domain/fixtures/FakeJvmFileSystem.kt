package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.model.DirEntry
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class FakeJvmFileSystem(private val dispatcherProvider: DispatcherProvider, rootPath: Path? = null) : FileSystem, AutoCloseable {
    private val shutdownHook = Thread {
        try {
            root.deleteRecursively()
        } catch (_: Exception) {
        }
    }

    private val root: Path = rootPath ?: createTempDirectory("fake_jvm_fs").also {
        Runtime.getRuntime().addShutdownHook(shutdownHook)
    }
    private val isOwned = rootPath == null

    override suspend fun exists(path: String): Boolean = withContext(dispatcherProvider.io()) {
        getPath(path).exists()
    }
    override suspend fun size(path: String): Long = withContext(dispatcherProvider.io()) {
        Files.size(getPath(path))
    }
    override suspend fun lastModified(path: String): Long = withContext(dispatcherProvider.io()) {
        Files.getLastModifiedTime(getPath(path)).toMillis()
    }
    override suspend fun canRead(path: String): Boolean = withContext(dispatcherProvider.io()) {
        Files.isReadable(getPath(path))
    }
    override suspend fun isDirectory(path: String): Boolean = withContext(dispatcherProvider.io()) {
        getPath(path).isDirectory()
    }
    override suspend fun isFile(path: String): Boolean = withContext(dispatcherProvider.io()) {
        getPath(path).isRegularFile()
    }

    private fun toFakePath(path: Path): String {
        val relative = root.relativize(path.normalize()).toString().replace("\\", "/")
        return if (relative.isEmpty()) "/" else if (relative.startsWith("/")) relative else "/$relative"
    }

    private fun getInternalCanonicalPath(path: Path): String {
        val realPath = if (path.exists()) path.toRealPath() else path.normalize()
        return toFakePath(realPath)
    }

    override suspend fun getCanonicalPath(path: String): String = withContext(dispatcherProvider.io()) {
        val p = getPath(path)
        getInternalCanonicalPath(p)
    }

    override suspend fun getParent(path: String): String? {
        val p = getPath(path)
        val parent = p.parent ?: return null
        return if (parent.startsWith(root)) toFakePath(parent) else null
    }

    override suspend fun getFileName(path: String): String = getPath(path).fileName.toString()

    override suspend fun openInputStream(path: String): InputStream = withContext(dispatcherProvider.io()) {
        Files.newInputStream(getPath(path))
    }

    override suspend fun openOutputStream(path: String, append: Boolean): OutputStream = withContext(dispatcherProvider.io()) {
        val p = getPath(path)
        p.parent?.createDirectories()
        if (append) {
            Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        } else {
            Files.newOutputStream(p)
        }
    }

    private fun getPath(path: String): Path {
        val p = java.nio.file.Paths.get(path)
        val resolved = if (p.isAbsolute && p.startsWith(root)) {
            p.normalize()
        } else {
            val cleanPath = path.trim().replace(Regex("^/+"), "")
            root.resolve(cleanPath).normalize()
        }

        if (!resolved.startsWith(root)) {
            throw IllegalArgumentException("Path traversal attempt: $path")
        }
        return resolved
    }

    override suspend fun delete(path: String): Boolean = withContext(dispatcherProvider.io()) {
        Files.deleteIfExists(getPath(path))
    }

    override suspend fun list(path: String): List<DirEntry> = withContext(dispatcherProvider.io()) {
        val p = getPath(path)
        if (!p.isDirectory()) return@withContext emptyList()
        p.listDirectoryEntries().map { entry ->
            val type = when {
                Files.isSymbolicLink(entry) -> DirEntry.TYPE_LINK
                entry.isDirectory() -> DirEntry.TYPE_DIR
                else -> DirEntry.TYPE_FILE
            }
            DirEntry(entry.fileName.toString(), type)
        }
    }

    override suspend fun writeText(path: String, text: String) = withContext(dispatcherProvider.io()) {
        val p = getPath(path)
        p.parent?.createDirectories()
        p.writeText(text)
    }

    override suspend fun appendText(path: String, text: String) = withContext(dispatcherProvider.io()) {
        val p = getPath(path)
        p.parent?.createDirectories()
        Files.write(p, text.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        Unit
    }

    override suspend fun getCacheDir(): String = "/cache"

    override suspend fun join(parent: String, child: String): String {
        val p = java.nio.file.Paths.get(parent)
        val resolved = if (p.isAbsolute && p.startsWith(root)) {
            p.resolve(child.trim().replace(Regex("^/+"), "")).normalize()
        } else {
            val cleanParent = parent.trim().replace(Regex("^/+"), "")
            root.resolve(cleanParent).resolve(child.trim().replace(Regex("^/+"), "")).normalize()
        }
        return toFakePath(resolved)
    }

    fun addFileWithText(path: String, text: String): String {
        val p = getPath(path)
        p.parent?.createDirectories()
        p.writeText(text)
        return toFakePath(p)
    }

    fun addFileOfSize(path: String, size: Long = 0L): String {
        val p = getPath(path)
        p.parent?.createDirectories()
        Files.newOutputStream(p).use { output ->
            val buffer = ByteArray(8192)
            buffer.fill(' '.code.toByte())
            var remaining = size
            while (remaining > 0) {
                val toWrite = minOf(remaining, buffer.size.toLong()).toInt()
                output.write(buffer, 0, toWrite)
                remaining -= toWrite
            }
        }
        return toFakePath(p)
    }

    fun addDir(path: String): String {
        val p = getPath(path)
        p.createDirectories()
        return toFakePath(p)
    }

    fun addSymlink(path: String, target: String): String {
        val p = getPath(path)
        val t = getPath(target)
        p.parent?.createDirectories()
        Files.createSymbolicLink(p, t)
        return toFakePath(p)
    }

    override fun close() {
        if (isOwned) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook)
            } catch (_: Exception) {
            }
            try {
                shutdownHook.start()
            } catch (_: IllegalThreadStateException) {
            }
        }
    }
}
