package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliRunner
import com.example.typespec.workflow.TypeSpecOutputService
import com.example.typespec.workflow.TypeSpecWorkflowGuards
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import java.nio.file.Files

class TypeSpecCreateProjectAction : AnAction(
    TypeSpecBundle.message("action.createProject.text"),
    TypeSpecBundle.message("action.createProject.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.updateForProject(event)
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

        val output = TypeSpecOutputService.getInstance(project)
        output.show(project)
        output.clear()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, TypeSpecBundle.message("action.createProject.progress"), true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                if (!TypeSpecWorkflowGuards.confirmWriteToNonEmptyDirectory(
                        project,
                        targetPath,
                        "action.createProject.nonEmptyWarning",
                        "action.createProject.title",
                    )
                ) {
                    return
                }
                Files.createDirectories(targetPath)

                val cli = TypeSpecCliResolver.resolveTspCli(project, targetPath)
                if (cli == null) {
                    TypeSpecActionSupport.showCompilerMissing(project, "action.createProject.title")
                    return
                }

                val exitCode = TypeSpecCliRunner(project).run(cli, args, TypeSpecBundle.message("action.createProject.progress"))
                if (exitCode == 0) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
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
            }
        })
    }
}
