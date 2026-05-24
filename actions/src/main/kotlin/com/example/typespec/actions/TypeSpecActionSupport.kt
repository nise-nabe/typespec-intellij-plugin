package com.example.typespec.actions

import com.example.typespec.TypeSpecCompilerPackageResolver
import com.example.typespec.TypeSpecFileType
import com.example.typespec.TypeSpecServiceMode
import com.example.typespec.TypeSpecServiceSettings
import com.example.typespec.workflow.TypeSpecOutputService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal enum class CliRequirement {
    None,
    Compiler,
}

internal data class ActionVisibility(
    val requireServiceEnabled: Boolean = true,
    val requireVirtualFile: Boolean = false,
    val requireTypeSpecContextWhenFilePresent: Boolean = false,
    val requireTypeSpecContextWhenFileRequired: Boolean = false,
    val cli: CliRequirement = CliRequirement.None,
)

internal object TypeSpecActionSupport {
    val projectOnly = ActionVisibility(requireServiceEnabled = false)
    val projectWithCompilerCli = ActionVisibility(cli = CliRequirement.Compiler)
    val projectWithOpenApi3Cli = ActionVisibility(cli = CliRequirement.Compiler)
    val typeSpecFileWithCompilerCli = ActionVisibility(
        requireVirtualFile = true,
        requireTypeSpecContextWhenFileRequired = true,
        cli = CliRequirement.Compiler,
    )
    val serviceEnabledOptionalFile = ActionVisibility(requireTypeSpecContextWhenFilePresent = true)

    fun update(event: AnActionEvent, policy: ActionVisibility) {
        val project = event.project
        val enabled = project != null && matchesPolicy(project, event, policy)
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

    private fun matchesPolicy(project: Project, event: AnActionEvent, policy: ActionVisibility): Boolean {
        if (policy.requireServiceEnabled && !isServiceEnabled(project)) {
            return false
        }
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        if (policy.requireVirtualFile && file == null) {
            return false
        }
        if (file != null && policy.requireTypeSpecContextWhenFilePresent && !isTypeSpecContext(file)) {
            return false
        }
        if (policy.requireTypeSpecContextWhenFileRequired && (file == null || !isTypeSpecContext(file))) {
            return false
        }
        return when (policy.cli) {
            CliRequirement.None -> true
            CliRequirement.Compiler -> TypeSpecCompilerPackageResolver.isCompilerCliResolvable(project)
        }
    }
}
