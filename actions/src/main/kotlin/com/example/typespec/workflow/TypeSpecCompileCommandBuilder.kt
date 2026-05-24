package com.example.typespec.workflow

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project

internal object TypeSpecCompileCommandBuilder {
    fun buildCommandLine(project: Project, request: TypeSpecCompileRequest): GeneralCommandLine? {
        val cli = TypeSpecCliResolver.resolveTspCli(project, request.projectRoot) ?: return null
        val args = cli.args + buildTypeSpecCompileTspArgs(request)
        return GeneralCommandLine()
            .withExePath(cli.executable)
            .withParameters(args)
            .withWorkDirectory(request.projectRoot.toFile())
            .withEnvironment("TYPESPEC_SKIP_COMPILER_RESOLVE", "1")
    }
}
