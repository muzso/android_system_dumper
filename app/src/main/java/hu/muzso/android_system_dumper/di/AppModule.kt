package hu.muzso.android_system_dumper.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.muzso.android_system_dumper.common.DefaultDispatcherProvider
import hu.muzso.android_system_dumper.common.DefaultRandomProvider
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.common.RandomProvider
import hu.muzso.android_system_dumper.platform.AndroidXmlParser
import hu.muzso.android_system_dumper.platform.CustomTorServiceController
import hu.muzso.android_system_dumper.platform.DefaultQrGenerator
import hu.muzso.android_system_dumper.platform.QrGenerator
import hu.muzso.android_system_dumper.platform.TorServiceController
import hu.muzso.android_system_dumper.platform.XmlParser
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindXmlParser(impl: AndroidXmlParser): XmlParser

    @Binds
    @Singleton
    abstract fun bindTorService(impl: CustomTorServiceController): TorServiceController

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindQrGenerator(impl: DefaultQrGenerator): QrGenerator

    @Binds
    @Singleton
    abstract fun bindRandomProvider(impl: DefaultRandomProvider): RandomProvider
}
