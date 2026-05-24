package com.example.typespec.workflow

import java.nio.file.Path

internal data class TypeSpecCompileRequest(
    val projectRoot: Path,
    val entrypoint: Path,
    val emitters: List<String>,
    val extraArgs: List<String> = emptyList(),
    val watch: Boolean = false,
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
        addAll(request.extraArgs)
    }
