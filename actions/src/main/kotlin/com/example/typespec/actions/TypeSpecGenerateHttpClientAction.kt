package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecArtifactNavigator
import com.example.typespec.workflow.TypeSpecHttpClientGenerator
import com.example.typespec.workflow.TypeSpecProjectContext
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
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
        val openApiFile = TypeSpecArtifactNavigator.findPrimaryArtifact(outputDirectory) ?: return
        val httpFile = resolution.projectRoot.resolve("typespec-generated.http")
        Files.writeString(httpFile, TypeSpecHttpClientGenerator.generateFromOpenApiFile(openApiFile))
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(httpFile.toString()) ?: return
        OpenFileDescriptor(project, virtualFile).navigate(true)
    }
}
