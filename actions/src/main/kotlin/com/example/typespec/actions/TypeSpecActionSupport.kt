package com.example.typespec.actions

import com.example.typespec.TypeSpecFileType
import com.example.typespec.TypeSpecCompilerPackageResolver
import com.example.typespec.TypeSpecServiceMode
import com.example.typespec.TypeSpecServiceSettings
import com.example.typespec.workflow.TypeSpecOutputService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal object TypeSpecActionSupport {
    fun updateForTypeSpecContext(event: AnActionEvent, requireResolvableCompiler: Boolean = true) {
        val project = event.project
        val file = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        val enabled = project != null &&
            isServiceEnabled(project) &&
            file != null &&
            isTypeSpecContext(file) &&
            (!requireResolvableCompiler || TypeSpecCompilerPackageResolver.isCompilerCliResolvable(project))
        event.presentation.isEnabledAndVisible = enabled
    }

    fun updateForProject(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabledAndVisible = project != null
    }

    fun updateWhenServiceEnabled(
        event: AnActionEvent,
        requireResolvableCompiler: Boolean = true,
    ) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val enabled = project != null &&
            isServiceEnabled(project) &&
            (file == null || isTypeSpecContext(file)) &&
            (!requireResolvableCompiler || TypeSpecCompilerPackageResolver.isCompilerCliResolvable(project))
        event.presentation.isEnabledAndVisible = enabled
    }

    fun updateActionThread(): ActionUpdateThread = ActionUpdateThread.BGT

    fun isServiceEnabled(project: Project): Boolean =
        TypeSpecServiceSettings.getInstance(project).serviceMode == TypeSpecServiceMode.ENABLED

    fun isTypeSpecContext(file: VirtualFile): Boolean =
        file.fileType == TypeSpecFileType ||
            file.name == "tspconfig.yaml" ||
            file.extension == "tsp"

    fun openOutput(project: Project) {
        TypeSpecOutputService.getInstance(project).show(project)
    }

}
