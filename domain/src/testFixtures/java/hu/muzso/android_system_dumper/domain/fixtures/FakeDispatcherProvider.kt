package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class FakeDispatcherProvider(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : DispatcherProvider {
    override fun main(): CoroutineDispatcher = testDispatcher
    override fun default(): CoroutineDispatcher = testDispatcher
    override fun io(): CoroutineDispatcher = testDispatcher
    override fun unconfined(): CoroutineDispatcher = UnconfinedTestDispatcher(testDispatcher.scheduler)
}
