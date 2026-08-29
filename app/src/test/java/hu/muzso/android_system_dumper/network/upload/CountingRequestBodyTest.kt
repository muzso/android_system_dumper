package hu.muzso.android_system_dumper.network.upload

import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeJvmFileSystem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okio.Buffer
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.InterruptedIOException

@OptIn(ExperimentalCoroutinesApi::class)
class CountingRequestBodyTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(testDispatcher)
    private val fileSystem = FakeJvmFileSystem(dispatcherProvider)
    private val clock = FakeClock()

    @Test
    fun `writeTo throws InterruptedIOException when job is cancelled`() {
        val path = fileSystem.addFileWithText("test.txt", "some content")
        val job = Job()
        job.cancel()

        val requestBody = CountingRequestBody(
            filePath = path,
            fileSystem = fileSystem,
            clock = clock,
            onProgress = { _, _ -> },
            job = job
        )

        val buffer = Buffer()
        assertThrows(InterruptedIOException::class.java) {
            requestBody.writeTo(buffer)
        }
    }
}
