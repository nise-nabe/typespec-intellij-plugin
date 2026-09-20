package com.example.typespec

internal data class TypeSpecStructureNode(
    val name: String,
    val kind: String,
    val line: Int,
    val children: List<TypeSpecStructureNode> = emptyList(),
)

internal object TypeSpecStructureParser {
    private val declarationPattern = Regex(
        """^(namespace|model|interface|enum|union|alias|op|using)\s+([A-Za-z_][\w.]*)""",
    )

    fun parse(text: String): List<TypeSpecStructureNode> {
        val nodes = mutableListOf<TypeSpecStructureNode>()
        var depth = 0
        var inBlockComment = false
        for ((index, rawLine) in text.lines().withIndex()) {
            val stripped = stripCommentsAndStrings(rawLine, inBlockComment)
            inBlockComment = stripped.inBlockComment
            val clean = stripped.text

            var i = 0
            while (i < clean.length && (clean[i].isWhitespace() || clean[i] == '}')) {
                if (clean[i] == '}') {
                    depth--
                }
                i++
            }
            if (depth == 0) {
                declarationPattern.find(clean.substring(i))?.let { match ->
                    nodes += TypeSpecStructureNode(
                        name = match.groupValues[2],
                        kind = match.groupValues[1],
                        line = index,
                    )
                }
            }
            for (j in i until clean.length) {
                when (clean[j]) {
                    '{' -> depth++
                    '}' -> depth--
                }
            }
            if (depth < 0) {
                depth = 0
            }
        }
        return nodes
    }

    private data class StrippedLine(val text: String, val inBlockComment: Boolean)

    private fun stripCommentsAndStrings(line: String, inBlockComment: Boolean): StrippedLine {
        val result = StringBuilder(line.length)
        var comment = inBlockComment
        var i = 0
        while (i < line.length) {
            when {
                comment -> {
                    if (line.startsWith("*/", i)) {
                        comment = false
                        i += 2
                    } else {
                        i++
                    }
                }
                line.startsWith("//", i) -> i = line.length
                line.startsWith("/*", i) -> {
                    comment = true
                    i += 2
                }
                line[i] == '"' -> {
                    val end = line.indexOf('"', i + 1)
                    i = if (end < 0) line.length else end + 1
                    result.append(' ')
                }
                else -> {
                    result.append(line[i])
                    i++
                }
            }
        }
        return StrippedLine(result.toString(), comment)
    }
}
