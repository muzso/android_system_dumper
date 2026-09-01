package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.model.ZipEncryption
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.platform.ResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.net.Proxy

class ValidateUploadUseCaseTest {

    private val resourceProvider = mockk<ResourceProvider>()
    private val networkUtils = mockk<NetworkUtils>()
    private val uploadRepository = mockk<UploadRepository>()
    private lateinit var useCase: ValidateUploadUseCase

    @Before
    fun setup() {
        useCase = ValidateUploadUseCase(resourceProvider, networkUtils)
        every { resourceProvider.getMinBatchSizeMb() } returns 1
        every { resourceProvider.getMaxBatchSizeMb() } returns 100
        every { networkUtils.getProxyFromSpecification("localhost:8080") } returns mockk<Proxy>()
        every { networkUtils.getProxyFromSpecification(not("localhost:8080")) } returns null
    }

    @Test
    fun `execute returns Success when parameters are valid`() {
        val parameters = createValidParameters()
        every { networkUtils.getProxyFromSpecification("localhost:8080") } returns mockk<Proxy>()

        val result = useCase.execute(parameters)

        assertThat(result).isEqualTo(ValidateUploadUseCase.ValidationResult.Success)
    }

    @Test
    fun `execute returns Success for boundary batch sizes`() {
        assertThat(useCase.execute(createValidParameters().copy(customBatchSizeMb = 1)))
            .isEqualTo(ValidateUploadUseCase.ValidationResult.Success)
        assertThat(useCase.execute(createValidParameters().copy(customBatchSizeMb = 100)))
            .isEqualTo(ValidateUploadUseCase.ValidationResult.Success)
    }

    @Test
    fun `execute returns InvalidBatchSize when batch size is too small or too large`() {
        // Too small
        assertThat(useCase.execute(createValidParameters().copy(customBatchSizeMb = 0)))
            .isInstanceOf(ValidateUploadUseCase.ValidationResult.Error.InvalidBatchSize::class.java)
        
        // Too large
        assertThat(useCase.execute(createValidParameters().copy(customBatchSizeMb = 101)))
            .isInstanceOf(ValidateUploadUseCase.ValidationResult.Error.InvalidBatchSize::class.java)
    }

    @Test
    fun `execute returns InvalidProxy when proxy spec is invalid`() {
        val parameters = createValidParameters().copy(proxySpecification = "invalid")
        every { networkUtils.getProxyFromSpecification("invalid") } returns null

        val result = useCase.execute(parameters)

        assertThat(result).isInstanceOf(ValidateUploadUseCase.ValidationResult.Error.InvalidProxy::class.java)
        assertThat((result as ValidateUploadUseCase.ValidationResult.Error.InvalidProxy).spec).isEqualTo("invalid")
    }

    @Test
    fun `execute returns NoUploadSelected when nothing is selected for upload`() {
        val parameters = createValidParameters().copy(
            shouldUploadZips = false,
            shouldUploadFileLists = false
        )

        val result = useCase.execute(parameters)

        assertThat(result).isEqualTo(ValidateUploadUseCase.ValidationResult.Error.NoUploadSelected)
    }

    private fun createValidParameters() = UploadParameters(
        customBatchSizeMb = 10,
        proxySpecification = "localhost:8080",
        shouldUseTor = false,
        shouldUploadZips = true,
        shouldUploadFileLists = true,
        shouldUploadGetprop = true,
        shouldUploadAppLogs = true,
        maxUploadRetries = 5,
        zipEncryption = ZipEncryption.NONE,
        selectedService = uploadRepository,
        maxBatches = 0,
        useDoubleZipping = false
    )
}
