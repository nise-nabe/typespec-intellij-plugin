package com.example.typespec.workflow

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeSpecTspConfigReaderTest {
    @Test
    fun parseEmittersReadsListUnderEmitKey() {
        val yaml = """
            emit:
              - "@typespec/openapi3"
              - "@typespec/json-schema"
            options:
              "@typespec/openapi3":
                emitter-output-dir: "{output-dir}"
        """.trimIndent()

        assertEquals(
            listOf("@typespec/openapi3", "@typespec/json-schema"),
            TypeSpecTspConfigReader.parseEmitters(yaml),
        )
    }

    @Test
    fun parseEmittersReadsInlineEmitValue() {
        val yaml = """
            emit: "@typespec/openapi3"
        """.trimIndent()

        assertEquals(listOf("@typespec/openapi3"), TypeSpecTspConfigReader.parseEmitters(yaml))
    }
}
