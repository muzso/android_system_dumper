package hu.muzso.android_system_dumper.common

import kotlin.random.Random

interface RandomProvider {
    fun getRandom(): Random
}
