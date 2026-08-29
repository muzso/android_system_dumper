package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.model.DirEntry
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths

class FakeMemoryFileSystem : FileSystem {

    data class Node(
        val path: String,
        val type: Int,
        val canRead: Boolean = true,
        val size: Long = 0L,
        val lastModified: Long = 0L,
        val binaryContent: ByteArray = byteArrayOf(),
        val children: List<DirEntry> = emptyList(),
        val symlinkTarget: String? = null
    ) {
        val content: String get() = String(binaryContent)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Node
            if (path != other.path) return false
            if (type != other.type) return false
            if (canRead != other.canRead) return false
            if (size != other.size) return false
            if (lastModified != other.lastModified) return false
            if (!binaryContent.contentEquals(other.binaryContent)) return false
            if (children != other.children) return false
            if (symlinkTarget != other.symlinkTarget) return false
            return true
        }

        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + type
            result = 31 * result + canRead.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + lastModified.hashCode()
            result = 31 * result + binaryContent.contentHashCode()
            result = 31 * result + children.hashCode()
            result = 31 * result + (symlinkTarget?.hashCode() ?: 0)
            return result
        }
    }

    val nodes = mutableMapOf<String, Node>()
    var simulateNoSpaceError = false

    private fun normalizePath(path: String): String {
        val p = Paths.get(path).toAbsolutePath().normalize()
        return p.toString().replace("\\", "/")
    }

    fun addFileWithText(path: String, text: String, canRead: Boolean = true): String {
        val normalized = normalizePath(path)
        createParentDirs(normalized)
        val bytes = text.toByteArray()
        nodes[normalized] = Node(normalized, type = DirEntry.TYPE_FILE, size = bytes.size.toLong(), canRead = canRead, binaryContent = bytes, lastModified = System.currentTimeMillis())
        updateParent(normalized, DirEntry.TYPE_FILE)
        return normalized
    }

    fun addFileOfSize(path: String, size: Long = 0L, canRead: Boolean = true): String {
        val normalized = normalizePath(path)
        createParentDirs(normalized)
        val bytes = ByteArray(size.toInt()) { ' '.code.toByte() }
        nodes[normalized] = Node(normalized, type = DirEntry.TYPE_FILE, size = bytes.size.toLong(), canRead = canRead, binaryContent = bytes, lastModified = System.currentTimeMillis())
        updateParent(normalized, DirEntry.TYPE_FILE)
        return normalized
    }

    fun addDir(path: String): String {
        val normalized = normalizePath(path)
        createParentDirs(normalized)
        nodes[normalized] = Node(normalized, type = DirEntry.TYPE_DIR, lastModified = System.currentTimeMillis())
        updateParent(normalized, DirEntry.TYPE_DIR)
        return normalized
    }

    private fun createParentDirs(path: String) {
        val pathObj = Paths.get(path)
        var parent = pathObj.parent
        val parentsToCreate = mutableListOf<String>()
        while (parent != null) {
            val parentStr = parent.toString().replace("\\", "/")
            if (nodes.containsKey(parentStr)) break
            parentsToCreate.add(parentStr)
            parent = parent.parent
        }
        parentsToCreate.reversed().forEach { p ->
            nodes[p] = Node(p, type = DirEntry.TYPE_DIR, lastModified = System.currentTimeMillis())
            updateParent(p, DirEntry.TYPE_DIR)
        }
    }

    private fun updateParent(path: String, type: Int) {
        val pathObj = Paths.get(path)
        pathObj.parent?.let { parent ->
            val parentStr = parent.toString().replace("\\", "/")
            val parentNode = nodes[parentStr] ?: Node(parentStr, type = DirEntry.TYPE_DIR, lastModified = System.currentTimeMillis())
            nodes[parentStr] = parentNode.copy(
                children = (parentNode.children + DirEntry(pathObj.fileName.toString(), type)).distinctBy { it.name }
            )
        }
    }

    fun addSymlink(path: String, target: String): String {
        val normalized = normalizePath(path)
        createParentDirs(normalized)
        nodes[normalized] = Node(normalized, type = DirEntry.TYPE_LINK, symlinkTarget = target, lastModified = System.currentTimeMillis())
        updateParent(normalized, DirEntry.TYPE_LINK)
        return normalized
    }

    private fun resolveSymlink(path: String, maxDepth: Int = 10): String {
        if (maxDepth == 0) return path
        val node = nodes[path] ?: return path
        val target = node.symlinkTarget ?: return path
        return resolveSymlink(normalizePath(target), maxDepth - 1)
    }

    override suspend fun exists(path: String): Boolean {
        val normalized = normalizePath(path)
        val node = nodes[normalized] ?: return false
        if (node.type == DirEntry.TYPE_LINK) {
            val resolved = resolveSymlink(normalized)
            return resolved != normalized && nodes.containsKey(resolved)
        }
        return true
    }
    override suspend fun size(path: String): Long {
        val normalized = normalizePath(path)
        val resolved = resolveSymlink(normalized)
        return nodes[resolved]?.size ?: 0L
    }

    override suspend fun lastModified(path: String): Long {
        val normalized = normalizePath(path)
        val resolved = resolveSymlink(normalized)
        return nodes[resolved]?.lastModified ?: 0L
    }

    override suspend fun canRead(path: String): Boolean {
        val normalized = normalizePath(path)
        val resolved = resolveSymlink(normalized)
        return nodes[resolved]?.canRead ?: false
    }

    override suspend fun isDirectory(path: String): Boolean {
        val normalized = normalizePath(path)
        val resolved = resolveSymlink(normalized)
        return nodes[resolved]?.type == DirEntry.TYPE_DIR
    }

    override suspend fun isFile(path: String): Boolean {
        val normalized = normalizePath(path)
        val resolved = resolveSymlink(normalized)
        return nodes[resolved]?.type == DirEntry.TYPE_FILE
    }

    override suspend fun getCanonicalPath(path: String): String = resolveSymlink(normalizePath(path))
    override suspend fun getParent(path: String): String? {
        val normalized = normalizePath(path)
        return Paths.get(normalized).parent?.toString()?.replace("\\", "/")
    }

    override suspend fun getFileName(path: String): String {
        val normalized = normalizePath(path)
        return Paths.get(normalized).fileName.toString()
    }

    override suspend fun openInputStream(path: String): InputStream {
        val normalized = normalizePath(path)
        val resolved = resolveSymlink(normalized)
        return (nodes[resolved]?.binaryContent ?: byteArrayOf()).inputStream()
    }

    override suspend fun openOutputStream(path: String, append: Boolean): OutputStream {
        if (simulateNoSpaceError) throw java.io.IOException("No space left on device")
        val normalized = normalizePath(path)
        val node = nodes[normalized]
        if (node != null && node.type == DirEntry.TYPE_DIR) {
            throw java.io.IOException("Cannot write to a directory: $normalized")
        }
        createParentDirs(normalized)
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                val bytes = toByteArray()
                val oldNode = nodes[normalized]
                val newContent = if (append) (oldNode?.binaryContent ?: byteArrayOf()) + bytes else bytes
                nodes[normalized] = Node(normalized, type = DirEntry.TYPE_FILE, size = newContent.size.toLong(), binaryContent = newContent, lastModified = System.currentTimeMillis())
                updateParent(normalized, DirEntry.TYPE_FILE)
            }
        }
    }

    override suspend fun delete(path: String): Boolean {
        val normalized = normalizePath(path)
        if (nodes.containsKey(normalized)) {
            nodes.remove(normalized)
            removeFromParent(normalized)
            return true
        }
        return false
    }

    private fun removeFromParent(path: String) {
        val pathObj = Paths.get(path)
        pathObj.parent?.let { parent ->
            val parentStr = parent.toString().replace("\\", "/")
            val parentNode = nodes[parentStr]
            if (parentNode != null) {
                nodes[parentStr] = parentNode.copy(
                    children = parentNode.children.filter { it.name != pathObj.fileName.toString() }
                )
            }
        }
    }

    override suspend fun list(path: String): List<DirEntry> {
        val normalized = normalizePath(path)
        val resolved = resolveSymlink(normalized)
        return nodes[resolved]?.children ?: emptyList()
    }

    override suspend fun writeText(path: String, text: String) {
        if (simulateNoSpaceError) throw java.io.IOException("No space left on device")
        val normalized = normalizePath(path)
        val node = nodes[normalized]
        if (node != null && node.type == DirEntry.TYPE_DIR) {
            throw java.io.IOException("Cannot write to a directory: $normalized")
        }
        createParentDirs(normalized)
        val bytes = text.toByteArray()
        nodes[normalized] = Node(normalized, type = DirEntry.TYPE_FILE, size = bytes.size.toLong(), binaryContent = bytes, lastModified = System.currentTimeMillis())
        updateParent(normalized, DirEntry.TYPE_FILE)
    }

    override suspend fun appendText(path: String, text: String) {
        val normalized = normalizePath(path)
        val node = nodes[normalized]
        if (node != null && node.type == DirEntry.TYPE_DIR) {
            throw java.io.IOException("Cannot write to a directory: $normalized")
        }
        createParentDirs(normalized)
        val oldNode = nodes[normalized]
        val bytes = text.toByteArray()
        val newContent = (oldNode?.binaryContent ?: byteArrayOf()) + bytes
        nodes[normalized] = Node(normalized, type = DirEntry.TYPE_FILE, size = newContent.size.toLong(), binaryContent = newContent, lastModified = System.currentTimeMillis())
        updateParent(normalized, DirEntry.TYPE_FILE)
    }

    override suspend fun getCacheDir(): String = "/cache"
    override suspend fun join(parent: String, child: String): String {
        val cleanParent = normalizePath(parent)
        val cleanChild = child.trim().replace(Regex("^/+"), "")
        return normalizePath(if (cleanParent == "/") "/$cleanChild" else "$cleanParent/$cleanChild")
    }
}
