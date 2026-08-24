package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.zip.ZipCreator
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.ceil

class DefaultArchiveRepositoryTest {

    private val zipCreator = mockk<ZipCreator>()
    private val fileSystem = FakeMemoryFileSystem()
    private lateinit var repository: DefaultArchiveRepository

    @Before
    fun setup() {
        repository = DefaultArchiveRepository(zipCreator, fileSystem)
    }

    @Test
    fun `createArchive delegates to zipCreator`() = runTest {
        val files = listOf(ZipFileEntry("/path", "name"))
        val options = ZipOptions("/out.zip", ZipEncryption.NONE)
        coEvery { zipCreator.create(files, options, any()) } returns DomainResult.Success("/out.zip")

        val result = repository.createArchive(files, options, false)

        assertEquals(DomainResult.Success("/out.zip"), result)
    }

    @Test
    fun `cleanupArchives deletes existing files`() = runTest {
        val path1 = "/archive1.zip"
        val path2 = "/archive2.zip"
        fileSystem.writeText(path1, "content1")
        fileSystem.writeText(path2, "content2")

        assertTrue(fileSystem.exists(path1))
        assertTrue(fileSystem.exists(path2))

        repository.cleanupArchives(listOf(path1, path2))

        assertFalse(fileSystem.exists(path1))
        assertFalse(fileSystem.exists(path2))
    }

    @Test
    fun `cleanupArchives ignores non-existent files`() = runTest {
        val path = "/non_existent.zip"
        assertFalse(fileSystem.exists(path))

        repository.cleanupArchives(listOf(path))

        assertFalse(fileSystem.exists(path))
    }

    @Test
    fun `createArchive returns InsufficientSpace when storage is full`() = runTest {
        val files = listOf(ZipFileEntry("/path1", "name1"), ZipFileEntry("/path2", "name2"))
        fileSystem.addFileWithText("/path1", "content1")
        fileSystem.addFileWithText("/path2", "content2")
        val options = ZipOptions("/out.zip", ZipEncryption.NONE)

        coEvery { zipCreator.create(files, options, any()) } returns DomainResult.Error(ZipError.IOException("No space left on device"))

        val result = repository.createArchive(files, options, false)

        assertTrue(result is DomainResult.Error)
        val error = (result as DomainResult.Error).error
        assertTrue(error is ZipError.InsufficientSpace)

        val totalSize = "content1".length.toLong() + "content2".length.toLong()
        val expectedMinSize = ceil(totalSize * 1.00008).toLong() + 1024L * (1L + files.size)
        assertEquals(expectedMinSize, (error as ZipError.InsufficientSpace).requiredBytes)
    }
}
