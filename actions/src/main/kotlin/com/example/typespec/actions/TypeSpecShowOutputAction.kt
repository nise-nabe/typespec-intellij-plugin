package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class TypeSpecShowOutputAction : AnAction(
    TypeSpecBundle.message("action.showOutput.text"),
    TypeSpecBundle.message("action.showOutput.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.updateForProject(event)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        TypeSpecActionSupport.openOutput(project)
    }
}
