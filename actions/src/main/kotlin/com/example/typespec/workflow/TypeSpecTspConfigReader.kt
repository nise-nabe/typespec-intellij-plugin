package com.example.typespec.workflow

import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecTspConfigReader {
    fun readEmitters(projectRoot: Path): List<String> {
        val configFile = projectRoot.resolve(TypeSpecProjectContext.TSP_CONFIG_FILE_NAME)
        if (!Files.isRegularFile(configFile)) {
            return emptyList()
        }
        val text = Files.readString(configFile)
        return parseEmitters(text)
    }

    fun readOutputDir(projectRoot: Path): String? {
        val configFile = projectRoot.resolve(TypeSpecProjectContext.TSP_CONFIG_FILE_NAME)
        if (!Files.isRegularFile(configFile)) {
            return null
        }
        return parseOutputDir(Files.readString(configFile))
    }

    internal fun parseOutputDir(yamlText: String): String? {
        val outputDirPattern = Regex("""^output-dir:\s*["']?([^"'\n#]+)["']?\s*$""")
        for (line in yamlText.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }
            outputDirPattern.matchEntire(trimmed)?.groupValues?.getOrNull(1)?.trim()?.let { return it }
        }
        return null
    }

    internal fun parseEmitters(yamlText: String): List<String> {
        val lines = yamlText.lines()
        var inEmitSection = false
        val emitters = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }
            if (!inEmitSection) {
                if (trimmed == "emit:" || trimmed.startsWith("emit:")) {
                    val inline = trimmed.removePrefix("emit:").trim()
                    if (inline.isNotEmpty()) {
                        parseEmitterToken(inline)?.let { emitters.add(it) }
                    }
                    inEmitSection = inline.isEmpty()
                }
                continue
            }
            if (!line.startsWith(" ") && !line.startsWith("\t")) {
                break
            }
            val item = trimmed.removePrefix("-").trim()
            parseEmitterToken(item)?.let { emitters.add(it) }
        }
        return emitters.distinct()
    }

    private fun parseEmitterToken(token: String): String? {
        val value = token.trim().trim('"', '\'')
        return value.takeIf { it.isNotEmpty() }
    }
}
