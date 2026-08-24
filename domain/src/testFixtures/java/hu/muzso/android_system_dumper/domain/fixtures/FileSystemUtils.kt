package hu.muzso.android_system_dumper.domain.fixtures

import hu.muzso.android_system_dumper.filesystem.FileSystem
import hu.muzso.android_system_dumper.model.DirEntry

/**
 * Generates a human-readable string representation of the filesystem hierarchy.
 * Useful for debugging and verifying filesystem state in tests.
 */
suspend fun FileSystem.dumpTree(startPath: String = "/"): String {
    val sb = StringBuilder()
    val visitedCanonicalDirs = mutableSetOf<String>()
    
    if (!exists(startPath)) {
        return "Path does not exist: $startPath"
    }

    dumpNode(startPath, "", sb, visitedCanonicalDirs)
    return sb.toString()
}

private suspend fun FileSystem.dumpNode(
    path: String,
    indent: String,
    sb: StringBuilder,
    visitedCanonicalDirs: MutableSet<String>
) {
    val name = getFileName(path).let { if (it.isEmpty() && path == "/") "/" else it }
    val type = when {
        isLink(path) -> DirEntry.TYPE_LINK
        isDirectory(path) -> DirEntry.TYPE_DIR
        else -> DirEntry.TYPE_FILE
    }

    val typeDesc = when (type) {
        DirEntry.TYPE_FILE -> "[FILE, ${size(path)}]"
        DirEntry.TYPE_LINK -> "[LINK] -> ${getCanonicalPath(path)}"
        else -> "[${DirEntry.decodeType(type)}]"
    }

    sb.append("${indent}${if (indent.isEmpty()) "" else "|-- "}$name $typeDesc\n")

    if (type == DirEntry.TYPE_DIR) {
        val canonical = getCanonicalPath(path)
        if (visitedCanonicalDirs.add(canonical)) {
            val children = list(path).sortedBy { it.name }
            val nextIndent = if (indent.isEmpty()) "  " else "$indent    "
            for (child in children) {
                dumpNode(join(path, child.name), nextIndent, sb, visitedCanonicalDirs)
            }
        } else {
            sb.append("$indent    |-- (already visited: $canonical)\n")
        }
    }
}

/**
 * Helper to check if a path is a symbolic link.
 * Since the FileSystem interface doesn't have an isLink method,
 * we infer it if getCanonicalPath returns something different from the input path,
 * OR we can just check if we can resolve it as a link in the specific implementation.
 * However, the most reliable way for this utility is to use the fact that 'list' returns DirEntry with type.
 * But dumpNode starts with a path.
 */
private suspend fun FileSystem.isLink(path: String): Boolean {
    // If it's the root, it's not a link in our current model.
    if (path == "/" || path.isEmpty()) return false
    
    val parent = getParent(path) ?: return false
    return list(parent).find { it.name == getFileName(path) }?.type == DirEntry.TYPE_LINK
}
