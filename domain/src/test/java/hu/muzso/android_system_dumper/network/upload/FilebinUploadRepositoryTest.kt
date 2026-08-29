package hu.muzso.android_system_dumper.network.upload

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.model.UploadError
import hu.muzso.android_system_dumper.model.upload.UploadResult
import hu.muzso.android_system_dumper.network.upload.gateway.FilebinGateway
import hu.muzso.android_system_dumper.network.upload.gateway.GatewayResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FilebinUploadRepositoryTest {

    private val gateway = mockk<FilebinGateway>(relaxed = true)
    private val platformUtils = mockk<PlatformUtils>()
    private lateinit var repository: FilebinUploadRepository

    @Before
    fun setup() {
        repository = FilebinUploadRepository(gateway, platformUtils)
        every { platformUtils.makeBinName() } returns "test-bin"
    }

    @Test
    fun `upload generates binName only once`() = runTest {
        every { gateway.upload(any(), any(), any()) } returns flowOf(GatewayResult.Success(Unit))

        repository.upload("path1", "file1").test { awaitItem(); awaitComplete() }
        repository.upload("path2", "file2").test { awaitItem(); awaitComplete() }

        verify(exactly = 1) { platformUtils.makeBinName() }
        verify { gateway.upload("test-bin", "file1", "path1").run { } }
        verify { gateway.upload("test-bin", "file2", "path2").run { } }
    }

    @Test
    fun `reset clears binName and total bytes`() = runTest {
        every { gateway.upload(any(), any(), any()) } returns flowOf(GatewayResult.Success(Unit))
        
        repository.incrementTotalUploadedBytes(100L)
        repository.upload("path1", "file1").test { awaitItem(); awaitComplete() }
        
        assertThat(repository.totalUploadedBytes.value).isEqualTo(100L)
        assertThat(repository.getUrlListUrl()).contains("test-bin")

        repository.reset()

        assertThat(repository.totalUploadedBytes.value).isEqualTo(0L)
        assertThat(repository.getUrlListUrl()).isEmpty()
        
        // Next upload should generate new bin name
        every { platformUtils.makeBinName() } returns "new-bin"
        repository.upload("path2", "file2").test { awaitItem(); awaitComplete() }
        verify(exactly = 2) { platformUtils.makeBinName() }
    }

    @Test
    fun `upload maps NetworkError correctly`() = runTest {
        every { gateway.upload(any(), any(), any()) } returns flowOf(GatewayResult.Error("Network failure"))

        repository.upload("path", "file").test {
            val result = awaitItem() as UploadResult.Error
            assertThat(result.error).isInstanceOf(UploadError.NetworkError::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `upload maps Progress correctly`() = runTest {
        every { gateway.upload(any(), any(), any()) } returns flowOf(GatewayResult.Progress(50, 100))

        repository.upload("path", "file").test {
            val result = awaitItem() as UploadResult.Progress
            assertThat(result.bytesWritten).isEqualTo(50)
            assertThat(result.totalBytes).isEqualTo(100)
            awaitComplete()
        }
    }
}
