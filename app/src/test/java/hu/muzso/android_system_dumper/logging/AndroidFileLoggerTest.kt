package hu.muzso.android_system_dumper.logging

import com.google.common.truth.Truth
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class AndroidFileLoggerTest {

    private val logFilePath = "/logs/test_logs.txt"
    private val fileSystem = FakeMemoryFileSystem()
    private val systemLogSink = mockk<SystemLogSink>(relaxed = true)
    private val clock = mockk<Clock>(relaxed = true)

    @Before
    fun setup() {
        every { systemLogSink.getStackTraceString(any()) } returns "stacktrace"
        every { clock.now() } returns Instant.now()
    }

    @Test
    fun `v logs to both system and file when logToSystem is true`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Verbose message"

        logger.v(tag, msg)

        verify { systemLogSink.v(tag, msg) }
        assertTrue(fileSystem.exists(logFilePath))
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("V/$tag: $msg") == true)
    }

    @Test
    fun `d logs only to file when logToSystem is false`() = runTest {
        val logger = AndroidFileLogger(logFilePath, false, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Debug message"

        logger.d(tag, msg)

        verify(exactly = 0) { systemLogSink.d(any(), any()) }
        assertTrue(fileSystem.exists(logFilePath))
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("D/$tag: $msg") == true)
    }

    @Test
    fun `e with exception logs stack trace to file`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Error message"
        val exception = RuntimeException("Boom")

        logger.e(tag, msg, exception)

        verify { systemLogSink.e(tag, msg, exception) }
        assertTrue(fileSystem.exists(logFilePath))
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("E/$tag: $msg\nstacktrace") == true)
    }

    @Test
    fun `init deletes existing log file`() = runTest {
        fileSystem.writeText(logFilePath, "old logs")
        assertTrue(fileSystem.exists(logFilePath))

        AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)

        assertThat(fileSystem.nodes[logFilePath]?.content ?: "").doesNotContain("old logs")
    }

    @Test
    fun `i logs to both system and file when logToSystem is true`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Info message"

        logger.i(tag, msg)

        verify { systemLogSink.i(tag, msg) }
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("I/$tag: $msg") == true)
    }

    @Test
    fun `w logs to both system and file when logToSystem is true`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Warning message"

        logger.w(tag, msg)

        verify { systemLogSink.w(tag, msg) }
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("W/$tag: $msg") == true)
    }

    @Test
    fun `e logs to both system and file when logToSystem is true`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Error message"

        logger.e(tag, msg)

        verify { systemLogSink.e(tag, msg) }
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("E/$tag: $msg") == true)
    }

    @Test
    fun `v with exception logs stack trace to file`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Verbose message"
        val exception = RuntimeException("Boom")

        logger.v(tag, msg, exception)

        verify { systemLogSink.v(tag, msg, exception) }
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("V/$tag: $msg\nstacktrace") == true)
    }

    @Test
    fun `d with exception logs stack trace to file`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Debug message"
        val exception = RuntimeException("Boom")

        logger.d(tag, msg, exception)

        verify { systemLogSink.d(tag, msg, exception) }
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("D/$tag: $msg\nstacktrace") == true)
    }

    @Test
    fun `i with exception logs stack trace to file`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Info message"
        val exception = RuntimeException("Boom")

        logger.i(tag, msg, exception)

        verify { systemLogSink.i(tag, msg, exception) }
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("I/$tag: $msg\nstacktrace") == true)
    }

    @Test
    fun `w with exception logs stack trace to file`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val tag = "TestTag"
        val msg = "Warning message"
        val exception = RuntimeException("Boom")

        logger.w(tag, msg, exception)

        verify { systemLogSink.w(tag, msg, exception) }
        assertTrue(fileSystem.nodes[logFilePath]?.content?.contains("W/$tag: $msg\nstacktrace") == true)
    }

    @Test
    fun `logDirectoryContents logs files and directories recursively`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val dir = "/test_dir"
        val file1 = "$dir/file1.txt"
        val subDir = "$dir/subdir"
        val file2 = "$subDir/file2.txt"

        fileSystem.writeText(file1, "file1 content")
        fileSystem.writeText(file2, "file2 content")

        logger.logDirectoryContents(dir)

        val logContent = fileSystem.nodes[logFilePath]?.content ?: ""
        assertTrue(logContent.contains("logDirectoryContents(): starting to recurse into \"$dir\""))
        assertTrue(logContent.contains("logPathContents(): directory: $dir"))
        assertTrue(logContent.contains("logPathContents(): file: $file1"))
        assertTrue(logContent.contains("logPathContents(): directory: $subDir"))
        assertTrue(logContent.contains("logPathContents(): file: $file2"))
    }

    @Test
    fun `logDirectoryContents logs error if directory does not exist`() = runTest {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        val dir = "/non_existent"

        logger.logDirectoryContents(dir)

        val logContent = fileSystem.nodes[logFilePath]?.content ?: ""
        assertTrue(logContent.contains("E/Logger: logDirectoryContents(): missing: $dir"))
    }

    @Test
    fun `flush is a no-op`() {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        logger.flush()
        // No assertion needed as it's a no-op, just increasing coverage
    }

    @Test
    fun `getLogFilePath returns correct path`() {
        val logger = AndroidFileLogger(logFilePath, true, systemLogSink, clock, fileSystem)
        assertEquals(logFilePath, logger.getLogFilePath())
    }

    private fun assertTrue(condition: Boolean) {
        Assert.assertTrue(condition)
    }

    private fun assertThat(actual: String) = Truth.assertThat(actual)
}
