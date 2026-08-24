package hu.muzso.android_system_dumper.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.muzso.android_system_dumper.common.Clock
import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.common.PlatformUtils
import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.logging.FileLogger
import hu.muzso.android_system_dumper.platform.QrGenerator
import hu.muzso.android_system_dumper.platform.ResourceProvider
import hu.muzso.android_system_dumper.platform.SystemInfo
import hu.muzso.android_system_dumper.platform.TorServiceController
import hu.muzso.android_system_dumper.scan.ArchiveRepository
import hu.muzso.android_system_dumper.scan.ScanRepository
import hu.muzso.android_system_dumper.upload.BatchingLogic
import hu.muzso.android_system_dumper.upload.network.TorChecker
import hu.muzso.android_system_dumper.upload.network.UploadExecutor
import hu.muzso.android_system_dumper.upload.network.UploadProgressTracker
import hu.muzso.android_system_dumper.upload.network.UploadRetryPolicy
import hu.muzso.android_system_dumper.usecase.BatchFilesUseCase
import hu.muzso.android_system_dumper.usecase.CalculateStatisticsUseCase
import hu.muzso.android_system_dumper.usecase.CancelScanUseCase
import hu.muzso.android_system_dumper.usecase.CleanupUseCase
import hu.muzso.android_system_dumper.usecase.ClearScanResultsUseCase
import hu.muzso.android_system_dumper.usecase.CreateArchiveUseCase
import hu.muzso.android_system_dumper.usecase.CreateZipUseCase
import hu.muzso.android_system_dumper.usecase.GenerateQrUseCase
import hu.muzso.android_system_dumper.usecase.GetScanRootUseCase
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import hu.muzso.android_system_dumper.usecase.ScanSystemUseCase
import hu.muzso.android_system_dumper.usecase.StartScanUseCase
import hu.muzso.android_system_dumper.usecase.UploadArchiveUseCase
import hu.muzso.android_system_dumper.usecase.UploadBatchUseCase
import hu.muzso.android_system_dumper.usecase.ValidateUploadUseCase
import hu.muzso.android_system_dumper.zip.ZipCreator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideLoadExcludeListUseCase() = LoadExcludeListUseCase()

    @Provides
    @Singleton
    fun provideGetSeedPathsUseCase() = GetSeedPathsUseCase()

    @Provides
    @Singleton
    fun provideGetScanRootUseCase() = GetScanRootUseCase()

    @Provides
    @Singleton
    fun provideStartScanUseCase(
        repository: ScanRepository
    ) = StartScanUseCase(repository)

    @Provides
    @Singleton
    fun provideCancelScanUseCase() = CancelScanUseCase()

    @Provides
    @Singleton
    fun provideClearScanResultsUseCase(repository: ScanRepository) = ClearScanResultsUseCase(repository)

    @Provides
    @Singleton
    fun provideCalculateStatisticsUseCase(repository: ScanRepository) = CalculateStatisticsUseCase(repository)

    @Provides
    @Singleton
    fun provideBatchingLogic(logger: FileLogger) = BatchingLogic(logger)

    @Provides
    @Singleton
    fun provideBatchFilesUseCase(batchingLogic: BatchingLogic) = BatchFilesUseCase(batchingLogic)

    @Provides
    @Singleton
    fun provideCreateZipUseCase(zipCreator: ZipCreator) = CreateZipUseCase(zipCreator)

    @Provides
    @Singleton
    fun provideCleanupUseCase(fileSystem: FileSystem) = CleanupUseCase(fileSystem)

    @Provides
    @Singleton
    fun provideUploadBatchUseCase(
        torServiceController: TorServiceController,
        torChecker: TorChecker,
        logger: FileLogger,
        executor: UploadExecutor,
        retryPolicy: UploadRetryPolicy
    ) = UploadBatchUseCase(torServiceController, torChecker, logger, executor, retryPolicy)

    @Provides
    @Singleton
    fun provideScanSystemUseCase(
        startScanUseCase: StartScanUseCase,
        cancelScanUseCase: CancelScanUseCase,
        clearScanResultsUseCase: ClearScanResultsUseCase,
        calculateStatisticsUseCase: CalculateStatisticsUseCase,
        getSeedPathsUseCase: GetSeedPathsUseCase
    ) = ScanSystemUseCase(
        startScanUseCase, cancelScanUseCase, clearScanResultsUseCase,
        calculateStatisticsUseCase, getSeedPathsUseCase
    )

    @Provides
    @Singleton
    fun provideCreateArchiveUseCase(
        archiveRepository: ArchiveRepository,
        platformUtils: PlatformUtils
    ) = CreateArchiveUseCase(archiveRepository, platformUtils)

    @Provides
    @Singleton
    fun provideUploadArchiveUseCase(
        fileSystem: FileSystem,
        clock: Clock,
        logger: FileLogger,
        systemInfo: SystemInfo,
        batchFilesUseCase: BatchFilesUseCase,
        createArchiveUseCase: CreateArchiveUseCase,
        uploadBatchUseCase: UploadBatchUseCase,
        cleanupUseCase: CleanupUseCase,
        resourceProvider: ResourceProvider,
        progressTracker: UploadProgressTracker,
        dispatcherProvider: DispatcherProvider
    ) = UploadArchiveUseCase(
        fileSystem, clock, logger, systemInfo, batchFilesUseCase,
        createArchiveUseCase, uploadBatchUseCase, cleanupUseCase, resourceProvider,
        progressTracker, dispatcherProvider
    )

    @Provides
    @Singleton
    fun provideValidateUploadUseCase(
        resourceProvider: ResourceProvider,
        networkUtils: NetworkUtils
    ) = ValidateUploadUseCase(resourceProvider, networkUtils)

    @Provides
    @Singleton
    fun provideGenerateQrUseCase(qrGenerator: QrGenerator) = GenerateQrUseCase(qrGenerator)
}
