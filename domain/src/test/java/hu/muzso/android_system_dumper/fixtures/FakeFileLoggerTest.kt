package hu.muzso.android_system_dumper.fixtures

import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class FakeFileLoggerTest {

    @Test
    fun `assertLogExists passes when log is present`() {
        val logger = FakeFileLogger()
        logger.i("Tag", "Message")
        logger.assertLogExists("I", "Tag", "Message")
    }

    @Test
    fun `assertLogExists throws when log is missing`() {
        val logger = FakeFileLogger()
        logger.i("Tag", "Message")
        assertThrows<AssertionError> {
            logger.assertLogExists("I", "Tag", "Wrong Message")
        }
        assertThrows<AssertionError> {
            logger.assertLogExists("I", "Wrong Tag", "Message")
        }
        assertThrows<AssertionError> {
            logger.assertLogExists("E", "Tag", "Message")
        }
    }

    @Test
    fun `assertErrorLogExists passes when error log is present`() {
        val logger = FakeFileLogger()
        logger.e("Tag", "Error", RuntimeException("test"))
        logger.assertErrorLogExists("Tag", "Error", RuntimeException::class.java)
    }

    @Test
    fun `assertErrorLogExists throws when exception type mismatch`() {
        val logger = FakeFileLogger()
        logger.e("Tag", "Error", RuntimeException("test"))
        assertThrows<AssertionError> {
            logger.assertErrorLogExists("Tag", "Error", IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `assertWarningLogExists passes when warning log is present`() {
        val logger = FakeFileLogger()
        logger.w("Tag", "Warn")
        logger.assertWarningLogExists("Tag", "Warn")
    }

    @Test
    fun `assertWarningLogExists throws when missing`() {
        val logger = FakeFileLogger()
        assertThrows<AssertionError> {
            logger.assertWarningLogExists("Tag", "Missing")
        }
    }
}
