package com.example.typespec.highlighting

/**
 * TypeSpec keyword set aligned with `@typespec/compiler` 1.17.0 `Keywords` in
 * `packages/compiler/src/core/scanner.ts` (including reserved keywords the
 * compiler still tokenizes as keywords). The tokenized set is unchanged from
 * 1.13.0; `auto` is a modifier keyword as of 1.14.0 and was already listed here.
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
        "declare", "array", "struct", "record", "module", "mod", "sym",
        "context", "prop", "property", "scenario", "pub", "sub", "typeref",
        "trait", "this", "self", "super", "keyof", "with", "implements",
        "impl", "satisfies", "flag", "partial", "private", "public",
        "protected", "sealed", "local", "async",
    )
}
