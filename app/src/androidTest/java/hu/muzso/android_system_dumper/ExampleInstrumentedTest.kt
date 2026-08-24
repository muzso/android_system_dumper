package hu.muzso.android_system_dumper

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

  @get:Rule
  var hiltRule = HiltAndroidRule(this)

  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("hu.muzso.android_system_dumper", appContext.packageName)
  }
}
