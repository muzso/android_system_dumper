package hu.muzso.android_system_dumper.scan

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes SELinux file context regular expressions and transforms them into fixed string path candidates.
 */
@Singleton
class SelinuxContextAnalyzer @Inject constructor() {

    /**
     * Extracts fixed string path candidates from an SELinux file context regex.
     *
     * @param regex The regex from the first column of a file_contexts file.
     * @return A list of fixed string path candidates.
     */
    fun extractPathCandidates(regex: String): List<String> {
        val truncated = truncate(regex)
        val expanded = expandRecursive(truncated)
        return expanded.map { unescape(it) }.filter { it.isNotEmpty() }.distinct()
    }

    private fun truncate(regex: String): String {
        var i = 0
        val sb = StringBuilder()
        while (i < regex.length) {
            val c = regex[i]
            val atom: String
            val nextPos: Int

            if (c == '\\') {
                atom = if (i + 1 < regex.length) regex.substring(i, i + 2) else c.toString()
                nextPos = i + atom.length
            } else if (c == '(') {
                var depth = 1
                var j = i + 1
                while (j < regex.length && depth > 0) {
                    if (regex[j] == '\\') { j += 2; continue }
                    if (regex[j] == '(') depth++
                    else if (regex[j] == ')') depth--
                    j++
                }
                atom = regex.substring(i, j)
                nextPos = j
            } else if (c == '[') {
                var j = i + 1
                while (j < regex.length && regex[j] != ']') {
                    if (regex[j] == '\\') { j += 2; continue }
                    j++
                }
                if (j < regex.length) j++
                atom = regex.substring(i, j)
                nextPos = j
            } else if (c == '.') {
                return sb.toString()
            } else {
                atom = c.toString()
                nextPos = i + 1
            }

            // Check for arbitrary quantifiers after the atom
            if (nextPos < regex.length) {
                val q = regex[nextPos]
                if (q == '*' || q == '+') {
                    return sb.toString()
                }
                if (q == '{') {
                    val end = regex.indexOf('}', nextPos)
                    if (end != -1 && regex.substring(nextPos + 1, end).contains(',')) {
                        return sb.toString()
                    }
                }
            }

            // Check if atom itself contains arbitrary expressions (if group)
            if (atom.startsWith("(") && !isClean(atom.substring(1, atom.length - 1))) {
                return sb.toString()
            }

            sb.append(atom)

            // Handle optionality '?'
            if (nextPos < regex.length && regex[nextPos] == '?') {
                sb.append('?')
                i = nextPos + 1
            } else {
                i = nextPos
            }
        }
        return sb.toString()
    }

    private fun isClean(regex: String): Boolean {
        var i = 0
        while (i < regex.length) {
            val c = regex[i]
            if (c == '\\') { i += 2; continue }
            if (c == '.' || c == '*' || c == '+') return false
            if (c == '{') {
                val end = regex.indexOf('}', i)
                if (end != -1 && regex.substring(i + 1, end).contains(',')) return false
            }
            if (c == '(') {
                var depth = 1
                var j = i + 1
                while (j < regex.length && depth > 0) {
                    if (regex[j] == '\\') { j += 2; continue }
                    if (regex[j] == '(') depth++
                    else if (regex[j] == ')') depth--
                    j++
                }
                if (!isClean(regex.substring(i + 1, j - 1))) return false
                i = j
                continue
            }
            if (c == '[') {
                i++
                while (i < regex.length && regex[i] != ']') {
                    if (regex[i] == '\\') { i += 2; continue }
                    i++
                }
            }
            i++
        }
        return true
    }

    private fun expandRecursive(regex: String): List<String> {
        if (regex.isEmpty()) return listOf("")

        // 1. Handle top-level alternation
        val parts = splitAlternation(regex)
        if (parts.size > 1) {
            return parts.flatMap { expandRecursive(it) }
        }

        // 2. Identify first atom
        val c = regex[0]
        val atom: String
        val nextPos: Int
        if (c == '\\') {
            atom = if (regex.length > 1) regex.substring(0, 2) else c.toString()
            nextPos = atom.length
        } else if (c == '(') {
            var depth = 1
            var j = 1
            while (j < regex.length && depth > 0) {
                if (regex[j] == '\\') { j += 2; continue }
                if (regex[j] == '(') depth++
                else if (regex[j] == ')') depth--
                j++
            }
            atom = regex.substring(0, j)
            nextPos = j
        } else if (c == '[') {
            var j = 1
            while (j < regex.length && regex[j] != ']') {
                if (regex[j] == '\\') { j += 2; continue }
                j++
            }
            if (j < regex.length) j++
            atom = regex.substring(0, j)
            nextPos = j
        } else {
            atom = c.toString()
            nextPos = 1
        }

        // 3. Check for quantifier '?'
        val hasQuantifier = nextPos < regex.length && regex[nextPos] == '?'
        val remainingStart = if (hasQuantifier) nextPos + 1 else nextPos

        // 4. Expand the atom
        val atomExpansions = when {
            atom.startsWith("(") -> expandRecursive(atom.substring(1, atom.length - 1))
            atom.startsWith("[") -> expandCharClass(atom.substring(1, atom.length - 1))
            else -> listOf(atom)
        }

        val choices = if (hasQuantifier) listOf("") + atomExpansions else atomExpansions

        // 5. Recursively expand the rest
        val suffixes = expandRecursive(regex.substring(remainingStart))

        // 6. Combine
        return choices.flatMap { p -> suffixes.map { s -> p + s } }
    }

    private fun splitAlternation(content: String): List<String> {
        val parts = mutableListOf<String>()
        var start = 0
        var depth = 0
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '\\') { i += 2; continue }
            if (c == '(') depth++
            else if (c == ')') depth--
            else if (c == '[') {
                i++
                while (i < content.length && content[i] != ']') {
                    if (content[i] == '\\') { i += 2; continue }
                    i++
                }
            } else if (c == '|' && depth == 0) {
                parts.add(content.substring(start, i))
                start = i + 1
            }
            i++
        }
        parts.add(content.substring(start))
        return parts
    }

    private fun expandCharClass(content: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < content.length) {
            if (i + 2 < content.length && content[i + 1] == '-') {
                val start = content[i]
                val end = content[i + 2]
                if (start <= end) {
                    for (c in start..end) {
                        result.add(c.toString())
                    }
                } else {
                    result.add(start.toString())
                    result.add("-")
                    result.add(end.toString())
                }
                i += 3
            } else {
                val c = content[i]
                if (c == '\\' && i + 1 < content.length) {
                    result.add(content.substring(i, i + 2))
                    i += 2
                } else {
                    result.add(c.toString())
                    i++
                }
            }
        }
        return if (result.isEmpty()) listOf("") else result
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length) {
                sb.append(s[i + 1])
                i += 2
            } else {
                sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }
}
