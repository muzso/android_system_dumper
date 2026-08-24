package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.common.Clock
import java.time.Instant

class FakeClock(
    private var now: Instant = Instant.EPOCH,
    private var monotonicNow: Long = 0L
) : Clock {
    override fun now(): Instant = now
    override fun monotonicTime(): Long = monotonicNow

    fun setNow(newNow: Instant) {
        now = newNow
    }

    fun setMonotonicTime(newMonotonicNow: Long) {
        monotonicNow = newMonotonicNow
    }

    fun tick(millis: Long) {
        now = now.plusMillis(millis)
        monotonicNow += millis * 1_000_000L
    }
}
