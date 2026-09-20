package com.example.typespec.workflow

import java.nio.file.Path

internal data class TypeSpecCompileRequest(
    val projectRoot: Path,
    val entrypoint: Path,
    val emitters: List<String>,
    val extraArgs: List<String> = emptyList(),
    val watch: Boolean = false,
    val dryRun: Boolean = false,
    val noEmit: Boolean = false,
    val stats: Boolean = false,
    val trace: String = "",
    val warnAsError: Boolean = false,
)

internal fun buildTypeSpecCompileTspArgs(request: TypeSpecCompileRequest): List<String> = buildList {
        add("compile")
        add(request.entrypoint.toString())
        request.emitters.forEach { emitter ->
            add("--emit")
            add(emitter)
        }
        if (request.watch) {
            add("--watch")
        }
        if (request.dryRun) {
            add("--dry-run")
        }
        if (request.noEmit) {
            add("--no-emit")
        }
        if (request.stats) {
            add("--stats")
        }
        if (request.trace.isNotBlank()) {
            add("--trace")
            add(request.trace)
        }
        if (request.warnAsError) {
            add("--warn-as-error")
        }
        addAll(request.extraArgs)
    }
