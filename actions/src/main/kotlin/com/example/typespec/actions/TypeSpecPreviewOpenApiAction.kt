package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliRunner
import com.example.typespec.workflow.TypeSpecOutputService
import com.example.typespec.workflow.TypeSpecProjectContext
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

class TypeSpecPreviewOpenApiAction : AnAction(
    TypeSpecBundle.message("action.previewOpenApi.text"),
    TypeSpecBundle.message("action.previewOpenApi.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = project != null &&
            file != null &&
            file.extension == "tsp" &&
            TypeSpecActionSupport.isServiceEnabled(project)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val resolution = TypeSpecProjectContext.resolveFromVirtualFile(file) ?: return
        val entrypoint = resolution.entrypointFile
        if (entrypoint == null) {
            Messages.showErrorDialog(
                project,
                TypeSpecBundle.message("action.emit.noEntrypoint"),
                TypeSpecBundle.message("action.previewOpenApi.title"),
            )
            return
        }

        val output = TypeSpecOutputService.getInstance(project)
        output.show(project)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, TypeSpecBundle.message("action.previewOpenApi.progress"), true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                val tempDir = Files.createTempDirectory("typespec-openapi-preview-")
                try {
                    val runner = TypeSpecCliRunner(project)
                    val exitCode = runner.compile(
                        project = project,
                        projectRoot = resolution.projectRoot,
                        entrypoint = entrypoint,
                        emitters = listOf("@typespec/openapi3"),
                        extraArgs = listOf(
                            "--option",
                            "@typespec/openapi3.file-type=json",
                            "--option",
                            "@typespec/openapi3.emitter-output-dir=${tempDir.toAbsolutePath()}",
                        ),
                    ) ?: run {
                        showCompilerMissing(project)
                        return
                    }
                    if (exitCode != 0) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(
                                project,
                                TypeSpecBundle.message("action.previewOpenApi.failed", exitCode),
                                TypeSpecBundle.message("action.previewOpenApi.title"),
                            )
                        }
                        return
                    }
                    val openApiFile = findOpenApiFile(tempDir)
                    if (openApiFile == null) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(
                                project,
                                TypeSpecBundle.message("action.previewOpenApi.noOutput"),
                                TypeSpecBundle.message("action.previewOpenApi.title"),
                            )
                        }
                        return
                    }
                    val specJson = Files.readString(openApiFile)
                    val previewHtml = Files.createTempFile("typespec-openapi-preview-", ".html")
                    Files.writeString(previewHtml, buildSwaggerHtml(specJson))
                    ApplicationManager.getApplication().invokeLater {
                        BrowserUtil.browse(previewHtml.toUri())
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            e.message ?: e.toString(),
                            TypeSpecBundle.message("action.previewOpenApi.title"),
                        )
                    }
                }
            }
        })
    }

    private fun showCompilerMissing(project: com.intellij.openapi.project.Project) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                project,
                TypeSpecBundle.message("action.emit.compilerMissing"),
                TypeSpecBundle.message("action.previewOpenApi.title"),
            )
            TypeSpecActionSupport.openSettings(project)
        }
    }

    private fun findOpenApiFile(directory: Path): Path? {
        Files.walk(directory).use { paths: Stream<Path> ->
            return paths
                .filter { Files.isRegularFile(it) }
                .filter {
                    val name = it.fileName.toString().lowercase()
                    name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml")
                }
                .findFirst()
                .orElse(null)
        }
    }

    private fun buildSwaggerHtml(specJson: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
              <script>
                const spec = $specJson;
                window.ui = SwaggerUIBundle({ spec: spec, dom_id: '#swagger-ui' });
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
