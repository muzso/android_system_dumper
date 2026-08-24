package hu.muzso.android_system_dumper.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.fixtures.FakeScanRepository
import hu.muzso.android_system_dumper.domain.fixtures.FakeUploadRepository
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.scan.ScanRepository
import hu.muzso.android_system_dumper.upload.network.UploadRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestAppModule {

    @Provides
    @Singleton
    fun provideFileSystem(): FileSystem = FakeMemoryFileSystem()

    @Provides
    @Singleton
    fun provideScanRepository(): ScanRepository = FakeScanRepository()

    @Provides
    @IntoMap
    @StringKey("dummy")
    @Singleton
    fun provideDummyRepository(): UploadRepository = FakeUploadRepository()
}
