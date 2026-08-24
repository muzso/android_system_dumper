package hu.muzso.android_system_dumper.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.DefaultClock
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.filesystem.SystemFileSystem
import hu.muzso.android_system_dumper.repository.DefaultIpInfoRepository
import hu.muzso.android_system_dumper.repository.DefaultSettingsRepository
import hu.muzso.android_system_dumper.repository.IpInfoRepository
import hu.muzso.android_system_dumper.repository.SettingsRepository
import hu.muzso.android_system_dumper.scan.ArchiveRepository
import hu.muzso.android_system_dumper.scan.DefaultArchiveRepository
import hu.muzso.android_system_dumper.scan.DefaultFileCollector
import hu.muzso.android_system_dumper.scan.DefaultMetadataCollector
import hu.muzso.android_system_dumper.scan.DefaultScanRepository
import hu.muzso.android_system_dumper.scan.FileCollector
import hu.muzso.android_system_dumper.scan.MetadataCollector
import hu.muzso.android_system_dumper.scan.ScanRepository
import hu.muzso.android_system_dumper.zip.Zip4jZipCreator
import hu.muzso.android_system_dumper.zip.ZipCreator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindClock(impl: DefaultClock): Clock

    @Binds
    @Singleton
    abstract fun bindSettings(impl: DefaultSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(impl: DefaultScanRepository): ScanRepository

    @Binds
    @Singleton
    abstract fun bindFileSystem(impl: SystemFileSystem): FileSystem

    @Binds
    @Singleton
    abstract fun bindZipCreator(impl: Zip4jZipCreator): ZipCreator

    @Binds
    @Singleton
    abstract fun bindFileCollector(impl: DefaultFileCollector): FileCollector

    @Binds
    @Singleton
    abstract fun bindMetadataCollector(impl: DefaultMetadataCollector): MetadataCollector

    @Binds
    @Singleton
    abstract fun bindArchiveRepository(impl: DefaultArchiveRepository): ArchiveRepository

    @Binds
    @Singleton
    abstract fun bindIpInfoRepository(impl: DefaultIpInfoRepository): IpInfoRepository
}
