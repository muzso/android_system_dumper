package hu.muzso.android_system_dumper.platform

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import hu.muzso.android_system_dumper.logging.FileLogger
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomTorServiceControllerTest {

    private lateinit var context: Context
    private val appServiceManager = mockk<AppServiceManager>(relaxed = true)
    private val logger = mockk<FileLogger>(relaxed = true)
    private lateinit var torService: CustomTorServiceController

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        torService = CustomTorServiceController(context, appServiceManager, logger)
    }

    @Test
    fun `rebuildCircuit calls startTorService with NEWNYM action`() = runTest {
        torService.rebuildCircuit()
        verify { appServiceManager.startTorService(CustomTorService.ACTION_NEWNYM) }
    }

    @Test
    fun `waitForCircuit returns true when circuit established broadcast is received`() = runTest {
        val waitJob = async {
            torService.waitForCircuit(5000)
        }

        testScheduler.runCurrent()

        // Simulate receiving the broadcast
        context.sendBroadcast(Intent(CustomTorService.ACTION_CIRCUIT_ESTABLISHED).apply {
            setPackage(context.packageName)
        })

        ShadowLooper.idleMainLooper()
        testScheduler.runCurrent()

        val result = waitJob.await()
        assertTrue(result)
    }

    @Test
    fun `waitForCircuit returns false on timeout`() = runTest {
        val result = torService.waitForCircuit(100)
        assertFalse(result)
    }

    @Test
    fun `restartTorService stops service, waits for stop, starts service and waits for circuit`() = runTest {
        val waitJob = async {
            torService.restartTorService(5000)
        }

        testScheduler.runCurrent()

        // Verify stop was called
        verify { appServiceManager.stopTorService() }

        // Simulate service stopped broadcast
        context.sendBroadcast(Intent(CustomTorService.ACTION_SERVICE_STOPPED).apply {
            setPackage(context.packageName)
        })

        ShadowLooper.idleMainLooper()
        testScheduler.runCurrent()

        // Verify start was called
        verify { appServiceManager.startTorService() }

        // Simulate circuit established broadcast
        context.sendBroadcast(Intent(CustomTorService.ACTION_CIRCUIT_ESTABLISHED).apply {
            setPackage(context.packageName)
        })

        ShadowLooper.idleMainLooper()
        testScheduler.runCurrent()

        val result = waitJob.await()
        assertTrue(result)
    }
}
