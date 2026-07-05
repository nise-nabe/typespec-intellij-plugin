package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeSpecStructureParserTest {
    @Test
    fun parseFindsTopLevelDeclarations() {
        val text = """
            namespace Demo;
            model Pet { name: string; }
            interface HasName { name: string; }
            op listPets(): Pet[];
        """.trimIndent()

        val nodes = TypeSpecStructureParser.parse(text)
        assertEquals(listOf("namespace Demo", "model Pet", "interface HasName", "op listPets"), nodes.map { "${it.kind} ${it.name}" })
    }
}
