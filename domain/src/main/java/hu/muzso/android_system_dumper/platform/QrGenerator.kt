package hu.muzso.android_system_dumper.platform

interface QrGenerator {
    // We can't use Bitmap here. We can return the BitMatrix as a 2D boolean array or just keep it abstract.
    // Or we can return a platform-specific object as 'Any' or a generic.
    // For now, let's just say it returns 'Any?' and the app casts it.
    fun generateQrCode(text: String, size: Int = 512): Any?
}
