package hu.muzso.android_system_dumper.zip

import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.repository.zip.ZipCreatorContract
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import io.mockk.mockk

class Zip4jZipCreatorContractTest : ZipCreatorContract() {
    private val logger = mockk<FileLogger>(relaxed = true)

    override fun createZipCreator(filesystem: FileSystem, dispatcherProvider: FakeDispatcherProvider): ZipCreator {
        return Zip4jZipCreator(logger, filesystem, dispatcherProvider)
    }
}
