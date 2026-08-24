package hu.muzso.android_system_dumper.usecase

class GetSeedPathsUseCase {
    @Suppress("SdCardPath")
    fun execute(): List<String> = listOf(
        "/", "/acct", "/apex", "/bin", "/bootstrap-apex", "/bugreports",
        "/cache", "/config", "/data", "/data-mirror", "/debug",
        "/debug_ramdisk", "/dev", "/dlkm", "/etc",
        "/init", "/init.bootstat.rc", "/init.car.rc", "/init.environ.rc",
        "/kernel", "/linkerconfig", "/lost+found", "/metadata", "/mnt", "/odm",
        "/odm_dlkm", "/oem", "/postinstall", "/proc", "/product", "/recovery",
        "/sdcard", "/second_stage_resources",
        "/storage", "/storage/emulated", "/storage/emulated/0",
        "/storage/emulated/1", "/storage/emulated/2", "/storage/emulated/3",
        "/storage/emulated/4", "/storage/emulated/5", "/storage/emulated/6",
        "/storage/emulated/7", "/storage/emulated/8", "/storage/emulated/9",
        "/storage/emulated/10", "/storage/emulated/11", "/storage/emulated/12",
        "/storage/emulated/13", "/storage/emulated/14", "/storage/emulated/15",
        "/storage/emulated/16", "/storage/emulated/17", "/storage/emulated/18",
        "/storage/emulated/19", "/storage/emulated/20",
        "/storage/self", "/system", "/system_dlkm", "/sys", "/system_ext",
        "/vendor", "/vendor_dlkm"
    )
}
