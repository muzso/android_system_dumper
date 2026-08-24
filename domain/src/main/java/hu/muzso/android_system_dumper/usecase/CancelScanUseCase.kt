package hu.muzso.android_system_dumper.usecase

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

class CancelScanUseCase {
    suspend fun execute(job: Job?) {
        job?.cancelAndJoin()
    }
}
