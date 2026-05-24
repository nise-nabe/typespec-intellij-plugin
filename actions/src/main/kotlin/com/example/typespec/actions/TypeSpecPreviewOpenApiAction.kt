package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TYPESPEC_OPENAPI3_EMITTER
import com.example.typespec.workflow.TypeSpecOpenApiPreview
import com.example.typespec.workflow.TypeSpecOpenApiPreviewWorkflow
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.TypeSpecTspConfigReader
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages

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
        if (resolution.entrypointFile == null) {
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

        TypeSpecOpenApiPreviewWorkflow.run(project, resolution)
    }
}
