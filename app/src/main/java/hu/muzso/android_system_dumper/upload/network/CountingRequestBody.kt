package hu.muzso.android_system_dumper.upload.network

import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.filesystem.FileSystem
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InterruptedIOException

class CountingRequestBody(
    private val filePath: String,
    private val fileSystem: FileSystem,
    private val clock: Clock,
    private val contentType: String = "application/octet-stream",
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit,
    private val job: Job? = null
) : RequestBody() {

    override fun contentType(): MediaType? {
        return contentType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long = runBlocking {
        fileSystem.size(filePath)
    }

    /**
     * Writes the file content to the provided [sink] while reporting progress.
     * 
     * This method reads the file from the [fileSystem] in chunks and writes them to OkHttp's sink.
     * It tracks `bytesWritten` and invokes [onProgress] at most once every 500ms, or when the
     * final byte is written. It also supports cooperative cancellation via the [job].
     *
     * @param sink The buffered sink to write to.
     * @throws InterruptedIOException If the [job] is canceled during writing.
     */
    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        var bytesWritten = 0L
        val buffer = ByteArray(65536)
        var lastUpdate = -500L

        runBlocking {
            fileSystem.openInputStream(filePath).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    if (job?.isCancelled == true) throw InterruptedIOException("Upload canceled cooperatively")
                    sink.write(buffer, 0, read)
                    bytesWritten += read

                    val now = clock.now().toEpochMilli()
                    if (now - lastUpdate >= 500L || bytesWritten >= totalBytes) {
                        lastUpdate = now
                        onProgress(bytesWritten, totalBytes)
                    }
                }
            }
        }
    }
}
