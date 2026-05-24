package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.restartTypeSpecServerAsync
import com.example.typespec.workflow.TypeSpecOutputService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

class TypeSpecRestartServerAction : AnAction(
    TypeSpecBundle.message("action.restartServer.text"),
    TypeSpecBundle.message("action.restartServer.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = project != null &&
            TypeSpecActionSupport.isServiceEnabled(project) &&
            (file == null || TypeSpecActionSupport.isTypeSpecContext(file))
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val output = TypeSpecOutputService.getInstance(project)
        output.append(TypeSpecBundle.message("action.restartServer.started"))
        restartTypeSpecServerAsync(project)
        output.show(project)
    }
}
