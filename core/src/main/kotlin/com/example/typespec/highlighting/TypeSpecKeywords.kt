package com.example.typespec.highlighting

internal object TypeSpecKeywords {
    val KEYWORDS = setOf(
        "namespace", "model", "interface", "enum", "union", "alias", "op", "using",
        "import", "extends", "is", "scalar", "void", "never", "unknown", "true", "false",
        "dec", "fn", "extern", "project", "function",
    )

    operator fun contains(word: String): Boolean = KEYWORDS.contains(word)
}
