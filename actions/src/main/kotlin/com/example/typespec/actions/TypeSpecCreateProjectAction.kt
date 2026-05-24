package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobResult
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecWorkflowGuards
import com.example.typespec.workflow.toJobResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class TypeSpecCreateProjectAction : AnAction(
    TypeSpecBundle.message("action.createProject.text"),
    TypeSpecBundle.message("action.createProject.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.projectWithCompilerCliOnly)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val dialog = TypeSpecCreateProjectDialog()
        if (!dialog.showAndGet()) {
            return
        }
        val targetPath = dialog.targetPath() ?: return
        if (!TypeSpecWorkflowGuards.confirmWriteToNonEmptyDirectory(
                project,
                targetPath,
                "action.createProject.nonEmptyWarning",
                "action.createProject.title",
            )
        ) {
            return
        }

        val template = dialog.selectedTemplate()
        val args = buildList {
            add("init")
            if (template != "default") {
                add("--template")
                add(template)
            }
        }

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.createProject.progress",
                titleKey = "action.createProject.title",
            ),
            onSuccess = {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("TypeSpec Notifications")
                    .createNotification(
                        TypeSpecBundle.message("action.createProject.success.title"),
                        TypeSpecBundle.message("action.createProject.success.content"),
                        NotificationType.INFORMATION,
                    )
                    .notify(project)
            },
        ) { runner, indicator ->
            val targetDirectory = TypeSpecWorkflowGuards.ensureTargetDirectory(targetPath)
            val cli = TypeSpecCliResolver.resolveTspCli(project, targetDirectory.path)
                ?: return@runCliJob TypeSpecCliJobResult.CliUnavailable
            val result = runner.run(cli, args, TypeSpecBundle.message("action.createProject.progress"), indicator)
                .toJobResult()
            TypeSpecWorkflowGuards.rollbackCreatedDirectory(targetDirectory, result)
            result
        }
    }
}
