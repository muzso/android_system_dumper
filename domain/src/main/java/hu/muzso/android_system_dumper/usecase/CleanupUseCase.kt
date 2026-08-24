package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.filesystem.FileSystem

class CleanupUseCase(
    private val fileSystem: FileSystem
) {
    suspend fun execute(paths: List<String>) {
        paths.forEach { if (fileSystem.exists(it)) fileSystem.delete(it) }
    }
}
