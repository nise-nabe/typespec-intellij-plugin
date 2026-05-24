package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.nio.file.Files

internal object TypeSpecOpenApiPreviewWorkflow {
    fun run(project: Project, resolution: TypeSpecProjectResolution) {
        val entrypoint = resolution.entrypointFile ?: return

        TypeSpecCliWorkflow.prepareOutput(project)
        TypeSpecCliWorkflow.runBackground(project, "action.previewOpenApi.progress") {
            val tempDir = Files.createTempDirectory("typespec-openapi-preview-")
            tempDir.toFile().deleteOnExit()
            try {
                val exitCode = TypeSpecCliRunner(project).compile(
                    project = project,
                    projectRoot = resolution.projectRoot,
                    entrypoint = entrypoint,
                    emitters = listOf(TYPESPEC_OPENAPI3_EMITTER),
                    extraArgs = TypeSpecOpenApiPreview.openApiPreviewCompileExtraArgs(tempDir),
                ) ?: run {
                    TypeSpecWorkflowOutcomes.presentCompilerMissingOnEdt(project, "action.previewOpenApi.title")
                    return@runBackground
                }
                if (exitCode != 0) {
                    TypeSpecWorkflowOutcomes.presentWarningOnEdt(
                        project,
                        "action.previewOpenApi.failed",
                        "action.previewOpenApi.title",
                        exitCode,
                    )
                    return@runBackground
                }

                val openApiFile = TypeSpecOpenApiPreview.findOpenApiOutputFile(tempDir)
                if (openApiFile == null) {
                    TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                        project,
                        TypeSpecBundle.message("action.previewOpenApi.noOutput"),
                        "action.previewOpenApi.title",
                    )
                    return@runBackground
                }

                val previewHtml = tempDir.resolve("preview.html")
                Files.writeString(
                    previewHtml,
                    TypeSpecOpenApiPreview.buildSwaggerPreviewHtml(Files.readString(openApiFile)),
                )
                ApplicationManager.getApplication().invokeLater {
                    BrowserUtil.browse(previewHtml.toUri())
                }
            } catch (e: Exception) {
                TypeSpecOpenApiPreview.deleteRecursivelyQuietly(tempDir)
                TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                    project,
                    e.message ?: e.toString(),
                    "action.previewOpenApi.title",
                )
            }
        }
    }
}
