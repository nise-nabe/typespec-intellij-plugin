package com.example.typespec.workflow

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project

internal object TypeSpecCompileCommandBuilder {
    fun buildCommandLine(project: Project, request: TypeSpecCompileRequest): GeneralCommandLine? {
        val cli = TypeSpecCliResolver.resolveTspCli(project, request.projectRoot) ?: return null
        return cli.copy(args = cli.args + buildTypeSpecCompileTspArgs(request)).toGeneralCommandLine()
    }
}
