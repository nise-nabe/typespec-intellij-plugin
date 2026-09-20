package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecArtifactNavigator
import com.example.typespec.workflow.TypeSpecHttpClientGenerator
import com.example.typespec.workflow.TypeSpecProjectContext
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files

class TypeSpecGenerateHttpClientAction : AnAction(
    TypeSpecBundle.message("action.generateHttpClient.text"),
    TypeSpecBundle.message("action.generateHttpClient.description"),
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
        val outputDirectory = TypeSpecArtifactNavigator.resolveOutputDirectory(resolution.projectRoot)
        val openApiFile = TypeSpecArtifactNavigator.findPrimaryArtifact(outputDirectory)
        if (openApiFile == null) {
            notify(project, "action.generateHttpClient.noOpenApiArtifact", NotificationType.WARNING)
            return
        }
        val httpFile = resolution.projectRoot.resolve("typespec-generated.http")
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                Files.writeString(httpFile, TypeSpecHttpClientGenerator.generateFromOpenApiFile(openApiFile))
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                notify(project, "action.generateHttpClient.failed", NotificationType.ERROR)
                return@executeOnPooledThread
            }
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(httpFile)
                ?: return@executeOnPooledThread
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed) {
                    OpenFileDescriptor(project, virtualFile).navigate(true)
                }
            }
        }
    }

    private fun notify(project: Project, messageKey: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("TypeSpec Notifications")
            .createNotification(
                TypeSpecBundle.message("action.generateHttpClient.title"),
                TypeSpecBundle.message(messageKey),
                type,
            )
            .notify(project)
    }
}
