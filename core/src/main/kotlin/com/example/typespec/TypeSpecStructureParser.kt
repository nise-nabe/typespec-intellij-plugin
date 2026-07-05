package com.example.typespec

internal data class TypeSpecStructureNode(
    val name: String,
    val kind: String,
    val children: List<TypeSpecStructureNode> = emptyList(),
)

internal object TypeSpecStructureParser {
    private val declarationPattern = Regex(
        """^\s*(namespace|model|interface|enum|union|alias|op|using)\s+([A-Za-z_][\w.]*)""",
    )

    fun parse(text: String): List<TypeSpecStructureNode> {
        val nodes = mutableListOf<TypeSpecStructureNode>()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                continue
            }
            val match = declarationPattern.find(trimmed) ?: continue
            val kind = match.groupValues[1]
            val name = match.groupValues[2]
            nodes += TypeSpecStructureNode(name = name, kind = kind)
        }
        return nodes
    }
}
