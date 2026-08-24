package hu.muzso.android_system_dumper.scan

interface MetadataCollector {
    /**
     * Checks if the given path points to a metadata file that should be processed.
     */
    fun isMetadataFile(path: String): Boolean

    /**
     * Processes the metadata file at the given path and reports discovered file paths.
     */
    suspend fun processMetadata(path: String, onNewPathFound: suspend (path: String, source: String) -> Unit)
}
