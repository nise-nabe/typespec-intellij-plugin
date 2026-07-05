package com.example.typespec.workflow

import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecHttpClientGenerator {
    private val pathPattern = Regex("""^\s*/([^:\s]+):""")

    fun generateFromOpenApiFile(openApiFile: Path): String {
        val text = Files.readString(openApiFile)
        val paths = pathPattern.findAll(text).map { it.groupValues[1] }.distinct().take(20)
        val builder = StringBuilder()
        builder.appendLine("### Generated from ${openApiFile.fileName}")
        builder.appendLine("### Edit and run with IntelliJ HTTP Client")
        builder.appendLine()
        for (path in paths) {
            builder.appendLine("GET http://localhost:8080$path")
            builder.appendLine("Accept: application/json")
            builder.appendLine()
        }
        if (paths.none()) {
            builder.appendLine("GET http://localhost:8080/")
            builder.appendLine("Accept: application/json")
        }
        return builder.toString().trimEnd() + "\n"
    }
}
