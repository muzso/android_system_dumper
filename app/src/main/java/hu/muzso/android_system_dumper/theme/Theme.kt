package hu.muzso.android_system_dumper.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF00A3F9),
    onPrimary = Color(0xFF003563),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF57A8F2),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004A77),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFF9840F5),
    onTertiary = Color.Black,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7D0)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF0277C4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF2D6CA6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D36),
    tertiary = Color(0xFF631DAD),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF43474E)
  )

@Composable
fun AndroidSystemDumperTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
