package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.ZipFileEntry
import hu.muzso.android_system_dumper.model.ZipOptions
import hu.muzso.android_system_dumper.scan.ArchiveRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Date

class CreateArchiveUseCaseTest {

    private val archiveRepository = mockk<ArchiveRepository>()
    private val platformUtils = mockk<PlatformUtils>()
    private val useCase = CreateArchiveUseCase(archiveRepository, platformUtils)

    @Test
    fun `execute calls archiveRepository with correct options`() {
        runBlocking {
            val files = listOf(ZipFileEntry("path", "name"))
            val outputFilePath = "out.zip"
            val encryption = ZipEncryption.AES
            val password = "pass".toCharArray()
            val options = ZipOptions(outputFilePath, encryption, password)
            coEvery { archiveRepository.createArchive(files, options, any()) } returns DomainResult.Success(outputFilePath)

            val result = useCase.execute(files, options, false)

            assertThat(result).isEqualTo(DomainResult.Success(outputFilePath))
            coEvery {
                archiveRepository.createArchive(files, options, any())
            }
        }
    }

    @Test
    fun `generatePassword calls platformUtils`() {
        every { platformUtils.generateSecureRandomString(16) } returns "random"
        val result = useCase.generatePassword(16)
        assertThat(result).isEqualTo("random")
    }

    @Test
    fun `generateBatchFilename calls platformUtils`() {
        val date = Date()
        every { platformUtils.makeFilename(date, 1, 2) } returns "file.zip"
        val result = useCase.generateBatchFilename(date, 1, 2)
        assertThat(result).isEqualTo("file.zip")
    }
}
