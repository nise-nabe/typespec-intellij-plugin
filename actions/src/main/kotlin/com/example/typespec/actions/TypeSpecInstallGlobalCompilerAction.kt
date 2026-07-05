package com.example.typespec.actions

import com.example.typespec.TYPESPEC_COMPILER_PACKAGE_NAME
import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecNpmExecutableResolver
import com.example.typespec.workflow.TypeSpecCliCommand
import com.example.typespec.workflow.toJobResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import java.nio.file.Paths

class TypeSpecInstallGlobalCompilerAction : AnAction(
    TypeSpecBundle.message("action.installGlobalCompiler.text"),
    TypeSpecBundle.message("action.installGlobalCompiler.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.projectOnly)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val workingDirectory = project.basePath?.let { Paths.get(it) } ?: Paths.get(".")

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.installGlobalCompiler.progress",
                titleKey = "action.installGlobalCompiler.title",
                failureMessageKey = "action.installGlobalCompiler.failed",
            ),
            onSuccess = {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("TypeSpec Notifications")
                    .createNotification(
                        TypeSpecBundle.message("action.installGlobalCompiler.success.title"),
                        TypeSpecBundle.message("action.installGlobalCompiler.success.content"),
                        NotificationType.INFORMATION,
                    )
                    .notify(project)
            },
        ) { runner, indicator ->
            val npm = TypeSpecNpmExecutableResolver.resolveExecutable(project)
            val command = TypeSpecCliCommand(
                executable = npm,
                args = listOf("install", "-g", TYPESPEC_COMPILER_PACKAGE_NAME),
                workingDirectory = workingDirectory,
                displayCommand = "$npm install -g $TYPESPEC_COMPILER_PACKAGE_NAME",
            )
            runner.run(command, emptyList(), TypeSpecBundle.message("action.installGlobalCompiler.progress"), indicator)
                .toJobResult()
        }
    }
}
