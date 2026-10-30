package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeSpecWorkspaceConfigurationTest {
    @Test
    fun emptySettingsOmitEmitAndEntrypoint() {
        assertEquals(emptyMap<String, Any>(), TypeSpecWorkspaceConfiguration.typespecSection(emptyList(), emptyList()))
        assertEquals(
            mapOf("typespec" to emptyMap<String, Any>()),
            TypeSpecWorkspaceConfiguration.changeNotificationSettings(emptyList(), emptyList()),
        )
    }

    @Test
    fun typespecSectionNestsEmitAndEntrypoint() {
        val section = TypeSpecWorkspaceConfiguration.typespecSection(
            lspEmit = listOf(" <config:defaults> ", "@typespec/openapi3", "<config:defaults>"),
            entrypoints = listOf(" client.tsp ", "main.tsp", ""),
        )

        assertEquals(
            mapOf(
                "lsp" to mapOf("emit" to listOf("<config:defaults>", "@typespec/openapi3")),
                "entrypoint" to listOf("client.tsp", "main.tsp"),
            ),
            section,
        )
    }

    @Test
    fun configDefaultsTokenIsPassedThroughUnchanged() {
        assertEquals("<config:defaults>", TypeSpecWorkspaceConfiguration.CONFIG_DEFAULTS)
        val section = TypeSpecWorkspaceConfiguration.typespecSection(
            listOf(TypeSpecWorkspaceConfiguration.CONFIG_DEFAULTS),
            emptyList(),
        )

        assertEquals(
            mapOf("lsp" to mapOf("emit" to listOf("<config:defaults>"))),
            section,
        )
    }

    @Test
    fun parseListSplitsCommaSemicolonAndNewlines() {
        assertEquals(emptyList<String>(), TypeSpecWorkspaceConfiguration.parseList(null))
        assertEquals(emptyList<String>(), TypeSpecWorkspaceConfiguration.parseList("  , ; \n"))
        assertEquals(
            listOf("<config:defaults>", "@typespec/openapi3", "client.tsp"),
            TypeSpecWorkspaceConfiguration.parseList(
                " <config:defaults> , @typespec/openapi3;\nclient.tsp, <config:defaults> ",
            ),
        )
    }

    @Test
    fun formatListCanonicalizesSpacingAndDuplicates() {
        assertEquals(
            "<config:defaults>, @typespec/openapi3",
            TypeSpecWorkspaceConfiguration.formatList(listOf(" <config:defaults> ", "@typespec/openapi3", "", " @typespec/openapi3 ")),
        )
        assertEquals("", TypeSpecWorkspaceConfiguration.formatList(emptyList()))
    }

    @Test
    fun typespecSectionAnswersEveryScope() {
        val emit = listOf("@typespec/openapi3")
        val entrypoints = listOf("main.tsp")
        val expected = TypeSpecWorkspaceConfiguration.Response.Value(
            mapOf(
                "lsp" to mapOf("emit" to emit),
                "entrypoint" to entrypoints,
            ),
        )
        val scopes = listOf(null, "", "file:///tmp/main.tsp", "not a uri", "file://[bad")

        for (scopeUri in scopes) {
            assertEquals(
                expected,
                TypeSpecWorkspaceConfiguration.responseFor("typespec", scopeUri, emit, entrypoints),
            )
        }
        assertEquals(
            expected,
            TypeSpecWorkspaceConfiguration.responseFor("  typespec  ", null, emit, entrypoints),
        )
    }

    @Test
    fun otherSectionsAreUnhandled() {
        val scopes = listOf<String?>(null, "", "file:///tmp/main.tsp")
        val sections = listOf(null, "", "   ", "typescript", "typespec.lsp.emit.extra", "typespec.formatting")

        for (section in sections) {
            for (scopeUri in scopes) {
                assertEquals(
                    TypeSpecWorkspaceConfiguration.Response.Unhandled,
                    TypeSpecWorkspaceConfiguration.responseFor(section, scopeUri, listOf("@typespec/openapi3"), listOf("main.tsp")),
                )
            }
        }
    }

    @Test
    fun dottedSectionsReturnTheMatchingSlice() {
        val emit = listOf("<config:defaults>", "@typespec/openapi3")
        val entrypoints = listOf("client.tsp", "main.tsp")

        assertEquals(
            TypeSpecWorkspaceConfiguration.Response.Value(mapOf("emit" to emit)),
            TypeSpecWorkspaceConfiguration.responseFor("typespec.lsp", null, emit, entrypoints),
        )
        assertEquals(
            TypeSpecWorkspaceConfiguration.Response.Value(emit),
            TypeSpecWorkspaceConfiguration.responseFor("typespec.lsp.emit", "file:///workspace/main.tsp", emit, entrypoints),
        )
        assertEquals(
            TypeSpecWorkspaceConfiguration.Response.Value(entrypoints),
            TypeSpecWorkspaceConfiguration.responseFor("typespec.entrypoint", null, emit, entrypoints),
        )
        assertEquals(
            TypeSpecWorkspaceConfiguration.Response.Value(null),
            TypeSpecWorkspaceConfiguration.responseFor("typespec.lsp.emit", null, emptyList(), entrypoints),
        )
        assertEquals(
            TypeSpecWorkspaceConfiguration.Response.Value(emptyMap<String, Any>()),
            TypeSpecWorkspaceConfiguration.responseFor("typespec.lsp", null, emptyList(), entrypoints),
        )
        assertEquals(
            TypeSpecWorkspaceConfiguration.Response.Value(null),
            TypeSpecWorkspaceConfiguration.responseFor("typespec.entrypoint", null, emit, emptyList()),
        )
    }

    @Test
    fun changeNotificationWrapsTheTypespecSection() {
        assertEquals(
            mapOf(
                "typespec" to mapOf(
                    "lsp" to mapOf("emit" to listOf("<config:defaults>")),
                    "entrypoint" to listOf("client.tsp"),
                ),
            ),
            TypeSpecWorkspaceConfiguration.changeNotificationSettings(
                listOf("<config:defaults>"),
                listOf("client.tsp"),
            ),
        )
    }
}
