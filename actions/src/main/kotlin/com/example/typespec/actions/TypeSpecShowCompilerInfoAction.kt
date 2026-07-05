package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.example.typespec.workflow.TypeSpecCliJobSpec
import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecCliWorkflow
import com.example.typespec.workflow.toJobResult
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import java.nio.file.Paths

class TypeSpecShowCompilerInfoAction : AnAction(
    TypeSpecBundle.message("action.showCompilerInfo.text"),
    TypeSpecBundle.message("action.showCompilerInfo.description"),
    null,
), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = TypeSpecActionSupport.updateActionThread()

    override fun update(event: AnActionEvent) {
        TypeSpecActionSupport.update(event, TypeSpecActionSupport.projectWithCompilerCliOnly)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val projectRoot = project.basePath?.let { Paths.get(it) } ?: Paths.get(".")

        TypeSpecCliWorkflow.runCliJob(
            project,
            TypeSpecCliJobSpec(
                progressMessageKey = "action.showCompilerInfo.progress",
                titleKey = "action.showCompilerInfo.title",
                failureMessageKey = "action.showCompilerInfo.failed",
            ),
        ) { runner, indicator ->
            val cli = TypeSpecCliResolver.resolveTspCli(project, projectRoot)
                ?: return@runCliJob com.example.typespec.workflow.TypeSpecCliJobResult.CliUnavailable
            runner.run(cli, listOf("info"), TypeSpecBundle.message("action.showCompilerInfo.progress"), indicator)
                .toJobResult()
        }
    }
}
