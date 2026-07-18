package com.example.typespec.highlighting

/**
 * TypeSpec keyword set aligned with `@typespec/compiler` `Keywords` in
 * `packages/compiler/src/core/scanner.ts` (including reserved keywords the
 * compiler still tokenizes as keywords).
 */
internal object TypeSpecKeywords {
    val KEYWORDS = setOf(
        "import", "model", "scalar", "namespace", "interface", "union",
        "if", "else", "projection", "using", "op", "extends", "is", "enum",
        "alias", "dec", "fn", "valueof", "typeof", "const", "init",
        "true", "false", "return", "void", "never", "unknown", "extern",
        "auto", "internal",
        // Reserved keywords (compiler still highlights/tokenizes them)
        "statemachine", "macro", "package", "metadata", "env", "arg",
        "declare", "array",
    )
}
