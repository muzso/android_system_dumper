package hu.muzso.android_system_dumper.upload.network

import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import kotlinx.coroutines.Job
import okhttp3.MultipartBody
import okio.Buffer
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.InterruptedIOException

class CountingMultipartBodyTest {

    private val clock = FakeClock()

    @Test
    fun `writeTo throws InterruptedIOException when job is cancelled`() {
        val delegate = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("field", "value")
            .build()
            
        val job = Job()
        job.cancel()

        val countingBody = CountingMultipartBody(
            delegate = delegate,
            clock = clock,
            onProgress = { _, _ -> },
            job = job
        )

        val buffer = Buffer()
        assertThrows(InterruptedIOException::class.java) {
            countingBody.writeTo(buffer)
        }
    }
}
