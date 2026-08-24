package hu.muzso.android_system_dumper.scan

import hu.muzso.android_system_dumper.common.DispatcherProvider
import hu.muzso.android_system_dumper.domain.fixtures.FakeClock
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import hu.muzso.android_system_dumper.domain.fixtures.FakeMemoryFileSystem
import hu.muzso.android_system_dumper.domain.repository.scan.ScanRepositoryContract
import hu.muzso.android_system_dumper.usecase.GetScanRootUseCase
import hu.muzso.android_system_dumper.usecase.GetSeedPathsUseCase
import hu.muzso.android_system_dumper.usecase.LoadExcludeListUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultScanRepositoryContractTest : ScanRepositoryContract() {
    private val fakeMemoryFileSystem = FakeMemoryFileSystem()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override fun main(): CoroutineDispatcher = testDispatcher
        override fun default(): CoroutineDispatcher = testDispatcher
        override fun io(): CoroutineDispatcher = testDispatcher
        override fun unconfined(): CoroutineDispatcher = testDispatcher
    }

    override fun createScanRepository(seedPaths: List<String>, excludeList: List<String>): ScanRepository {
        val loadExcludeListUseCase = mockk<LoadExcludeListUseCase>()
        val getSeedPathsUseCase = mockk<GetSeedPathsUseCase>()
        val getScanRootUseCase = mockk<GetScanRootUseCase>()

        every { loadExcludeListUseCase.execute() } returns excludeList
        every { getSeedPathsUseCase.execute() } returns seedPaths
        every { getScanRootUseCase.execute() } returns "/"

        return DefaultScanRepository(
            fileSystem = fakeMemoryFileSystem,
            collector = DefaultFileCollector(),
            metadataCollector = mockk(relaxed = true),
            logger = FakeFileLogger(),
            clock = FakeClock(),
            loadExcludeListUseCase = loadExcludeListUseCase,
            getSeedPathsUseCase = getSeedPathsUseCase,
            getScanRootUseCase = getScanRootUseCase,
            dispatcherProvider = dispatcherProvider
        )
    }
}
