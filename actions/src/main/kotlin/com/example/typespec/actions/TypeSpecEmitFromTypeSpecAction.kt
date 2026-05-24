package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.TypeSpecTspConfigReader
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages

class TypeSpecEmitFromTypeSpecAction : AnAction(
    TypeSpecBundle.message("action.emit.text"),
    TypeSpecBundle.message("action.emit.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.typeSpecFileWithCompilerCli)
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
                TypeSpecBundle.message("action.emit.title"),
            )
            return
        }

        val configuredEmitters = TypeSpecTspConfigReader.readEmitters(resolution.projectRoot)
        val emitters = when {
            configuredEmitters.isEmpty() -> {
                Messages.showErrorDialog(
                    project,
                    TypeSpecBundle.message("action.emit.noEmitters"),
                    TypeSpecBundle.message("action.emit.title"),
                )
                return
            }
            configuredEmitters.size == 1 -> configuredEmitters
            else -> {
                val selected = Messages.showEditableChooseDialog(
                    TypeSpecBundle.message("action.emit.chooseEmitter"),
                    TypeSpecBundle.message("action.emit.title"),
                    Messages.getQuestionIcon(),
                    configuredEmitters.toTypedArray(),
                    configuredEmitters.first(),
                    null,
                ) ?: return
                listOf(selected)
            }
        }

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.emit.progress",
                titleKey = "action.emit.title",
                failureMessageKey = "action.emit.failed",
            ),
        ) { runner, indicator ->
            runner.compile(resolution.projectRoot, entrypoint, emitters, indicator = indicator)
        }
    }
}
