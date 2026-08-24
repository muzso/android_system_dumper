package hu.muzso.android_system_dumper.common

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.random.asKotlinRandom

@Singleton
class DefaultRandomProvider @Inject constructor() : RandomProvider {
    private val secureRandom = SecureRandom().asKotlinRandom()
    override fun getRandom(): Random = secureRandom
}