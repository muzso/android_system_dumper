package hu.muzso.android_system_dumper.platform

import hu.muzso.android_system_dumper.model.DirEntry
import kotlinx.coroutines.Job

interface NativeBridge {
    fun listDirectory(path: String, job: Job?): Array<DirEntry>
}
