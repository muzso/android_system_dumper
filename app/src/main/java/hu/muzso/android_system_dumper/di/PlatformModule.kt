package hu.muzso.android_system_dumper.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.muzso.android_system_dumper.common.DefaultNetworkUtils
import hu.muzso.android_system_dumper.common.DefaultPlatformUtils
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.platform.AndroidAppServiceManager
import hu.muzso.android_system_dumper.platform.AndroidResourceProvider
import hu.muzso.android_system_dumper.platform.AndroidSystemInfo
import hu.muzso.android_system_dumper.platform.AndroidUiMessenger
import hu.muzso.android_system_dumper.platform.AppServiceManager
import hu.muzso.android_system_dumper.platform.JniNativeBridge
import hu.muzso.android_system_dumper.platform.NativeBridge
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.platform.UiMessenger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    @Singleton
    abstract fun bindResourceProvider(impl: AndroidResourceProvider): ResourceProvider

    @Binds
    @Singleton
    abstract fun bindSystemInfo(impl: AndroidSystemInfo): SystemInfo

    @Binds
    @Singleton
    abstract fun bindUiMessenger(impl: AndroidUiMessenger): UiMessenger

    @Binds
    @Singleton
    abstract fun bindAppServiceManager(impl: AndroidAppServiceManager): AppServiceManager

    @Binds
    @Singleton
    abstract fun bindNativeBridge(impl: JniNativeBridge): NativeBridge

    @Binds
    @Singleton
    abstract fun bindPlatformUtils(impl: DefaultPlatformUtils): PlatformUtils

    @Binds
    @Singleton
    abstract fun bindNetworkUtils(impl: DefaultNetworkUtils): NetworkUtils
}
