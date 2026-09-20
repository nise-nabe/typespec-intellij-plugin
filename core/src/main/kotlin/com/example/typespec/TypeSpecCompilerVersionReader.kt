package com.example.typespec

import java.nio.file.Files
import java.nio.file.Path

object TypeSpecCompilerVersionReader {
    fun readPackageVersion(packageDirectory: Path): String? {
        val packageJson = packageDirectory.resolve("package.json")
        if (!Files.isRegularFile(packageJson)) {
            return null
        }
        val text = try {
            Files.readString(packageJson)
        } catch (e: Exception) {
            return null
        }
        return findTopLevelStringValue(text, "version")
    }

    /** Returns the value of a top-level string property of a JSON object, or null. */
    internal fun findTopLevelStringValue(text: String, key: String): String? {
        var depth = 0
        var i = 0
        while (i < text.length) {
            when (text[i]) {
                '"' -> {
                    val end = text.indexOf('"', i + 1)
                    if (end < 0) {
                        return null
                    }
                    val literal = text.substring(i + 1, end)
                    var j = end + 1
                    while (j < text.length && text[j].isWhitespace()) {
                        j++
                    }
                    if (depth == 1 && literal == key && j < text.length && text[j] == ':') {
                        return readStringValue(text, j + 1)
                    }
                    i = end + 1
                }
                '{', '[' -> {
                    depth++
                    i++
                }
                '}', ']' -> {
                    depth--
                    i++
                }
                else -> i++
            }
        }
        return null
    }

    private fun readStringValue(text: String, from: Int): String? {
        var i = from
        while (i < text.length && text[i].isWhitespace()) {
            i++
        }
        if (i >= text.length || text[i] != '"') {
            return null
        }
        val end = text.indexOf('"', i + 1)
        return if (end > i) text.substring(i + 1, end) else null
    }
}
