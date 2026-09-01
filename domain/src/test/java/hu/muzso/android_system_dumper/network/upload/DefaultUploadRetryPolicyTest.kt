package hu.muzso.android_system_dumper.network.upload

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUploadRetryPolicyTest {

    private val logger = FakeFileLogger()
    private lateinit var retryPolicy: DefaultUploadRetryPolicy

    @Before
    fun setup() {
        retryPolicy = DefaultUploadRetryPolicy(logger)
    }

    @Test
    fun `withRetry executes block successfully`() = runTest {
        val result = retryPolicy.withRetry(
            label = "Test",
            retries = 3,
            onStatusUpdate = { _, _, _ -> },
            onFailure = { _, _ -> }
        ) {
            "success"
        }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun `withRetry retries on failure and eventually succeeds`() = runTest {
        val attempts = AtomicInteger(0)
        val result = retryPolicy.withRetry(
            label = "Test",
            retries = 3,
            onStatusUpdate = { _, _, _ -> },
            onFailure = { _, _ -> }
        ) {
            if (attempts.incrementAndGet() < 3) {
                throw IOException("Transient error")
            }
            "success"
        }

        assertThat(result).isEqualTo("success")
        assertThat(attempts.get()).isEqualTo(3)
    }

    @Test
    fun `withRetry calls onFailure on each failure`() = runTest {
        val failureAttempts = mutableListOf<Int>()
        val attempts = AtomicInteger(0)
        
        try {
            retryPolicy.withRetry(
                label = "Test",
                retries = 2,
                onStatusUpdate = { _, _, _ -> },
                onFailure = { attempt, _ -> failureAttempts.add(attempt) }
            ) {
                attempts.incrementAndGet()
                throw IOException("Persistent error")
            }
        } catch (_: Exception) {
            // Expected
        }

        assertThat(attempts.get()).isEqualTo(2)
        assertThat(failureAttempts).containsExactly(1, 2).inOrder()
    }

    @Test(expected = IOException::class)
    fun `withRetry throws last exception after exhausting retries`() = runTest {
        retryPolicy.withRetry(
            label = "Test",
            retries = 2,
            onStatusUpdate = { _, _, _ -> },
            onFailure = { _, _ -> }
        ) {
            throw IOException("Persistent error")
        }
    }

    @Test
    fun `withRetry retries indefinitely when retries is 0`() = runTest {
        val attempts = AtomicInteger(0)
        val result = retryPolicy.withRetry(
            label = "Test",
            retries = 0,
            onStatusUpdate = { _, _, _ -> },
            onFailure = { _, _ -> }
        ) {
            if (attempts.incrementAndGet() < 100) { // arbitrary high number
                throw IOException("Transient error")
            }
            "success"
        }

        assertThat(result).isEqualTo("success")
        assertThat(attempts.get()).isEqualTo(100)
    }

    @Test
    fun `withRetry retries indefinitely when retries is negative`() = runTest {
        val attempts = AtomicInteger(0)
        val result = retryPolicy.withRetry(
            label = "Test",
            retries = -1,
            onStatusUpdate = { _, _, _ -> },
            onFailure = { _, _ -> }
        ) {
            if (attempts.incrementAndGet() < 50) {
                throw IOException("Transient error")
            }
            "success"
        }

        assertThat(result).isEqualTo("success")
        assertThat(attempts.get()).isEqualTo(50)
    }
}
