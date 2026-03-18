package com.example.typespec

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import kotlin.text.Charsets

class TypeSpecLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (file.fileType == TypeSpecFileType) {
            serverStarter.ensureServerStarted(TypeSpecLspServerDescriptor(project))
        }
    }
}

class TypeSpecLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "TypeSpec") {
    override fun isSupportedFile(file: VirtualFile): Boolean = file.fileType == TypeSpecFileType

    override fun createCommandLine(): GeneralCommandLine {
        if (NodeJsInterpreterManager.getInstance(project).interpreter == null) {
            throw ExecutionException("Node.js interpreter is not configured")
        }
        
        val tspCliJs = TypeSpecUtil.findTspCliJs(project)
        val tspExecutable = TypeSpecUtil.findTspExecutable(project)
        val commandLine = GeneralCommandLine()
        
        if (tspCliJs != null) {
            commandLine.exePath = "node" // Rely on PATH for node, but use absolute path for cli.js
            commandLine.addParameter(tspCliJs)
            commandLine.addParameter("server")
            commandLine.addParameter("--stdio")
        } else if (tspExecutable.contains("tsp-server")) {
            commandLine.exePath = tspExecutable
            commandLine.addParameter("--stdio")
        } else {
            commandLine.exePath = tspExecutable
            commandLine.addParameter("server")
            commandLine.addParameter("--stdio")
        }

        commandLine.setWorkDirectory(project.basePath)
        commandLine.charset = Charsets.UTF_8
        
        return commandLine
    }
}
