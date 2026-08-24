package hu.muzso.android_system_dumper.common

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultClock @Inject constructor() : Clock {
    override fun now(): Instant = Instant.now()
    override fun monotonicTime(): Long = System.nanoTime()
}
