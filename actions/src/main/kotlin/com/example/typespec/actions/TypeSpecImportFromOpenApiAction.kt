package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliRunner
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecWorkflowGuards
import com.example.typespec.workflow.TypeSpecWorkflowOutcomes
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files
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

        if (!TypeSpecWorkflowGuards.confirmWriteToNonEmptyDirectory(
                project,
                targetFolder,
                "action.importOpenApi.nonEmptyWarning",
                "action.importOpenApi.title",
            )
        ) {
            return
        }

        TypeSpecCliWorkflow.prepareOutput(project)
        TypeSpecCliWorkflow.runBackground(project, "action.importOpenApi.progress") {
            Files.createDirectories(targetFolder)

            val cli = TypeSpecCliResolver.resolveOpenApi3Cli(project, targetFolder)
            if (cli == null) {
                TypeSpecCliWorkflow.showCompilerMissing(
                    project,
                    "action.importOpenApi.title",
                    messageKey = "action.importOpenApi.openapi3Missing",
                )
                return@runBackground
            }

            val args = listOf(
                sourceFile,
                "--output-dir",
                targetFolder.toString(),
            )
            val exitCode = TypeSpecCliRunner(project).run(cli, args, TypeSpecBundle.message("action.importOpenApi.progress"))
            if (exitCode == 0) {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    refreshAndOpen(project, targetFolder)
                }
            } else {
                TypeSpecWorkflowOutcomes.presentWarningOnEdt(
                    project,
                    "action.importOpenApi.failed",
                    "action.importOpenApi.title",
                    exitCode,
                )
            }
        }
    }

    private fun refreshAndOpen(project: com.intellij.openapi.project.Project, targetFolder: java.nio.file.Path) {
        LocalFileSystem.getInstance().refreshAndFindFileByPath(targetFolder.toString())?.let { directory ->
            directory.children.firstOrNull { it.extension == "tsp" }?.let { tspFile ->
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(tspFile, true)
            }
        }
    }
}
