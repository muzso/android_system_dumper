package hu.muzso.android_system_dumper.platform

import androidx.annotation.Keep
import hu.muzso.android_system_dumper.model.DirEntry
import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JniNativeBridge @Inject constructor() : NativeBridge {
    
    init {
        System.loadLibrary("scanner_jni")
    }

    /**
     * Lists the contents of a directory using native code for improved performance.
     * 
     * This method calls into the JNI layer to perform the directory listing. It maps 
     * the native entry types to the domain-specific [DirEntry] model. Cooperative 
     * cancellation is supported via the provided [job].
     *
     * @param path The absolute path to the directory to list.
     * @param job The coroutine job to monitor for cancellation.
     * @return An array of [DirEntry] objects found in the directory.
     */
    override fun listDirectory(path: String, job: Job?): Array<DirEntry> {
        return listDirectoryNative(path, job).map {
            DirEntry(it.name, it.type)
        }.toTypedArray()
    }

    private external fun listDirectoryNative(path: String, job: Job?): Array<NativeDirEntry>

    @Keep
    private data class NativeDirEntry(
        val name: String,
        val type: Int
    )
}
