package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.platform.AppServiceManager
import javax.inject.Inject

class StartTorUseCase @Inject constructor(
    private val appServiceManager: AppServiceManager
) {
    fun execute(action: String?) {
        appServiceManager.startTorService(action)
    }
}
