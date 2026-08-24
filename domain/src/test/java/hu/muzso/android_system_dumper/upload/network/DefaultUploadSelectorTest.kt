package hu.muzso.android_system_dumper.upload.network

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DefaultUploadSelectorTest {

    private val repo1 = mockk<UploadRepository>()
    private val repo2 = mockk<UploadRepository>()
    private val repositories = mapOf("id1" to repo1, "id2" to repo2)
    private val settingsRepository = mockk<SettingsRepository>()
    private val selector = DefaultUploadSelector(repositories, settingsRepository)

    @Test
    fun `getRepositories returns all values`() {
        assertThat(selector.getRepositories()).containsExactly(repo1, repo2)
    }

    @Test
    fun `getSelectedRepository returns selected repo from settings`() {
        every { settingsRepository.getSelectedUploadServiceId() } returns "id2"
        assertThat(selector.getSelectedRepository()).isEqualTo(repo2)
    }

    @Test
    fun `getSelectedRepository returns first repo if selected not found`() {
        every { settingsRepository.getSelectedUploadServiceId() } returns "unknown"
        assertThat(selector.getSelectedRepository()).isEqualTo(repo1)
    }

    @Test
    fun `selectRepository updates settings`() {
        every { settingsRepository.setSelectedUploadServiceId(any()) } returns Unit
        selector.selectRepository("id2")
        verify { settingsRepository.setSelectedUploadServiceId("id2") }
    }
}
