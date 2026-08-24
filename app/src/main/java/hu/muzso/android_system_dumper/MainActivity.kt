package hu.muzso.android_system_dumper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.platform.UiMessenger
import hu.muzso.android_system_dumper.presentation.IpInfoViewModel
import hu.muzso.android_system_dumper.presentation.ScanViewModel
import hu.muzso.android_system_dumper.presentation.SettingsViewModel
import hu.muzso.android_system_dumper.presentation.UploadViewModel
import hu.muzso.android_system_dumper.presentation.screens.HelpScreen
import hu.muzso.android_system_dumper.presentation.screens.IpInfoScreen
import hu.muzso.android_system_dumper.presentation.screens.MainScreen
import hu.muzso.android_system_dumper.presentation.screens.QrCodeScreen
import hu.muzso.android_system_dumper.presentation.state.AppState
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var logger: FileLogger
    @Inject
    lateinit var uiMessenger: UiMessenger

    /**
     * Initializes the activity, sets up the UI using Jetpack Compose, and handles app-level navigation.
     *
     * This method performs the following:
     * 1. Logs the contents of the application's cache directory for debugging purposes.
     * 2. Enables edge-to-edge display for a modern visual experience.
     * 3. Sets the content to [hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme] and initializes Hilt view models.
     * 4. Observes the application state to navigate between different screens (Main, QR Code, Help).
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     * this Bundle contains the data it most recently supplied in [onSaveInstanceState].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.logDirectoryContents(applicationContext.cacheDir.absolutePath)
        enableEdgeToEdge()
        setContent {
            AndroidSystemDumperTheme {
                val scanViewModel: ScanViewModel = hiltViewModel()
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val uploadViewModel: UploadViewModel = hiltViewModel()

                val appState by settingsViewModel.appState.collectAsState()

                when (val state = appState) {
                    is AppState.MainScreen -> {
                        MainScreen(
                            scanViewModel = scanViewModel,
                            settingsViewModel = settingsViewModel,
                            uploadViewModel = uploadViewModel,
                            onNavigateToQrCode = {
                                settingsViewModel.processIntent(
                                    SettingsViewModel.Intent.NavigateToQrCode(
                                        it
                                    )
                                )
                            },
                            onShowHelp = { settingsViewModel.processIntent(SettingsViewModel.Intent.NavigateToHelp) },
                            onNavigateToIpInfo = { settingsViewModel.processIntent(SettingsViewModel.Intent.NavigateToIpInfo) },
                            showShortToast = uiMessenger::showShortToast
                        )
                    }

                    is AppState.QrCodeScreen -> {
                        QrCodeScreen(
                            text = state.qrcodeText,
                            uploadViewModel = uploadViewModel,
                            onBack = { settingsViewModel.processIntent(SettingsViewModel.Intent.NavigateToMain) }
                        )
                    }

                    is AppState.HelpScreen -> {
                        HelpScreen(
                            settingsViewModel = settingsViewModel,
                            onBack = { settingsViewModel.processIntent(SettingsViewModel.Intent.NavigateToMain) }
                        )
                    }

                    is AppState.IpInfoScreen -> {
                        val ipInfoViewModel: IpInfoViewModel = hiltViewModel()
                        IpInfoScreen(
                            viewModel = ipInfoViewModel,
                            settingsViewModel = settingsViewModel,
                            onBack = { settingsViewModel.processIntent(SettingsViewModel.Intent.NavigateToMain) }
                        )
                    }
                }
            }
        }
    }
}