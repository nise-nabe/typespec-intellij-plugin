package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.TypeSpecWorkflowGuards
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
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

        if (!TypeSpecWorkflowGuards.confirmWriteToNonEmptyDirectory(
                project,
                targetFolder,
                "action.importOpenApi.nonEmptyWarning",
                "action.importOpenApi.title",
            )
        ) {
            return
        }

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.importOpenApi.progress",
                titleKey = "action.importOpenApi.title",
                compilerMissingMessageKey = "action.importOpenApi.openapi3Missing",
                failureMessageKey = "action.importOpenApi.failed",
            ),
            onExitCode = { exitCode ->
                if (exitCode == 0) {
                    ApplicationManager.getApplication().invokeLater {
                        refreshAndOpen(project, targetFolder)
                    }
                }
            },
        ) { runner ->
            Files.createDirectories(targetFolder)
            val cli = TypeSpecCliResolver.resolveOpenApi3Cli(project, targetFolder) ?: return@runCliJob null
            val args = listOf(
                sourceFile,
                "--output-dir",
                targetFolder.toString(),
            )
            runner.run(cli, args, TypeSpecBundle.message("action.importOpenApi.progress"))
        }
    }

    private fun refreshAndOpen(project: Project, targetFolder: Path) {
        LocalFileSystem.getInstance().refreshAndFindFileByPath(targetFolder.toString())?.let { directory ->
            val entrypoint = TypeSpecProjectContext.resolveEntrypointFile(
                targetFolder,
                directory.children.firstOrNull { it.extension == "tsp" }?.let { Paths.get(it.path) },
            )
            val fileToOpen = entrypoint?.let { path ->
                directory.children.firstOrNull { it.path == path.toString() }
            } ?: directory.children.firstOrNull { it.extension == "tsp" }
            fileToOpen?.let { tspFile ->
                FileEditorManager.getInstance(project).openFile(tspFile, true)
            }
        }
    }
}
