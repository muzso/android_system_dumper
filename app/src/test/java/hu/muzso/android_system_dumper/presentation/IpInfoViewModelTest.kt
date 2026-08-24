package hu.muzso.android_system_dumper.presentation

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.model.IpInfo
import hu.muzso.android_system_dumper.presentation.state.IpInfoUiState
import hu.muzso.android_system_dumper.repository.IpInfoRepository
import hu.muzso.android_system_dumper.upload.network.HttpClientProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IpInfoViewModelTest {

    private val repository = mockk<IpInfoRepository>()
    private val httpClientProvider = mockk<HttpClientProvider>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: IpInfoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { httpClientProvider.clientFlow } returns MutableStateFlow(OkHttpClient())
        every { repository.getAvailableSources() } returns listOf("url")
        // Loading state is initial
        coEvery { repository.fetchIpInfo(any()) } returns Result.success(IpInfo("url", emptyMap()))
        
        viewModel = IpInfoViewModel(repository, httpClientProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchIpInfo updates uiState to Success on repository success`() = runTest {
        val expectedInfo = IpInfo("https://api.ip.com", mapOf("Ip" to "1.1.1.1"))
        coEvery { repository.fetchIpInfo("https://api.ip.com") } returns Result.success(expectedInfo)

        viewModel.fetchIpInfo("https://api.ip.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(IpInfoUiState.Success::class.java)
        val successState = viewModel.uiState.value as IpInfoUiState.Success
        assertThat(successState.ipInfo).isEqualTo(expectedInfo)
    }

    @Test
    fun `fetchIpInfo updates uiState to Error on repository failure`() = runTest {
        coEvery { repository.fetchIpInfo(any()) } returns Result.failure(Exception("Network error"))

        viewModel.fetchIpInfo()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(IpInfoUiState.Error::class.java)
        val errorState = viewModel.uiState.value as IpInfoUiState.Error
        assertThat(errorState.message).isEqualTo("Network error")
    }
}
