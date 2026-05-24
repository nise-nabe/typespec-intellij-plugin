package com.example.typespec.workflow

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

class TypeSpecCompileCommandBuilderTest {
    @Test
    fun buildTspArgsIncludesEmittersWatchAndExtraArgs() {
        val request = TypeSpecCompileRequest(
            projectRoot = Paths.get("/project"),
            entrypoint = Paths.get("/project/main.tsp"),
            emitters = listOf("@typespec/openapi3"),
            extraArgs = listOf("--option", "foo=bar"),
            watch = true,
        )

        val args = buildTypeSpecCompileTspArgs(request)
        assertEquals("compile", args[0])
        assertEquals(request.entrypoint.toString(), args[1])
        assertEquals(
            listOf("--emit", "@typespec/openapi3", "--watch", "--option", "foo=bar"),
            args.drop(2),
        )
    }
}
