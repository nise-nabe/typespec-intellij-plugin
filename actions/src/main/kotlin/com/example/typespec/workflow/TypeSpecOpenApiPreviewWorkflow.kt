package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecOpenApiPreviewWorkflow {
    fun run(project: Project, resolution: TypeSpecProjectResolution) {
        val entrypoint = resolution.entrypointFile ?: return

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.previewOpenApi.progress",
                titleKey = "action.previewOpenApi.title",
                failureMessageKey = "action.previewOpenApi.failed",
            ),
        ) { runner ->
            val workDir = Files.createTempDirectory("typespec-openapi-preview-")
            try {
                when (
                    val exitCode = runner.compile(
                        projectRoot = resolution.projectRoot,
                        entrypoint = entrypoint,
                        emitters = listOf(TYPESPEC_OPENAPI3_EMITTER),
                        extraArgs = TypeSpecOpenApiPreview.openApiPreviewCompileExtraArgs(workDir),
                    )
                ) {
                    null -> {
                        TypeSpecOpenApiPreview.deleteRecursivelyQuietly(workDir)
                        null
                    }
                    0 -> {
                        openPreviewHtml(project, workDir)
                        0
                    }
                    else -> {
                        TypeSpecOpenApiPreview.deleteRecursivelyQuietly(workDir)
                        exitCode
                    }
                }
            } catch (e: Exception) {
                TypeSpecOpenApiPreview.deleteRecursivelyQuietly(workDir)
                TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                    project,
                    e.message ?: e.toString(),
                    "action.previewOpenApi.title",
                )
                -1
            }
        }
    }

    private fun openPreviewHtml(project: Project, workDir: Path) {
        val openApiFile = TypeSpecOpenApiPreview.findOpenApiOutputFile(workDir)
        if (openApiFile == null) {
            TypeSpecOpenApiPreview.deleteRecursivelyQuietly(workDir)
            TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                project,
                TypeSpecBundle.message("action.previewOpenApi.noOutput"),
                "action.previewOpenApi.title",
            )
            return
        }
        try {
            val previewHtml = Files.createTempFile("typespec-openapi-preview-", ".html")
            previewHtml.toFile().deleteOnExit()
            Files.writeString(
                previewHtml,
                TypeSpecOpenApiPreview.buildSwaggerPreviewHtml(Files.readString(openApiFile)),
            )
            TypeSpecOpenApiPreview.deleteRecursivelyQuietly(workDir)
            ApplicationManager.getApplication().invokeLater {
                BrowserUtil.browse(previewHtml.toUri())
            }
        } catch (e: Exception) {
            TypeSpecOpenApiPreview.deleteRecursivelyQuietly(workDir)
            TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                project,
                e.message ?: e.toString(),
                "action.previewOpenApi.title",
            )
        }
    }
}
