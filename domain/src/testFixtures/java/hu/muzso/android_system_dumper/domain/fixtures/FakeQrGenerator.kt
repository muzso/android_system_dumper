package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.platform.QrGenerator

class FakeQrGenerator : QrGenerator {
    override fun generateQrCode(text: String, size: Int): Any {
        return text
    }
}
