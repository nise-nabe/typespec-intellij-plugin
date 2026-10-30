package com.example.typespec.actions

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeSpecCreateProjectDialogTest {
    @Test
    fun offeredTemplatesMatchCompilerScaffoldingKeys() {
        assertEquals("empty", TypeSpecCreateProjectDialog.DEFAULT_TEMPLATE)
        assertEquals(
            listOf("empty", "rest", "library-ts", "emitter-ts"),
            TypeSpecCreateProjectDialog.TEMPLATES,
        )
    }

    @Test
    fun buildInitArgsPassesSelectedTemplate() {
        assertEquals(
            listOf("init", "--template", "empty"),
            buildTypeSpecInitArgs(template = "empty"),
        )
        assertEquals(
            listOf("init", "--template", "rest", "-y"),
            buildTypeSpecInitArgs(template = "rest", autoAccept = true),
        )
    }

    @Test
    fun buildInitArgsPrefersExternalTemplateUrl() {
        assertEquals(
            listOf("init", "https://example.com/templates", "-y"),
            buildTypeSpecInitArgs(
                template = "rest",
                templateUrl = "  https://example.com/templates  ",
                autoAccept = true,
            ),
        )
    }
}
