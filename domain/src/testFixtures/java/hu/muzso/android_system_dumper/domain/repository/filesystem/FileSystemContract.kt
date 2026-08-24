package hu.muzso.android_system_dumper.domain.repository.filesystem

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.filesystem.FileSystem
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

abstract class FileSystemContract {

    @get:Rule
    val tempFolder = TemporaryFolder()

    abstract fun createFileSystem(): FileSystem
    abstract fun createDirectory(path: String)

    @Test
    fun exists_returns_true_for_existing_file() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "test.txt")
        fs.writeText(path, "content")
        
        assertThat(fs.exists(path)).isTrue()
    }

    @Test
    fun exists_returns_false_for_non_existing_file() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "non-existent.txt")
        
        assertThat(fs.exists(path)).isFalse()
    }

    @Test
    fun size_returns_correct_file_size() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "test.txt")
        val content = "hello"
        fs.writeText(path, content)
        
        assertThat(fs.size(path)).isEqualTo(content.length.toLong())
    }

    @Test
    fun isDirectory_and_isFile_work_correctly() = runTest {
        val fs = createFileSystem()
        val filePath = fs.join(tempFolder.root.absolutePath, "test.txt")
        val dirPath = fs.join(tempFolder.root.absolutePath, "testDir")
        
        fs.writeText(filePath, "content")
        createDirectory(dirPath)
        
        assertThat(fs.isFile(filePath)).isTrue()
        assertThat(fs.isDirectory(filePath)).isFalse()
        assertThat(fs.isFile(dirPath)).isFalse()
        assertThat(fs.isDirectory(dirPath)).isTrue()
    }

    @Test
    fun writeText_and_openInputStream_work() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "write-test.txt")
        val content = "sample text"
        
        fs.writeText(path, content)
        
        val inputStream = fs.openInputStream(path)
        val readContent = inputStream.bufferedReader().use { it.readText() }
        assertThat(readContent).isEqualTo(content)
    }

    @Test
    fun delete_removes_file() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "delete-me.txt")
        fs.writeText(path, "content")
        
        val deleted = fs.delete(path)
        assertThat(deleted).isTrue()
        assertThat(fs.exists(path)).isFalse()
    }

    @Test
    fun list_returns_directory_entries() = runTest {
        val fs = createFileSystem()
        val path = tempFolder.root.absolutePath
        val file1 = fs.join(path, "file1.txt")
        val dir1 = fs.join(path, "dir1")
        
        fs.writeText(file1, "content")
        createDirectory(dir1)
        
        val entries = fs.list(path)
        assertThat(entries.map { it.name }).containsAtLeast("file1.txt", "dir1")
    }

    @Test
    fun lastModified_returns_reasonable_value() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "mod.txt")
        fs.writeText(path, "content")
        assertThat(fs.lastModified(path)).isGreaterThan(0L)
    }

    @Test
    fun getParent_returns_correct_parent() = runTest {
        val fs = createFileSystem()
        val parent = tempFolder.root.absolutePath
        val child = fs.join(parent, "child.txt")
        assertThat(fs.getParent(child)).isEqualTo(parent)
    }

    @Test
    fun getFileName_returns_correct_name() = runTest {
        val fs = createFileSystem()
        val name = "my_file.txt"
        val path = fs.join(tempFolder.root.absolutePath, name)
        assertThat(fs.getFileName(path)).isEqualTo(name)
    }

    @Test
    fun getCanonicalPath_returns_normalized_path() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "test.txt")
        assertThat(fs.getCanonicalPath(path)).isNotEmpty()
    }

    @Test
    fun appendText_appends_correctly() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "append.txt")
        fs.writeText(path, "Part 1")
        fs.appendText(path, " Part 2")
        val content = fs.openInputStream(path).bufferedReader().use { it.readText() }
        assertThat(content).isEqualTo("Part 1 Part 2")
    }

    @Test
    fun openOutputStream_works() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "stream.txt")
        fs.openOutputStream(path).use { it.write("stream".toByteArray()) }
        val content = fs.openInputStream(path).bufferedReader().use { it.readText() }
        assertThat(content).isEqualTo("stream")
    }

    @Test
    fun canRead_returns_true_for_readable_file() = runTest {
        val fs = createFileSystem()
        val path = fs.join(tempFolder.root.absolutePath, "readable.txt")
        fs.writeText(path, "content")
        assertThat(fs.canRead(path)).isTrue()
    }
}
