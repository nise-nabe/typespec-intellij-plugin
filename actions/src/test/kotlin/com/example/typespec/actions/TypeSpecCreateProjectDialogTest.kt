package com.example.typespec.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeSpecCreateProjectDialogTest {
    @Test
    fun offeredTemplatesMatchCompilerScaffoldingKeys() {
        assertEquals("empty", TypeSpecCreateProjectDialog.DEFAULT_TEMPLATE)
        assertEquals("rest", TypeSpecCreateProjectDialog.REST_TEMPLATE)
        assertEquals(
            listOf("empty", "rest", "library-ts", "emitter-ts"),
            TypeSpecCreateProjectDialog.TEMPLATES,
        )
        assertEquals("@typespec/openapi3", TypeSpecCreateProjectDialog.REST_SELECTED_EMITTER)
        assertEquals(
            listOf(
                "@typespec/http-client-csharp",
                "@typespec/http-client-java",
                "@typespec/http-client-js",
                "@typespec/http-client-python",
                "@typespec/http-server-csharp",
                "@typespec/http-server-js",
            ),
            TypeSpecCreateProjectDialog.REST_OPTIONAL_EMITTERS.map { it.packageName },
        )
    }

    @Test
    fun buildInitArgsAlwaysRunsNonInteractive() {
        assertEquals(
            listOf("init", "--template", "empty", "-y", "--project-name", "widget"),
            buildTypeSpecInitArgs(template = "empty", projectName = "widget"),
        )
        assertEquals(
            listOf(
                "init",
                "--template",
                "rest",
                "-y",
                "--project-name",
                "widget",
                "--template-emitters",
                "@typespec/http-client-js",
                "--template-emitters",
                "@typespec/http-client-python",
            ),
            buildTypeSpecInitArgs(
                template = " rest ",
                projectName = " widget ",
                emitters = listOf(
                    " @typespec/http-client-js ",
                    "@typespec/http-client-js",
                    "@typespec/http-client-python",
                ),
            ),
        )
    }

    @Test
    fun buildInitArgsPassesTemplateWithIndexUrl() {
        assertEquals(
            listOf(
                "init",
                "https://example.com/scaffolding.json",
                "--template",
                "rest",
                "-y",
                "--project-name",
                "service",
                "--template-emitters",
                "@typespec/http-client-js",
            ),
            buildTypeSpecInitArgs(
                template = "rest",
                templateUrl = "  https://example.com/scaffolding.json  ",
                projectName = "service",
                emitters = listOf("@typespec/http-client-js"),
            ),
        )
    }

    @Test
    fun buildInitArgsRejectsBlankTemplateAndProjectName() {
        val missingTemplate = assertThrows(IllegalArgumentException::class.java) {
            buildTypeSpecInitArgs(template = "  ", projectName = "service")
        }
        assertTrue(missingTemplate.message.orEmpty().contains("template"))

        val missingProjectName = assertThrows(IllegalArgumentException::class.java) {
            buildTypeSpecInitArgs(template = "empty", projectName = " ")
        }
        assertTrue(missingProjectName.message.orEmpty().contains("project name"))
    }

    @Test
    fun defaultProjectNameUsesTargetFolder() {
        assertEquals("service", defaultProjectName("/tmp/service"))
        assertEquals("service", defaultProjectName("  /tmp/service/  "))
        assertEquals("", defaultProjectName("   "))
        assertEquals("", defaultProjectName("/"))
    }

    @Test
    fun parseTemplateEmittersSplitsOnCommas() {
        assertEquals(
            listOf("@typespec/http-client-js", "@typespec/http-client-python"),
            parseTemplateEmitters(" @typespec/http-client-js, @typespec/http-client-python, "),
        )
        assertEquals(
            listOf("@typespec/http-client-js"),
            parseTemplateEmitters("@typespec/http-client-js,@typespec/http-client-js"),
        )
        assertEquals(emptyList<String>(), parseTemplateEmitters("  ,  "))
    }
}
