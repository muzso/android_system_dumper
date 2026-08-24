package hu.muzso.android_system_dumper.presentation

import android.os.SystemClock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hu.muzso.android_system_dumper.R
import hu.muzso.android_system_dumper.theme.AndroidSystemDumperTheme
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale
import java.util.TimeZone

/**
 * A JUnit rule to set up a deterministic environment for Roborazzi screenshot tests.
 */
class RoborazziTestConfigRule : TestWatcher() {
    override fun starting(description: Description) {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        SystemClock.setCurrentTimeMillis(1_000_000_000L) // Fixed time
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
        System.setProperty("robolectric.graphicsMode", "NATIVE")
        // Disable animations
        System.setProperty("robolectric.animations.disabled", "true")
    }
}

private val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

private val DeterministicTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    )
    // Add other styles as needed, or map all to JetBrainsMono
)

@Composable
fun ScreenshotTestTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    AndroidSystemDumperTheme(darkTheme = darkTheme) {
        MaterialTheme(
            typography = DeterministicTypography,
            content = {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    content()
                }
            }
        )
    }
}
