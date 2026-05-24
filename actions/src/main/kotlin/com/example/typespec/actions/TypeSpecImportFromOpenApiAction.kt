package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliRunner
import com.example.typespec.workflow.TypeSpecOutputService
import com.example.typespec.workflow.TypeSpecWorkflowGuards
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TypeSpecImportFromOpenApiAction : AnAction(
    TypeSpecBundle.message("action.importOpenApi.text"),
    TypeSpecBundle.message("action.importOpenApi.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.updateForProject(event)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val folderDescriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withTitle(TypeSpecBundle.message("action.importOpenApi.targetFolder"))
        val targetFolder = FileChooser.chooseFile(folderDescriptor, project, null)?.path?.let { Paths.get(it) }
            ?: return

        val openApiDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle(TypeSpecBundle.message("action.importOpenApi.sourceFile"))
            .withFileFilter { file ->
                val extension = file.extension?.lowercase()
                extension == "json" || extension == "yaml" || extension == "yml"
            }
        val sourceFile = FileChooser.chooseFile(openApiDescriptor, project, null)?.path
            ?: return

        val output = TypeSpecOutputService.getInstance(project)
        output.show(project)
        output.clear()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, TypeSpecBundle.message("action.importOpenApi.progress"), true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                if (!TypeSpecWorkflowGuards.confirmWriteToNonEmptyDirectory(
                        project,
                        targetFolder,
                        "action.importOpenApi.nonEmptyWarning",
                        "action.importOpenApi.title",
                    )
                ) {
                    return
                }
                Files.createDirectories(targetFolder)

                val npx = TypeSpecCliResolver.resolveNpxCli(targetFolder)
                if (npx == null) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            TypeSpecBundle.message("action.importOpenApi.npxMissing"),
                            TypeSpecBundle.message("action.importOpenApi.title"),
                        )
                    }
                    return
                }

                val args = listOf(
                    "tsp-openapi3",
                    sourceFile,
                    "--output-dir",
                    targetFolder.toString(),
                )
                val exitCode = TypeSpecCliRunner(project).run(npx, args, TypeSpecBundle.message("action.importOpenApi.progress"))
                if (exitCode == 0) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        refreshAndOpen(project, targetFolder)
                    }
                } else {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showWarningDialog(
                            project,
                            TypeSpecBundle.message("action.importOpenApi.failed", exitCode),
                            TypeSpecBundle.message("action.importOpenApi.title"),
                        )
                    }
                }
            }
        })
    }

    private fun refreshAndOpen(project: com.intellij.openapi.project.Project, targetFolder: Path) {
        LocalFileSystem.getInstance().refreshAndFindFileByPath(targetFolder.toString())?.let { directory ->
            directory.children.firstOrNull { it.extension == "tsp" }?.let { tspFile ->
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(tspFile, true)
            }
        }
    }
}
