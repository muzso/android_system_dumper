package hu.muzso.android_system_dumper.filesystem

import android.content.Context
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.domain.repository.filesystem.FileSystemContract
import hu.muzso.android_system_dumper.model.DirEntry
import hu.muzso.android_system_dumper.platform.NativeBridge
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemFileSystemContractTest : FileSystemContract() {
    private val context = mockk<Context>(relaxed = true)
    private val nativeBridge = mockk<NativeBridge>()

    private val dispatcherProvider = object : DispatcherProvider {
        override fun main(): CoroutineDispatcher = Dispatchers.Unconfined
        override fun default(): CoroutineDispatcher = Dispatchers.Unconfined
        override fun io(): CoroutineDispatcher = Dispatchers.Unconfined
        override fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
    }

    override fun createFileSystem(): FileSystem {
        every { nativeBridge.listDirectory(any(), any()) } answers {
            val path = it.invocation.args[0] as String
            val dir = File(path)
            dir.listFiles()?.map { file ->
                DirEntry(file.name, if (file.isDirectory) 4 else 8)
            }?.toTypedArray() ?: emptyArray()
        }
        return SystemFileSystem(context, nativeBridge, dispatcherProvider)
    }

    override fun createDirectory(path: String) {
        File(path).mkdir()
    }

    @Test
    fun getCacheDir_returns_context_cacheDir() = runTest {
        val cacheDir = File("/data/user/0/hu.muzso.android_system_dumper/cache")
        every { context.cacheDir } returns cacheDir
        val fs = createFileSystem()
        assertThat(fs.getCacheDir()).isEqualTo(cacheDir.absolutePath)
    }
}
