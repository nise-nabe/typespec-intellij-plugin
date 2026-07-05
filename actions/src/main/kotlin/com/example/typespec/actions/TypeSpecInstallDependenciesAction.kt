package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.toJobResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

class TypeSpecInstallDependenciesAction : AnAction(
    TypeSpecBundle.message("action.installDependencies.text"),
    TypeSpecBundle.message("action.installDependencies.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.typeSpecProjectWithCompilerCli)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val resolution = TypeSpecProjectContext.resolveFromVirtualFile(file) ?: return

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.installDependencies.progress",
                titleKey = "action.installDependencies.title",
                failureMessageKey = "action.installDependencies.failed",
            ),
            onSuccess = {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("TypeSpec Notifications")
                    .createNotification(
                        TypeSpecBundle.message("action.installDependencies.success.title"),
                        TypeSpecBundle.message("action.installDependencies.success.content"),
                        NotificationType.INFORMATION,
                    )
                    .notify(project)
            },
        ) { runner, indicator ->
            val cli = TypeSpecCliResolver.resolveTspCli(project, resolution.projectRoot)
                ?: return@runCliJob com.example.typespec.workflow.TypeSpecCliJobResult.CliUnavailable
            runner.run(cli, listOf("install"), TypeSpecBundle.message("action.installDependencies.progress"), indicator)
                .toJobResult()
        }
    }
}
