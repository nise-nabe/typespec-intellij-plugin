package com.example.typespec

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor

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
        if (!TypeSpecUtil.isNodeJsConfigured(project)) {
            throw ExecutionException("Node.js interpreter is not configured")
        }

        val commandLine = GeneralCommandLine()
        val tspPath = TypeSpecUtil.findTspExecutable(project)
        
        commandLine.exePath = tspPath
        commandLine.addParameter("compile")
        commandLine.addParameter("--lsp")
        commandLine.setWorkDirectory(project.basePath)
        
        return commandLine
    }
}
