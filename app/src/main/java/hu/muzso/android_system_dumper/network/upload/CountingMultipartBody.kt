package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.common.Clock
import kotlinx.coroutines.Job
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.InterruptedIOException

class CountingMultipartBody(
    private val delegate: MultipartBody,
    private val clock: Clock,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit,
    private val job: Job? = null
) : RequestBody() {
    override fun contentType(): MediaType = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    /**
     * Writes the multipart body to the provided [sink] while monitoring progress.
     * 
     * This method wraps the output sink in a [ForwardingSink] that counts the written bytes.
     * It periodically invokes the [onProgress] callback (every 500ms or when the upload is complete).
     * It also checks for cooperative cancellation by inspecting the provided [job].
     *
     * @param sink The buffered sink to write the body to.
     * @throws InterruptedIOException If the [job] is canceled.
     */
    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        val countingSink = object : ForwardingSink(sink) {
            private var bytesWritten = 0L
            private var lastUpdate = -500L

            override fun write(source: Buffer, byteCount: Long) {
                if (job?.isCancelled == true) throw InterruptedIOException("Upload canceled cooperatively")
                super.write(source, byteCount)
                bytesWritten += byteCount

                val now = clock.now().toEpochMilli()
                if (now - lastUpdate >= 500L || bytesWritten >= totalBytes) {
                    lastUpdate = now
                    onProgress(bytesWritten, totalBytes)
                }
            }
        }
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}
