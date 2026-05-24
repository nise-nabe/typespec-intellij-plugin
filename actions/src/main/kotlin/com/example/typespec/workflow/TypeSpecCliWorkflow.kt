package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.example.typespec.actions.TypeSpecActionSupport
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

internal object TypeSpecCliWorkflow {
    fun prepareOutput(project: Project): TypeSpecOutputService {
        val output = TypeSpecOutputService.getInstance(project)
        output.show(project)
        output.clear()
        return output
    }

    fun runBackground(
        project: Project,
        progressMessageKey: String,
        cancellable: Boolean = true,
        task: (ProgressIndicator) -> Unit,
    ) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, TypeSpecBundle.message(progressMessageKey), cancellable) {
                override fun run(indicator: ProgressIndicator) {
                    task(indicator)
                }
            },
        )
    }

    fun showCompilerMissing(
        project: Project,
        titleKey: String,
        messageKey: String = "workflow.compilerMissing",
    ) {
        TypeSpecActionSupport.showCompilerMissing(project, titleKey, messageKey)
    }
}
