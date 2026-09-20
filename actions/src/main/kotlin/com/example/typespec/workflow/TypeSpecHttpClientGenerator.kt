package com.example.typespec.workflow

import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecHttpClientGenerator {
    private const val BASE_URL = "http://localhost:8080"
    private const val MAX_REQUESTS = 20
    private val yamlPathPattern = Regex("""^ {2}(/[^:\s]+):""")
    private val yamlMethodPattern = Regex("""^ {4}(get|put|post|delete|patch|head|options|trace):""")
    private val jsonKeyPattern = Regex("""^(\s*)"([^"]+)"\s*:""")
    private val jsonMethodPattern = Regex("""^\s*"(get|put|post|delete|patch|head|options|trace)"\s*:""")

    fun generateFromOpenApiFile(openApiFile: Path): String {
        val requests = mutableListOf<Pair<String, String>>()
        var currentPath: String? = null
        var pathIndent = -1
        Files.newBufferedReader(openApiFile).useLines { lines ->
            for (line in lines) {
                yamlPathPattern.find(line)?.let {
                    currentPath = it.groupValues[1]
                    pathIndent = 2
                }
                jsonKeyPattern.find(line)?.let { match ->
                    val indent = match.groupValues[1].length
                    val key = match.groupValues[2]
                    if (key.startsWith("/")) {
                        currentPath = key
                        pathIndent = indent
                    } else if (indent <= pathIndent) {
                        currentPath = null
                        pathIndent = -1
                    }
                }
                val path = currentPath ?: continue
                (yamlMethodPattern.find(line) ?: jsonMethodPattern.find(line))?.let {
                    requests += it.groupValues[1].uppercase() to path
                }
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
