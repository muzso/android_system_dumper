package hu.muzso.android_system_dumper.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.muzso.android_system_dumper.presentation.state.IpInfoUiState
import hu.muzso.android_system_dumper.repository.IpInfoRepository
import hu.muzso.android_system_dumper.upload.network.HttpClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the IP Information screen.
 *
 * It triggers the fetching of IP information upon initialization and exposes
 * the result as [IpInfoUiState].
 */
@HiltViewModel
class IpInfoViewModel @Inject constructor(
    private val ipInfoRepository: IpInfoRepository,
    private val httpClientProvider: HttpClientProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<IpInfoUiState>(IpInfoUiState.Loading)
    val uiState: StateFlow<IpInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Re-fetch whenever the HTTP client configuration (e.g., Tor settings) changes.
            httpClientProvider.clientFlow.collect {
                fetchIpInfo()
            }
        }
    }

    /**
     * Triggers a new fetch of IP information.
     * 
     * @param sourceUrl Optional URL to fetch from.
     */
    fun fetchIpInfo(sourceUrl: String? = null) {
        viewModelScope.launch {
            _uiState.value = IpInfoUiState.Loading
            ipInfoRepository.fetchIpInfo(sourceUrl)
                .onSuccess { info ->
                    _uiState.value = IpInfoUiState.Success(info)
                }
                .onFailure { error ->
                    _uiState.value = IpInfoUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}
