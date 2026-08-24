package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.platform.AppServiceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TorUseCasesTest {

    private val appServiceManager = mockk<AppServiceManager>()

    @Test
    fun `StartTorUseCase calls appServiceManager`() {
        val useCase = StartTorUseCase(appServiceManager)
        every { appServiceManager.startTorService(any()) } returns Unit
        
        useCase.execute("some_action")
        verify { appServiceManager.startTorService("some_action") }
    }

    @Test
    fun `StopTorUseCase calls appServiceManager`() {
        val useCase = StopTorUseCase(appServiceManager)
        every { appServiceManager.stopTorService() } returns Unit
        
        useCase.execute()
        verify { appServiceManager.stopTorService() }
    }
}
