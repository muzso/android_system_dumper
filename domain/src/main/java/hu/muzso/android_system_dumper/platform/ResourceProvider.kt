package hu.muzso.android_system_dumper.platform

interface ResourceProvider {
    fun getMaxUploadRetries(): Int
    fun getMinBatchSizeMb(): Int
    fun getMaxBatchSizeMb(): Int
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg args: Any): String
}
