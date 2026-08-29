package hu.muzso.android_system_dumper.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import hu.muzso.android_system_dumper.network.upload.DefaultUploadExecutor
import hu.muzso.android_system_dumper.network.upload.DefaultUploadProgressTracker
import hu.muzso.android_system_dumper.network.upload.DefaultUploadRepositoryManager
import hu.muzso.android_system_dumper.network.upload.DefaultUploadRetryPolicy
import hu.muzso.android_system_dumper.network.upload.DefaultUploadSelector
import hu.muzso.android_system_dumper.network.upload.FilebinUploadRepository
import hu.muzso.android_system_dumper.network.upload.GofileUploadRepository
import hu.muzso.android_system_dumper.network.upload.UploadExecutor
import hu.muzso.android_system_dumper.network.upload.UploadProgressTracker
import hu.muzso.android_system_dumper.network.upload.UploadRepository
import hu.muzso.android_system_dumper.network.upload.UploadRepositoryManager
import hu.muzso.android_system_dumper.network.upload.UploadRetryPolicy
import hu.muzso.android_system_dumper.network.upload.UploadSelector
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadRepositoryModule {

    @Binds
    @IntoMap
    @StringKey("gofile.io")
    @Singleton
    abstract fun bindGofileRepository(impl: GofileUploadRepository): UploadRepository

    @Binds
    @IntoMap
    @StringKey("filebin.net")
    @Singleton
    abstract fun bindFilebinRepository(impl: FilebinUploadRepository): UploadRepository

    @Binds
    @Singleton
    abstract fun bindUploadRepositoryManager(impl: DefaultUploadRepositoryManager): UploadRepositoryManager

    @Binds
    @Singleton
    abstract fun bindUploadSelector(impl: DefaultUploadSelector): UploadSelector

    @Binds
    @Singleton
    abstract fun bindUploadExecutor(impl: DefaultUploadExecutor): UploadExecutor

    @Binds
    @Singleton
    abstract fun bindUploadRetryPolicy(impl: DefaultUploadRetryPolicy): UploadRetryPolicy

    @Binds
    @Singleton
    abstract fun bindUploadProgressTracker(impl: DefaultUploadProgressTracker): UploadProgressTracker
}
