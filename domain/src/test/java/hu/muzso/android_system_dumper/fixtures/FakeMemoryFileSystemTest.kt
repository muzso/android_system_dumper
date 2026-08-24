package hu.muzso.android_system_dumper.fixtures

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class FakeMemoryFileSystemTest {

    @Test
    fun `appendText appends to existing file`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.writeText("/test.txt", "Hello")
        fs.appendText("/test.txt", " World")
        
        fs.openInputStream("/test.txt").use {
            assertThat(it.readBytes().decodeToString()).isEqualTo("Hello World")
        }
    }

    @Test
    fun `appendText creates new file if not exists`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.appendText("/new.txt", "New")
        
        fs.openInputStream("/new.txt").use {
            assertThat(it.readBytes().decodeToString()).isEqualTo("New")
        }
    }

    @Test
    fun `symlink resolution works with multi-step paths`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.writeText("/target.txt", "content")
        fs.addSymlink("/link1", "/target.txt")
        fs.addSymlink("/link2", "/link1")
        
        fs.openInputStream("/link2").use {
            assertThat(it.readBytes().decodeToString()).isEqualTo("content")
        }
    }

    @Test
    fun `simulateNoSpaceError causes IOException`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.simulateNoSpaceError = true
        
        try {
            fs.openOutputStream("/test.txt")
            // fail if no exception
            assertThat(true).isFalse()
        } catch (e: IOException) {
            assertThat(e.message).contains("No space left on device")
        }
    }

    @Test
    fun `resolveSymlink handles broken links`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.addSymlink("/broken", "/missing")
        
        assertThat(fs.exists("/broken")).isFalse()
    }

    @Test
    fun `delete removes node from parent`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.writeText("/dir/test.txt", "content")
        assertThat(fs.list("/dir").map { it.name }).contains("test.txt")
        
        fs.delete("/dir/test.txt")
        assertThat(fs.list("/dir")).isEmpty()
    }

    @Test
    fun `createParentDirs handles complex paths`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.writeText("/a/b/c/d.txt", "data")
        assertThat(fs.exists("/a/b/c/d.txt")).isTrue()
    }

    @Test
    fun `getParent returns correct results`() = runTest {
        val fs = FakeMemoryFileSystem()
        assertThat(fs.getParent("/a/b/c")).isEqualTo("/a/b")
        assertThat(fs.getParent("/")).isNull()
    }

    @Test
    fun `resolveSymlink handles recursion limit`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.addSymlink("/link1", "/link2")
        fs.addSymlink("/link2", "/link1")
        
        assertThat(fs.exists("/link1")).isFalse() // Should detect cycle and return null or fail
    }

    @Test
    fun `openOutputStream with append true`() = runTest {
        val fs = FakeMemoryFileSystem()
        fs.writeText("/test.txt", "A")
        fs.openOutputStream("/test.txt", append = true).use {
            it.write("B".toByteArray())
        }
        val content = fs.openInputStream("/test.txt").bufferedReader().use { it.readText() }
        assertThat(content).isEqualTo("AB")
    }
}
