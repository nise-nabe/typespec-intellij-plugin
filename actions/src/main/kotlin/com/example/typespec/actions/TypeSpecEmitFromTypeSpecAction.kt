package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliRunner
import com.example.typespec.workflow.TypeSpecOutputService
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.TypeSpecTspConfigReader
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import java.nio.file.Paths

class TypeSpecEmitFromTypeSpecAction : AnAction(
    TypeSpecBundle.message("action.emit.text"),
    TypeSpecBundle.message("action.emit.description"),
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

        val output = TypeSpecOutputService.getInstance(project)
        output.show(project)
        output.clear()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, TypeSpecBundle.message("action.emit.progress"), true) {
            override fun run(indicator: ProgressIndicator) {
                val runner = TypeSpecCliRunner(project)
                val exitCode = runner.compile(project, resolution.projectRoot, entrypoint, emitters)
                    ?: run {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                TypeSpecBundle.message("action.emit.compilerMissing"),
                                TypeSpecBundle.message("action.emit.title"),
                            )
                            TypeSpecActionSupport.openSettings(project)
                        }
                        return
                    }
                if (exitCode != 0) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showWarningDialog(
                            project,
                            TypeSpecBundle.message("action.emit.failed", exitCode),
                            TypeSpecBundle.message("action.emit.title"),
                        )
                    }
                }
            }
        })
    }
}
