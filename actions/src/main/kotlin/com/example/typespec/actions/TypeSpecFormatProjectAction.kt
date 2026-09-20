package com.example.typespec.actions

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
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.nio.file.Paths

class TypeSpecFormatProjectAction : AnAction(
    TypeSpecBundle.message("action.formatProject.text"),
    TypeSpecBundle.message("action.formatProject.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.projectWithCompilerCliOnly)
        if (event.presentation.isEnabledAndVisible && findTypeSpecProjectRoot(event.project) == null) {
            event.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val projectRoot = findTypeSpecProjectRoot(project) ?: return

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.formatProject.progress",
                titleKey = "action.formatProject.title",
                failureMessageKey = "action.formatProject.failed",
            ),
        ) { runner, indicator ->
            val cli = TypeSpecCliResolver.resolveTspCli(project, projectRoot)
                ?: return@runCliJob TypeSpecCliJobResult.CliUnavailable
            runner.run(cli, listOf("format", "."), TypeSpecBundle.message("action.formatProject.progress"), indicator)
                .toJobResult()
        }
    }

    private fun findTypeSpecProjectRoot(project: Project?): Path? {
        val basePath = project?.basePath?.let { Paths.get(it) } ?: return null
        return TypeSpecProjectContext.findProjectRoot(basePath)
    }
}
