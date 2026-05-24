package com.example.typespec.workflow

import java.nio.file.Path

internal data class TypeSpecCliCommand(
    val executable: String,
    val args: List<String>,
    val workingDirectory: Path,
    val displayCommand: String,
)

internal fun TypeSpecCliCommand.toGeneralCommandLine(): com.intellij.execution.configurations.GeneralCommandLine {
    val line = com.intellij.execution.configurations.GeneralCommandLine()
        .withExePath(executable)
        .withWorkDirectory(workingDirectory.toFile())
    args.forEach { line.addParameter(it) }
    line.environment["TYPESPEC_SKIP_COMPILER_RESOLVE"] = "1"
    return line
}
