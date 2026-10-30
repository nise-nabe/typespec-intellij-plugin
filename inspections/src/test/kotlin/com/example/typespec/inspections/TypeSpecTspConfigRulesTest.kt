package com.example.typespec.inspections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecTspConfigRulesTest {
    @Test
    fun warnsWhenProjectKindWithoutEntrypoint() {
        val yaml = """
            kind: project
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        val findings = TypeSpecTspConfigRules.evaluate(yaml)
        val missingEntrypoint = findings.single { it.messageKey == "inspection.tcfg001" }
        assertEquals(TypeSpecFindingSeverity.WARNING, missingEntrypoint.severity)
    }

    @Test
    fun projectKindWithoutEntrypointIsCleanWhenMainTspExists() {
        val yaml = """
            kind: project
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        val keys = TypeSpecTspConfigRules.evaluate(
            yaml,
            TypeSpecTspConfigProjectHints(mainTspExists = true),
        ).map { it.messageKey }
        assertFalse(keys.contains("inspection.tcfg001"))
    }

    @Test
    fun projectKindWithoutEntrypointIsCleanWhenTspMainIsSet() {
        val yaml = """
            kind: project
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        val keys = TypeSpecTspConfigRules.evaluate(
            yaml,
            TypeSpecTspConfigProjectHints(tspMain = "lib/main.tsp"),
        ).map { it.messageKey }
        assertFalse(keys.contains("inspection.tcfg001"))
    }

    @Test
    fun projectKindWithoutEntrypointIsCleanWhenTypespecExportIsSet() {
        val yaml = """
            kind: project
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        val keys = TypeSpecTspConfigRules.evaluate(
            yaml,
            TypeSpecTspConfigProjectHints(typespecExport = "./lib/main.tsp"),
        ).map { it.messageKey }
        assertFalse(keys.contains("inspection.tcfg001"))
    }

    @Test
    fun blankFallbackValuesDoNotResolveAnEntrypoint() {
        val yaml = """
            kind: project
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        val keys = TypeSpecTspConfigRules.evaluate(
            yaml,
            TypeSpecTspConfigProjectHints(typespecExport = "  ", tspMain = ""),
        ).map { it.messageKey }
        assertTrue(keys.contains("inspection.tcfg001"))
    }

    @Test
    fun warnsWhenFeaturesWithoutProjectKind() {
        val yaml = """
            features:
              functions: true
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        val keys = TypeSpecTspConfigRules.evaluate(yaml).map { it.messageKey }
        assertTrue(keys.contains("inspection.tcfg002"))
    }

    @Test
    fun hintsWhenEmitSectionMissing() {
        val yaml = """
            output-dir: tsp-output
        """.trimIndent()

        assertEquals(listOf("inspection.tcfg003"), TypeSpecTspConfigRules.evaluate(yaml).map { it.messageKey })
    }

    @Test
    fun projectKindWithInlineCommentIsDetected() {
        val yaml = """
            kind: project  # explicit project config
            entrypoint: main.tsp
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        assertTrue(TypeSpecTspConfigRules.evaluate(yaml).isEmpty())
    }

    @Test
    fun cleanConfigProducesNoFindings() {
        val yaml = """
            kind: project
            entrypoint: main.tsp
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        assertTrue(TypeSpecTspConfigRules.evaluate(yaml).isEmpty())
    }
}
