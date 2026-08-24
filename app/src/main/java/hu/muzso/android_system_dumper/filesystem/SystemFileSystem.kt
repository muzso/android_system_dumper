package hu.muzso.android_system_dumper.filesystem

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.model.DirEntry
import hu.muzso.android_system_dumper.platform.NativeBridge
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemFileSystem @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeBridge: NativeBridge,
    private val dispatcherProvider: DispatcherProvider
) : FileSystem {
    override suspend fun exists(path: String): Boolean = withContext(dispatcherProvider.io()) {
        File(path).exists()
    }

    override suspend fun size(path: String): Long = withContext(dispatcherProvider.io()) {
        File(path).length()
    }

    override suspend fun lastModified(path: String): Long = withContext(dispatcherProvider.io()) {
        File(path).lastModified()
    }

    override suspend fun canRead(path: String): Boolean = withContext(dispatcherProvider.io()) {
        File(path).canRead()
    }

    override suspend fun isDirectory(path: String): Boolean = withContext(dispatcherProvider.io()) {
        File(path).isDirectory
    }

    override suspend fun isFile(path: String): Boolean = withContext(dispatcherProvider.io()) {
        File(path).isFile
    }

    override suspend fun getCanonicalPath(path: String): String = withContext(dispatcherProvider.io()) {
        File(path).canonicalPath
    }

    override suspend fun getParent(path: String): String? = withContext(dispatcherProvider.io()) {
        File(path).parent
    }

    override suspend fun getFileName(path: String): String = withContext(dispatcherProvider.io()) {
        File(path).name
    }

    override suspend fun openInputStream(path: String): InputStream = withContext(dispatcherProvider.io()) {
        FileInputStream(path)
    }

    override suspend fun openOutputStream(path: String, append: Boolean): OutputStream = withContext(dispatcherProvider.io()) {
        FileOutputStream(path, append)
    }

    override suspend fun delete(path: String): Boolean = withContext(dispatcherProvider.io()) {
        File(path).delete()
    }

    override suspend fun list(path: String): List<DirEntry> = withContext(dispatcherProvider.io()) {
        nativeBridge.listDirectory(path, currentCoroutineContext().job).toList()
    }

    override suspend fun writeText(path: String, text: String) = withContext(dispatcherProvider.io()) {
        File(path).writeText(text)
    }

    override suspend fun appendText(path: String, text: String) = withContext(dispatcherProvider.io()) {
        File(path).appendText(text)
    }

    override suspend fun getCacheDir(): String = context.cacheDir.absolutePath

    override suspend fun join(parent: String, child: String): String {
        return Paths.get(parent, child).toString()
    }
}
