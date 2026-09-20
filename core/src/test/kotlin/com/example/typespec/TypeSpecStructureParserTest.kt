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
        assertEquals(
            listOf("namespace Demo", "model Pet", "interface HasName", "op listPets"),
            nodes.map { "${it.kind} ${it.name}" },
        )
    }

    @Test
    fun parseSkipsNestedDeclarations() {
        val text = """
            namespace Demo {
              model Pet { name: string; }
            }
            model Other {}
        """.trimIndent()

        val nodes = TypeSpecStructureParser.parse(text)
        assertEquals(listOf("namespace Demo", "model Other"), nodes.map { "${it.kind} ${it.name}" })
    }

    @Test
    fun parseIgnoresCommentsAndStrings() {
        val text = """
            // model NotThis {}
            /* op alsoNotThis(): void; */
            model Pet { name: "model NotThisEither"; }
        """.trimIndent()

        val nodes = TypeSpecStructureParser.parse(text)
        assertEquals(listOf("model Pet"), nodes.map { "${it.kind} ${it.name}" })
    }

    @Test
    fun blockCommentDoesNotMergeTokens() {
        val nodes = TypeSpecStructureParser.parse("model/*c*/Pet {}")
        assertEquals(listOf("model Pet"), nodes.map { "${it.kind} ${it.name}" })
    }

    @Test
    fun escapedQuoteDoesNotEndString() {
        val text = """model Pet { name: "a\"}"; }""" + "\nmodel Next {}"

        val nodes = TypeSpecStructureParser.parse(text)
        assertEquals(listOf("model Pet", "model Next"), nodes.map { "${it.kind} ${it.name}" })
    }

    @Test
    fun parseTracksLineNumbers() {
        val text = "namespace Demo;\n\nop listPets(): void;"

        val nodes = TypeSpecStructureParser.parse(text)
        assertEquals(0, nodes[0].line)
        assertEquals(2, nodes[1].line)
    }
}
