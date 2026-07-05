package com.example.typespec.inspections

import org.junit.jupiter.api.Assertions.assertEquals
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

        val keys = TypeSpecTspConfigRules.evaluate(yaml).map { it.messageKey }
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
}
