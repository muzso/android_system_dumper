package hu.muzso.android_system_dumper.fixtures

import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.repository.filesystem.FileSystemContract
import hu.muzso.android_system_dumper.filesystem.FileSystem

class FakeFileSystemContractTest : FileSystemContract() {
    private val fakeMemoryFileSystem = FakeMemoryFileSystem()
    override fun createFileSystem(): FileSystem = fakeMemoryFileSystem
    
    override fun createDirectory(path: String) {
        fakeMemoryFileSystem.addDir(path)
    }
}
