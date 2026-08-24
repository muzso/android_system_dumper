package hu.muzso.android_system_dumper.common

import java.time.Instant

interface Clock {
    fun now(): Instant
    fun monotonicTime(): Long
}
