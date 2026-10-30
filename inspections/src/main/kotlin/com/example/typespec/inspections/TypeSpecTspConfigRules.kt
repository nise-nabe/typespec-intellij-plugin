package com.example.typespec.inspections

internal data class TypeSpecTspConfigFinding(
    val messageKey: String,
    val severity: TypeSpecFindingSeverity,
)

/**
 * Files next to tspconfig.yaml that can supply an entrypoint when `entrypoint` is omitted.
 *
 * @typespec/compiler 1.17.0 (`resolveTypeSpecEntrypointForDir`) uses, in order,
 * `package.json` `exports["."].typespec`, then `tspMain`, then `main.tsp`.
 */
internal data class TypeSpecTspConfigProjectHints(
    val typespecExport: String? = null,
    val tspMain: String? = null,
    val mainTspExists: Boolean = false,
) {
    val resolvesEntrypoint: Boolean
        get() = !typespecExport.isNullOrBlank() || !tspMain.isNullOrBlank() || mainTspExists
}

internal object TypeSpecTspConfigRules {
    fun evaluate(
        text: String,
        hints: TypeSpecTspConfigProjectHints = TypeSpecTspConfigProjectHints(),
    ): List<TypeSpecTspConfigFinding> {
        val findings = mutableListOf<TypeSpecTspConfigFinding>()
        val hasKindProject = containsKeyValue(text, "kind", "project")
        val hasEntrypoint = text.contains(Regex("""^\s*entrypoint\s*:""", RegexOption.MULTILINE))
        val hasEmit = text.contains(Regex("""^\s*emit\s*:""", RegexOption.MULTILINE))
        val hasFeatures = text.contains(Regex("""^\s*features\s*:""", RegexOption.MULTILINE))

        if (hasKindProject && !hasEntrypoint && !hints.resolvesEntrypoint) {
            findings += TypeSpecTspConfigFinding(
                "inspection.tcfg001",
                TypeSpecFindingSeverity.WARNING,
            )
        }
        if (hasFeatures && !hasKindProject) {
            findings += TypeSpecTspConfigFinding(
                "inspection.tcfg002",
                TypeSpecFindingSeverity.WARNING,
            )
        }
        if (!hasEmit) {
            findings += TypeSpecTspConfigFinding(
                "inspection.tcfg003",
                TypeSpecFindingSeverity.INFORMATION,
            )
        }
        return findings
    }

    private fun containsKeyValue(text: String, key: String, value: String): Boolean {
        val pattern = Regex(
            """^\s*${Regex.escape(key)}\s*:\s*["']?${Regex.escape(value)}["']?\s*(?:#.*)?$""",
            RegexOption.MULTILINE,
        )
        return pattern.containsMatchIn(text)
    }
}
