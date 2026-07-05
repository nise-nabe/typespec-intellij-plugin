package com.example.typespec.inspections

internal data class TypeSpecTspConfigFinding(
    val messageKey: String,
    val severity: TypeSpecFindingSeverity,
)

internal object TypeSpecTspConfigRules {
    fun evaluate(text: String): List<TypeSpecTspConfigFinding> {
        val findings = mutableListOf<TypeSpecTspConfigFinding>()
        val hasKindProject = containsKeyValue(text, "kind", "project")
        val hasEntrypoint = text.contains(Regex("""^\s*entrypoint\s*:""", RegexOption.MULTILINE))
        val hasEmit = text.contains(Regex("""^\s*emit\s*:""", RegexOption.MULTILINE))
        val hasFeatures = text.contains(Regex("""^\s*features\s*:""", RegexOption.MULTILINE))

        if (hasKindProject && !hasEntrypoint) {
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
        val pattern = Regex("""^\s*$key\s*:\s*["']?$value["']?\s*$""", RegexOption.MULTILINE)
        return pattern.containsMatchIn(text)
    }
}
