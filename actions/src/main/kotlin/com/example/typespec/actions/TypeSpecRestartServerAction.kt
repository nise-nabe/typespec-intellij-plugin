package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.TypeSpecLanguageServerCommandExecutor
import com.example.typespec.workflow.TypeSpecOutputService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class TypeSpecRestartServerAction : AnAction(
    TypeSpecBundle.message("action.restartServer.text"),
    TypeSpecBundle.message("action.restartServer.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.updateWhenServiceEnabled(event, requireResolvableCompiler = false)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val output = TypeSpecOutputService.getInstance(project)
        output.append(TypeSpecBundle.message("action.restartServer.started"))
        TypeSpecLanguageServerCommandExecutor.getInstance(project)?.restartServerAsync(project)
        output.show(project)
    }
}
