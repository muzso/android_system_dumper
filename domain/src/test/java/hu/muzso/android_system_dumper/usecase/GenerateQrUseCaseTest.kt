package hu.muzso.android_system_dumper.usecase

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.platform.QrGenerator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class GenerateQrUseCaseTest {

    @Test
    fun `GenerateQrUseCase calls QrGenerator with custom size`() {
        val qrGenerator = mockk<QrGenerator>()
        val useCase = GenerateQrUseCase(qrGenerator)
        val dummyQr = Any()
        every { qrGenerator.generateQrCode(any(), any()) } returns dummyQr

        val result = useCase.execute("text", 256)
        assertThat(result).isEqualTo(dummyQr)
        verify { qrGenerator.generateQrCode("text", 256) }
    }

    @Test
    fun `GenerateQrUseCase calls QrGenerator with default size`() {
        val qrGenerator = mockk<QrGenerator>()
        val useCase = GenerateQrUseCase(qrGenerator)
        val dummyQr = Any()
        every { qrGenerator.generateQrCode(any(), any()) } returns dummyQr

        val result = useCase.execute("text")
        assertThat(result).isEqualTo(dummyQr)
        verify { qrGenerator.generateQrCode("text", 512) }
    }
}
