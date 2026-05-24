package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
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
        ) { runner, indicator ->
            val workDir = Files.createTempDirectory("typespec-openapi-preview-")
            try {
                executePreview(runner, project, resolution, entrypoint, workDir, indicator)
            } catch (e: Exception) {
                TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                    project,
                    e.message ?: e.toString(),
                    "action.previewOpenApi.title",
                )
                TypeSpecCliJobResult.FailureNotified
            } finally {
                TypeSpecOpenApiPreview.deleteRecursivelyQuietly(workDir)
            }
        }
    }

    private fun executePreview(
        runner: TypeSpecCliRunner,
        project: Project,
        resolution: TypeSpecProjectResolution,
        entrypoint: Path,
        workDir: Path,
        indicator: ProgressIndicator,
    ): TypeSpecCliJobResult {
        return when (
            val compileResult = runner.compile(
                projectRoot = resolution.projectRoot,
                entrypoint = entrypoint,
                emitters = listOf(TYPESPEC_OPENAPI3_EMITTER),
                extraArgs = TypeSpecOpenApiPreview.openApiPreviewCompileExtraArgs(workDir),
                indicator = indicator,
            )
        ) {
            TypeSpecCliJobResult.CliUnavailable,
            TypeSpecCliJobResult.Cancelled,
            TypeSpecCliJobResult.AbortedByUser,
            TypeSpecCliJobResult.FailureNotified,
            -> compileResult
            is TypeSpecCliJobResult.Finished -> {
                if (compileResult.exitCode != 0) {
                    compileResult
                } else if (openPreviewHtml(project, workDir)) {
                    TypeSpecCliJobResult.Finished(0)
                } else {
                    TypeSpecCliJobResult.FailureNotified
                }
            }
        }
    }

    private fun openPreviewHtml(project: Project, workDir: Path): Boolean {
        val openApiFile = TypeSpecOpenApiPreview.findOpenApiOutputFile(workDir)
        if (openApiFile == null) {
            TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                project,
                TypeSpecBundle.message("action.previewOpenApi.noOutput"),
                "action.previewOpenApi.title",
            )
            return false
        }
        return try {
            val previewHtml = Files.createTempFile("typespec-openapi-preview-", ".html")
            previewHtml.toFile().deleteOnExit()
            Files.writeString(
                previewHtml,
                TypeSpecOpenApiPreview.buildSwaggerPreviewHtml(Files.readString(openApiFile)),
            )
            ApplicationManager.getApplication().invokeLater {
                BrowserUtil.browse(previewHtml.toUri())
            }
            true
        } catch (e: Exception) {
            TypeSpecWorkflowOutcomes.presentErrorOnEdt(
                project,
                e.message ?: e.toString(),
                "action.previewOpenApi.title",
            )
            false
        }
    }
}
