package hu.muzso.android_system_dumper.platform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.muzso.android_system_dumper.presentation.state.SettingsUiState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidResourceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ResourceProvider {
    override fun getMinBatchSizeMb(): Int = SettingsUiState.CUSTOM_BATCH_SIZE_MB_MIN
    override fun getMaxBatchSizeMb(): Int = SettingsUiState.CUSTOM_BATCH_SIZE_MB_MAX
    override fun getString(resId: Int): String = context.getString(resId)
    override fun getString(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
