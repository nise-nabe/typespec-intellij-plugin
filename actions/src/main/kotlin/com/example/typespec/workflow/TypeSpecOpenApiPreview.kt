package com.example.typespec.workflow

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.stream.Stream

internal const val TYPESPEC_OPENAPI3_EMITTER = "@typespec/openapi3"

internal object TypeSpecOpenApiPreview {
    fun resolvePreviewEmitter(configuredEmitters: List<String>): String? =
        configuredEmitters.firstOrNull { it == TYPESPEC_OPENAPI3_EMITTER }

    fun openApiPreviewCompileExtraArgs(outputDirectory: Path): List<String> = listOf(
        "--option",
        "@typespec/openapi3.file-type=json",
        "--option",
        "@typespec/openapi3.emitter-output-dir=${outputDirectory.toAbsolutePath()}",
    )

    fun findOpenApiOutputFile(directory: Path): Path? {
        Files.walk(directory).use { paths: Stream<Path> ->
            return paths
                .filter { Files.isRegularFile(it) }
                .filter {
                    val name = it.fileName.toString().lowercase()
                    name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml")
                }
                .sorted(Comparator.comparing { it.fileName.toString().lowercase() })
                .findFirst()
                .orElse(null)
        }
    }

    fun buildSwaggerPreviewHtml(jsonSpec: String): String {
        val embeddedSpec = jsonSpec.trim().replace("</", "<\\/")
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script type="application/json" id="typespec-openapi-spec">$embeddedSpec</script>
              <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
              <script>
                const spec = JSON.parse(document.getElementById('typespec-openapi-spec').textContent);
                window.ui = SwaggerUIBundle({ spec, dom_id: '#swagger-ui' });
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun deleteRecursivelyQuietly(path: Path) {
        try {
            if (!Files.exists(path)) {
                return
            }
            Files.walk(path).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        } catch (_: Exception) {
            // Best-effort cleanup for temp preview directories.
        }
    }
}
