package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.scan.MetadataCollector

class FakeMetadataCollector(private val fileSystem: FileSystem? = null) : MetadataCollector {
    private val _processedPaths = mutableListOf<String>()
    val processedPaths: List<String> get() = _processedPaths

    private val triggerMap = mutableMapOf<String, List<String>>()

    fun setTrigger(path: String, newPaths: List<String>) {
        triggerMap[path] = newPaths
    }

    override fun isMetadataFile(path: String): Boolean {
        return triggerMap.containsKey(path)
    }

    override suspend fun processMetadata(path: String, onNewPathFound: suspend (path: String, source: String) -> Unit) {
        val pathStr = fileSystem?.getCanonicalPath(path) ?: path
        _processedPaths.add(pathStr)
        triggerMap[pathStr]?.forEach {
            onNewPathFound(it, "fake metadata analysis of $pathStr")
        }
    }
}
