package hu.muzso.android_system_dumper.presentation.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsReducerTest {

    @Test
    fun `AppStateChanged updates AppState`() {
        val states = listOf(
            AppState.MainScreen,
            AppState.QrCodeScreen("text", AppState.MainScreen),
            AppState.HelpScreen
        )

        states.forEach { newState ->
            val result = SettingsResult.AppStateChanged(newState)
            val reducedState = reduce(result)
            assertThat(reducedState).isEqualTo(newState)
        }
    }
}
