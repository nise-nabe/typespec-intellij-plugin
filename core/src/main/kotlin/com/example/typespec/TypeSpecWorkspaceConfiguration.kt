package com.example.typespec

/**
 * Settings tsp-server reads from `workspace/configuration` for the `typespec` section.
 *
 * `@typespec/compiler` 1.17 (`client-config-provider.ts`) requests that section and keeps
 * `lsp.emit` plus `entrypoint`. `<config:defaults>` is not expanded here: the server treats
 * it as "also run the emitters declared in tspconfig.yaml". An empty `lsp.emit` is omitted
 * so the server keeps its default of `options.emit = []`, matching the VS Code extension
 * (`typespec.lsp.emit` and `typespec.entrypoint` default to null).
 */
object TypeSpecWorkspaceConfiguration {
    const val SECTION: String = "typespec"
    const val CONFIG_DEFAULTS: String = "<config:defaults>"

    fun parseList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return normalizeList(raw.split(',', ';', '\n', '\r'))
    }

    fun formatList(values: List<String>): String = normalizeList(values).joinToString(", ")

    fun typespecSection(lspEmit: List<String>, entrypoints: List<String>): Map<String, Any> {
        val emit = normalizeList(lspEmit)
        val files = normalizeList(entrypoints)
        return buildMap {
            if (emit.isNotEmpty()) {
                put("lsp", mapOf("emit" to emit))
            }
            if (files.isNotEmpty()) {
                put("entrypoint", files)
            }
        }
    }

    fun changeNotificationSettings(lspEmit: List<String>, entrypoints: List<String>): Map<String, Any> =
        mapOf(SECTION to typespecSection(lspEmit, entrypoints))

    /**
     * @param scopeUri file or folder URI from the server, or null when the request has no scope.
     * Settings are project-wide, so null and any other scope receive the same section.
     * A scope value is never parsed and cannot fail the request.
     */
    fun responseFor(
        section: String?,
        @Suppress("UNUSED_PARAMETER") scopeUri: String?,
        lspEmit: List<String>,
        entrypoints: List<String>,
    ): Response {
        val normalizedSection = section?.trim()?.takeIf { it.isNotEmpty() }
        return when (normalizedSection) {
            null -> Response.Unhandled
            SECTION -> Response.Value(typespecSection(lspEmit, entrypoints))
            "$SECTION.lsp" -> Response.Value(lspSection(lspEmit))
            "$SECTION.lsp.emit" -> Response.Value(normalizeList(lspEmit).ifEmpty { null })
            "$SECTION.entrypoint" -> Response.Value(normalizeList(entrypoints).ifEmpty { null })
            else -> Response.Unhandled
        }
    }

    private fun lspSection(lspEmit: List<String>): Map<String, Any> {
        val emit = normalizeList(lspEmit)
        return if (emit.isEmpty()) emptyMap() else mapOf("emit" to emit)
    }

    private fun normalizeList(values: List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        for (value in values) {
            val trimmed = value.trim()
            if (trimmed.isNotEmpty()) {
                seen.add(trimmed)
            }
        }
        return seen.toList()
    }

    sealed interface Response {
        data class Value(val value: Any?) : Response

        data object Unhandled : Response
    }
}
