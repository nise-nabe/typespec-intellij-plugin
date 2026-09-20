package com.example.typespec.actions

import com.example.typespec.TSP_CONFIG_FILE_NAME
import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobResult
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.toJobResult
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import java.nio.file.Path

class TypeSpecFormatAction : AnAction(
    TypeSpecBundle.message("action.format.text"),
    TypeSpecBundle.message("action.format.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.typeSpecProjectWithCompilerCli)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val resolution = TypeSpecProjectContext.resolveFromVirtualFile(file) ?: return
        val formatTarget = when {
            file.name == TSP_CONFIG_FILE_NAME -> "."
            file.extension == "tsp" -> formatTargetPath(resolution.projectRoot, resolution.contextFile)
            else -> return
        }

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.format.progress",
                titleKey = "action.format.title",
                failureMessageKey = "action.format.failed",
            ),
        ) { runner, indicator ->
            val cli = TypeSpecCliResolver.resolveTspCli(project, resolution.projectRoot)
                ?: return@runCliJob TypeSpecCliJobResult.CliUnavailable
            runner.run(
                cli,
                listOf("format", formatTarget),
                TypeSpecBundle.message("action.format.progress"),
                indicator,
            ).toJobResult()
        }
    }

    private fun formatTargetPath(projectRoot: Path, contextFile: Path): String =
        if (contextFile.startsWith(projectRoot)) {
            projectRoot.relativize(contextFile).toString()
        } else {
            contextFile.toString()
        }
}
