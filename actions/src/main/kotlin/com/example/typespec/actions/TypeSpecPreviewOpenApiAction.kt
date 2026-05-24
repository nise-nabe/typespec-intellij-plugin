package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TYPESPEC_OPENAPI3_EMITTER
import com.example.typespec.workflow.TypeSpecCliRunner
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecOpenApiPreview
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.TypeSpecTspConfigReader
import com.example.typespec.workflow.TypeSpecWorkflowOutcomes
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import java.nio.file.Files

class TypeSpecPreviewOpenApiAction : AnAction(
    TypeSpecBundle.message("action.previewOpenApi.text"),
    TypeSpecBundle.message("action.previewOpenApi.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.updateForTypeSpecContext(event)
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

        val configuredEmitters = TypeSpecTspConfigReader.readEmitters(resolution.projectRoot)
        if (TypeSpecOpenApiPreview.resolvePreviewEmitter(configuredEmitters) == null) {
            Messages.showErrorDialog(
                project,
                TypeSpecBundle.message("action.previewOpenApi.noOpenApiEmitter", TYPESPEC_OPENAPI3_EMITTER),
                TypeSpecBundle.message("action.previewOpenApi.title"),
            )
            return
        }

        TypeSpecCliWorkflow.prepareOutput(project)
        TypeSpecCliWorkflow.runBackground(project, "action.previewOpenApi.progress") {
            val tempDir = Files.createTempDirectory("typespec-openapi-preview-")
            var previewHtml: java.nio.file.Path? = null
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
                val openApiJson = Files.readString(openApiFile)
                previewHtml = Files.createTempFile("typespec-openapi-preview-", ".html")
                Files.writeString(previewHtml, TypeSpecOpenApiPreview.buildSwaggerPreviewHtml(openApiJson))
                val htmlToOpen = previewHtml
                ApplicationManager.getApplication().invokeLater {
                    BrowserUtil.browse(htmlToOpen.toUri())
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        e.message ?: e.toString(),
                        TypeSpecBundle.message("action.previewOpenApi.title"),
                    )
                }
            } finally {
                TypeSpecOpenApiPreview.deleteRecursivelyQuietly(tempDir)
            }
        }
    }
}
