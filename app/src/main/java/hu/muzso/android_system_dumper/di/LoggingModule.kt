package hu.muzso.android_system_dumper.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hu.muzso.android_system_dumper.config.AppConfig
import hu.muzso.android_system_dumper.logging.AndroidFileLogger
import hu.muzso.android_system_dumper.logging.AndroidSystemLogSink
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.logging.SystemLogSink
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LogFilePath

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LogToSystem

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Binds
    @Singleton
    abstract fun bindFileLogger(impl: AndroidFileLogger): FileLogger

    @Binds
    @Singleton
    abstract fun bindSystemLogSink(impl: AndroidSystemLogSink): SystemLogSink

    companion object {
        @Provides
        @LogFilePath
        fun provideLogFilePath(@ApplicationContext context: Context): String {
            return "${context.cacheDir}/logs.txt"
        }

        @Provides
        @LogToSystem
        fun provideLogToSystem(appConfig: AppConfig): Boolean = appConfig.logToSystem
    }
}
