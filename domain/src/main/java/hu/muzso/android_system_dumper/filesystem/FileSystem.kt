package hu.muzso.android_system_dumper.filesystem

import hu.muzso.android_system_dumper.model.DirEntry
import java.io.InputStream
import java.io.OutputStream

interface FileSystem {
    suspend fun exists(path: String): Boolean
    suspend fun size(path: String): Long
    suspend fun lastModified(path: String): Long
    suspend fun canRead(path: String): Boolean
    suspend fun isDirectory(path: String): Boolean
    suspend fun isFile(path: String): Boolean
    suspend fun getCanonicalPath(path: String): String
    suspend fun getParent(path: String): String?
    suspend fun getFileName(path: String): String
    suspend fun openInputStream(path: String): InputStream
    suspend fun openOutputStream(path: String, append: Boolean = false): OutputStream
    suspend fun delete(path: String): Boolean
    suspend fun list(path: String): List<DirEntry>
    suspend fun writeText(path: String, text: String)
    suspend fun appendText(path: String, text: String)
    suspend fun getCacheDir(): String
    suspend fun join(parent: String, child: String): String
}
