package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.platform.QrGenerator
import javax.inject.Inject

class GenerateQrUseCase @Inject constructor(
    private val qrGenerator: QrGenerator
) {
    fun execute(text: String, size: Int = 512): Any? {
        return qrGenerator.generateQrCode(text, size)
    }
}
