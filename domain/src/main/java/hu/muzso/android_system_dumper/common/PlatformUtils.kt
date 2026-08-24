package hu.muzso.android_system_dumper.common

import java.util.Date

interface PlatformUtils {
    fun formatBytes(bytes: Long): String
    fun generateSecureRandomString(length: Int): String
    fun makeBinName(): String
    fun formatDate2Filename(date: Date): String
    fun makeFilename(date: Date, sequence: Int, digits: Int): String
}
