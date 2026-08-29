package hu.muzso.android_system_dumper.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelinuxContextAnalyzerTest {

    private val analyzer = SelinuxContextAnalyzer()

    @Test
    fun `extractPathCandidates handles arbitrary length truncation - example 1`() {
        val input = "/sys/devices/virtual/graphics/fb([0-3])+/msm_fb_type"
        val expected = listOf("/sys/devices/virtual/graphics/fb")
        assertEquals(expected, analyzer.extractPathCandidates(input))
    }

    @Test
    fun `extractPathCandidates handles arbitrary length truncation - example 2`() {
        val input = "/(vendor|system/vendor)/bin/hw/android\\.hardware\\.drm@[0-9]+\\.[0-9]+-service.widevine"
        val results = analyzer.extractPathCandidates(input)
        assertTrue(results.contains("/vendor/bin/hw/android.hardware.drm@"))
        assertTrue(results.contains("/system/vendor/bin/hw/android.hardware.drm@"))
        assertEquals(2, results.size)
    }

    @Test
    fun `extractPathCandidates handles arbitrary length truncation - example 3`() {
        val input = "/(odm|vendor/odm)/etc(/.*)?"
        val results = analyzer.extractPathCandidates(input)
        assertTrue(results.contains("/odm/etc"))
        assertTrue(results.contains("/vendor/odm/etc"))
        assertEquals(2, results.size)
    }

    @Test
    fun `extractPathCandidates handles expansion - example 4`() {
        val input = "/(odm|vendor/odm)/bin/sh"
        val expected = listOf("/odm/bin/sh", "/vendor/odm/bin/sh")
        val results = analyzer.extractPathCandidates(input)
        assertEquals(expected.sorted(), results.sorted())
    }

    @Test
    fun `extractPathCandidates handles expansion - example 5`() {
        val input = "/(vendor|system/vendor)/lib(64)?"
        val expected = listOf(
            "/vendor/lib",
            "/vendor/lib64",
            "/system/vendor/lib",
            "/system/vendor/lib64"
        )
        val results = analyzer.extractPathCandidates(input)
        assertEquals(expected.sorted(), results.sorted())
    }

    @Test
    fun `extractPathCandidates handles escaped dots`() {
        val input = "/system/etc/hosts\\.bak"
        val expected = listOf("/system/etc/hosts.bak")
        assertEquals(expected, analyzer.extractPathCandidates(input))
    }

    @Test
    fun `extractPathCandidates handles char class expansion`() {
        val input = "/dev/tty[0-2]"
        val expected = listOf("/dev/tty0", "/dev/tty1", "/dev/tty2")
        val results = analyzer.extractPathCandidates(input)
        assertEquals(expected.sorted(), results.sorted())
    }

    @Test
    fun `extractPathCandidates truncates on unescaped dot`() {
        val input = "/dev/bus/usb/.*"
        val expected = listOf("/dev/bus/usb/")
        assertEquals(expected, analyzer.extractPathCandidates(input))
    }

    @Test
    fun `extractPathCandidates handles nested groups`() {
        // /(system(/vendor)?|vendor)/bin/.*
        val input = "/(system(/vendor)?|vendor)/bin/.*"
        val results = analyzer.extractPathCandidates(input)
        val expected = listOf(
            "/system/bin/",
            "/system/vendor/bin/",
            "/vendor/bin/"
        )
        assertEquals(expected.sorted(), results.sorted())
    }

    @Test
    fun `extractPathCandidates handles complex character classes`() {
        val input = "/dev/block/platform/soc/[a-c0-9]+\\.sdhci/mmc_host"
        val results = analyzer.extractPathCandidates(input)
        // Truncates at unescaped dot (or in this case, the char class expansion + escaped dot)
        // Actually, SelinuxContextAnalyzer truncates at '.' if not escaped.
        // Let's see how it handles [a-c0-9]+
        // truncate() returns sb.toString() when it sees '+'.
        assertTrue(results.any { it.startsWith("/dev/block/platform/soc/") })
    }

    @Test
    fun `extractPathCandidates handles multiple alternations and optionality`() {
        val input = "/(system|vendor)/etc/(permissions|sysconfig)/.*\\.xml"
        val results = analyzer.extractPathCandidates(input)
        val expected = listOf(
            "/system/etc/permissions/",
            "/system/etc/sysconfig/",
            "/vendor/etc/permissions/",
            "/vendor/etc/sysconfig/"
        )
        assertEquals(expected.sorted(), results.sorted())
    }
}
