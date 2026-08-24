package hu.muzso.android_system_dumper.platform

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidUiMessenger @Inject constructor(
    @ApplicationContext private val context: Context
) : UiMessenger {
    override fun showShortToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
