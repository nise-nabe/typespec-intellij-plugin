package com.example.typespec.workflow

import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.nio.file.Path

internal class TypeSpecCliRunner(
    private val project: Project,
) {
    fun run(
        baseCommand: TypeSpecCliCommand,
        additionalArgs: List<String>,
        title: String,
        indicator: ProgressIndicator? = null,
    ): TypeSpecCliProcessOutcome {
        val output = TypeSpecOutputService.getInstance(project)
        val command = baseCommand.copy(
            args = baseCommand.args + additionalArgs,
            displayCommand = baseCommand.displayCommand + " " + additionalArgs.joinToString(" "),
        )
        output.append(title)
        output.append("> ${command.displayCommand}")
        output.append("  cwd: ${command.workingDirectory}")

        return try {
            val processHandler = OSProcessHandler(command.toGeneralCommandLine())
            var exitCode = -1
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    event.text?.trimEnd()?.lines()?.forEach { line ->
                        if (line.isNotEmpty()) {
                            output.append(line)
                        }
                    }
                }

                override fun processTerminated(event: ProcessEvent) {
                    output.append("Process finished with exit code ${event.exitCode}")
                    exitCode = event.exitCode
                }
            })
            processHandler.startNotify()
            while (!processHandler.isProcessTerminated) {
                if (indicator?.isCanceled == true) {
                    processHandler.destroyProcess()
                    output.append("Process cancelled by user")
                    return TypeSpecCliProcessOutcome.Cancelled
                }
                Thread.sleep(50)
            }
            processHandler.waitFor()
            TypeSpecCliProcessOutcome.Exited(exitCode)
        } catch (e: Exception) {
            output.append("Failed to start process: ${e.message}")
            TypeSpecCliProcessOutcome.FailedToStart(e.message ?: e.toString())
        }
    }

    fun compile(
        projectRoot: Path,
        entrypoint: Path,
        emitters: List<String>,
        extraArgs: List<String> = emptyList(),
        indicator: ProgressIndicator? = null,
    ): TypeSpecCliJobResult {
        val cli = TypeSpecCliResolver.resolveTspCli(project, projectRoot) ?: return TypeSpecCliJobResult.CliUnavailable
        val request = TypeSpecCompileRequest(
            projectRoot = projectRoot,
            entrypoint = entrypoint,
            emitters = emitters,
            extraArgs = extraArgs,
        )
        return run(cli, buildTypeSpecCompileTspArgs(request), "TypeSpec compile", indicator).toJobResult()
    }
}
