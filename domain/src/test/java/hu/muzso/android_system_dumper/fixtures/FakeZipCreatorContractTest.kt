package hu.muzso.android_system_dumper.fixtures

import hu.muzso.android_system_dumper.domain.fixtures.FakeDispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeZipCreator
import hu.muzso.android_system_dumper.domain.repository.zip.ZipCreatorContract
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.zip.ZipCreator

class FakeZipCreatorContractTest : ZipCreatorContract() {
    override fun createZipCreator(filesystem: FileSystem, dispatcherProvider: FakeDispatcherProvider): ZipCreator = FakeZipCreator(filesystem)
}
