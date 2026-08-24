package hu.muzso.android_system_dumper.usecase

class LoadExcludeListUseCase {
    @Suppress("SdCardPath")
    fun execute(): List<String> = listOf(
        "/bugreports", "/cache", "/data", "/debug", "/dev", "/lost+found",
        "/sdcard", "/storage", "/sys"
    )
}
