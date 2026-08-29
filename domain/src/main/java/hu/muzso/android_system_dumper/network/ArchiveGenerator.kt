package hu.muzso.android_system_dumper.network

import hu.muzso.android_system_dumper.model.DomainResult
import hu.muzso.android_system_dumper.model.ScanResult
import hu.muzso.android_system_dumper.model.ZipError
import hu.muzso.android_system_dumper.model.upload.UploadParameters

/**
 * Interface for generating ZIP archives from scan results.
 * This encapsulates the logic for batching files and creating encrypted archives.
 */
interface ArchiveGenerator {
    /**
     * Prepares the generator with the given parameters and scan results.
     */
    fun prepare(parameters: UploadParameters, scanResult: ScanResult)

    /**
     * Returns the total number of data batches.
     */
    fun getBatchCount(): Int

    /**
     * Generates a ZIP archive for the specified batch index (1-based).
     * @return A [DomainResult] containing the path to the generated ZIP file and its display name.
     */
    suspend fun generateBatch(index: Int): DomainResult<GeneratedZip, ZipError>

    /**
     * Generates the miscellaneous ZIP archive (logs, lists, etc.).
     * @return A [DomainResult] containing the path to the generated ZIP file and its display name.
     */
    suspend fun generateMisc(): DomainResult<GeneratedZip, ZipError>

    /**
     * Returns true if a miscellaneous ZIP should be generated based on the parameters.
     */
    fun shouldGenerateMisc(): Boolean

    /**
     * Returns the display filename for the specified batch index.
     */
    fun getBatchFilename(index: Int): String

    /**
     * Returns the display filename for the miscellaneous ZIP.
     */
    fun getMiscZipFilename(): String

    /**
     * Returns the passphrase used for ZIP encryption, if any.
     */
    fun getEncryptionPassphrase(): String?

    /**
     * Cleans up any temporary files generated.
     */
    suspend fun cleanup(path: String)
}

data class GeneratedZip(val path: String, val filename: String)
