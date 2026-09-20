package com.example.typespec.workflow

import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecHttpClientGenerator {
    private const val BASE_URL = "http://localhost:8080"
    private const val MAX_REQUESTS = 20
    private val pathPattern = Regex("""^ {2}(/[^:\s]+):""")
    private val methodPattern = Regex("""^ {4}(get|put|post|delete|patch|head|options|trace):""")

    fun generateFromOpenApiFile(openApiFile: Path): String {
        val text = Files.readString(openApiFile)
        val requests = mutableListOf<Pair<String, String>>()
        var currentPath: String? = null
        for (line in text.lines()) {
            pathPattern.find(line)?.let {
                currentPath = it.groupValues[1]
            }
            val path = currentPath ?: continue
            methodPattern.find(line)?.let {
                requests += it.groupValues[1].uppercase() to path
            }
        }
        val builder = StringBuilder()
        builder.appendLine("### Generated from ${openApiFile.fileName}")
        builder.appendLine("### Edit and run with IntelliJ HTTP Client")
        builder.appendLine()
        val distinct = requests.distinct().take(MAX_REQUESTS)
        for ((method, path) in distinct) {
            builder.appendLine("$method $BASE_URL$path")
            builder.appendLine("Accept: application/json")
            builder.appendLine()
        }
        if (distinct.isEmpty()) {
            builder.appendLine("GET $BASE_URL/")
            builder.appendLine("Accept: application/json")
        }
        return builder.toString().trimEnd() + "\n"
    }
}
