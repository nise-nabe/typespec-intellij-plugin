package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
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

    fun runCliJob(
        project: Project,
        spec: TypeSpecCliJobSpec,
        cancellable: Boolean = true,
        onExitCode: ((exitCode: Int) -> Unit)? = null,
        task: (TypeSpecCliRunner) -> Int?,
    ) {
        prepareOutput(project)
        runBackground(project, spec.progressMessageKey, cancellable) {
            when (val exitCode = task(TypeSpecCliRunner(project))) {
                null -> TypeSpecWorkflowOutcomes.presentCompilerMissingOnEdt(
                    project,
                    spec.titleKey,
                    spec.cliUnavailableMessageKey,
                )
                else -> {
                    onExitCode?.invoke(exitCode)
                    if (exitCode != 0 && spec.failureMessageKey != null) {
                        TypeSpecWorkflowOutcomes.presentWarningOnEdt(
                            project,
                            spec.failureMessageKey,
                            spec.titleKey,
                            exitCode,
                        )
                    }
                }
            }
        }
    }
}
