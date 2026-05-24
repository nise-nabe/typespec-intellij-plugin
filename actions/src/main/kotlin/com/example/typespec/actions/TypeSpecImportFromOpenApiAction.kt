package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.virtualFilePathEquals
import com.example.typespec.workflow.TypeSpecCliJobResult
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.TypeSpecWorkflowGuards
import com.example.typespec.workflow.toJobResult
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Path
import java.nio.file.Paths

class TypeSpecImportFromOpenApiAction : AnAction(
    TypeSpecBundle.message("action.importOpenApi.text"),
    TypeSpecBundle.message("action.importOpenApi.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.projectWithCompilerCliOnly)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val folderDescriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withTitle(TypeSpecBundle.message("action.importOpenApi.targetFolder"))
        val targetFolder = FileChooser.chooseFile(folderDescriptor, project, null)?.path?.let { Paths.get(it) }
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

        val openApiDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle(TypeSpecBundle.message("action.importOpenApi.sourceFile"))
            .withFileFilter { file ->
                val extension = file.extension?.lowercase()
                extension == "json" || extension == "yaml" || extension == "yml"
            }
        val sourceFile = FileChooser.chooseFile(openApiDescriptor, project, null)?.path
            ?: return

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.importOpenApi.progress",
                titleKey = "action.importOpenApi.title",
                cliUnavailableMessageKey = "action.importOpenApi.openapi3Missing",
                failureMessageKey = "action.importOpenApi.failed",
            ),
            onSuccess = {
                refreshAndOpen(project, targetFolder)
            },
        ) { runner, indicator ->
            val targetDirectory = TypeSpecWorkflowGuards.ensureTargetDirectory(targetFolder)
            val cli = TypeSpecCliResolver.resolveOpenApi3Cli(project, targetDirectory.path)
                ?: return@runCliJob TypeSpecCliJobResult.CliUnavailable
            val args = listOf(
                sourceFile,
                "--output-dir",
                targetDirectory.path.toString(),
            )
            val result = runner.run(cli, args, TypeSpecBundle.message("action.importOpenApi.progress"), indicator)
                .toJobResult()
            TypeSpecWorkflowGuards.rollbackCreatedDirectory(targetDirectory, result)
            result
        }
    }

    private fun refreshAndOpen(project: Project, targetFolder: Path) {
        val normalizedTarget = targetFolder.toAbsolutePath().normalize()
        val directory = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(normalizedTarget.toFile())
            ?: return
        val preferredTsp = directory.children.firstOrNull { it.extension == "tsp" }?.let { Paths.get(it.path) }
        val entrypoint = TypeSpecProjectContext.resolveEntrypointFile(normalizedTarget, preferredTsp) ?: return
        val fileToOpen = directory.children.firstOrNull { virtualFilePathEquals(it, entrypoint) }
            ?: LocalFileSystem.getInstance().refreshAndFindFileByIoFile(entrypoint.toFile())
        fileToOpen?.let { tspFile ->
            FileEditorManager.getInstance(project).openFile(tspFile, true)
        }
    }
}
