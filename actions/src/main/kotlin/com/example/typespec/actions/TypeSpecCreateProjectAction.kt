package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecWorkflowGuards
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import java.nio.file.Files

class TypeSpecCreateProjectAction : AnAction(
    TypeSpecBundle.message("action.createProject.text"),
    TypeSpecBundle.message("action.createProject.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.projectWithCompilerCli)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val dialog = TypeSpecCreateProjectDialog()
        if (!dialog.showAndGet()) {
            return
        }
        val targetPath = dialog.targetPath() ?: return

        val template = dialog.selectedTemplate()
        val args = buildList {
            add("init")
            if (template != "default") {
                add("--template")
                add(template)
            }
        }

        if (!TypeSpecWorkflowGuards.confirmWriteToNonEmptyDirectory(
                project,
                targetPath,
                "action.createProject.nonEmptyWarning",
                "action.createProject.title",
            )
        ) {
            return
        }

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.createProject.progress",
                titleKey = "action.createProject.title",
            ),
            onExitCode = { exitCode ->
                if (exitCode == 0) {
                    ApplicationManager.getApplication().invokeLater {
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("TypeSpec Notifications")
                            .createNotification(
                                TypeSpecBundle.message("action.createProject.success.title"),
                                TypeSpecBundle.message("action.createProject.success.content"),
                                NotificationType.INFORMATION,
                            )
                            .notify(project)
                    }
                }
            },
        ) { runner ->
            Files.createDirectories(targetPath)
            val cli = TypeSpecCliResolver.resolveTspCli(project, targetPath) ?: return@runCliJob null
            runner.run(cli, args, TypeSpecBundle.message("action.createProject.progress"))
        }
    }
}
