package hu.muzso.android_system_dumper.platform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidResourceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ResourceProvider {
    override fun getMaxUploadRetries(): Int = context.resources.getInteger(R.integer.max_number_of_upload_retries)
    override fun getMinBatchSizeMb(): Int = context.resources.getInteger(R.integer.custom_batch_size_mb_min)
    override fun getMaxBatchSizeMb(): Int = context.resources.getInteger(R.integer.custom_batch_size_mb_max)
    override fun getString(resId: Int): String = context.getString(resId)
    override fun getString(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
