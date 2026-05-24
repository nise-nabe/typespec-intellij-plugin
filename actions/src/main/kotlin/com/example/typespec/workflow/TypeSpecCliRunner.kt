package com.example.typespec.workflow

import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

internal class TypeSpecCliRunner(
    private val project: Project,
) {
    fun run(
        baseCommand: TypeSpecCliCommand,
        additionalArgs: List<String>,
        title: String,
    ): CompletableFuture<Int> {
        val output = TypeSpecOutputService.getInstance(project)
        val command = baseCommand.copy(
            args = baseCommand.args + additionalArgs,
            displayCommand = baseCommand.displayCommand + " " + additionalArgs.joinToString(" "),
        )
        output.append("$title")
        output.append("> ${command.displayCommand} ${additionalArgs.joinToString(" ")}")
        output.append("  cwd: ${command.workingDirectory}")

        val future = CompletableFuture<Int>()
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val processHandler = OSProcessHandler(command.toGeneralCommandLine())
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
                        future.complete(event.exitCode)
                    }
                })
                processHandler.startNotify()
                processHandler.waitFor()
            } catch (e: Exception) {
                output.append("Failed to start process: ${e.message}")
                future.complete(-1)
            }
        }
        return future
    }

    fun compile(
        project: Project,
        projectRoot: Path,
        entrypoint: Path,
        emitters: List<String>,
        extraArgs: List<String> = emptyList(),
    ): CompletableFuture<Int>? {
        val cli = TypeSpecCliResolver.resolveTspCli(project, projectRoot) ?: return null
        val args = buildList {
            add("compile")
            add(entrypoint.toString())
            emitters.forEach { emitter ->
                add("--emit")
                add(emitter)
            }
            addAll(extraArgs)
        }
        return run(cli, args, "TypeSpec compile")
    }
}
