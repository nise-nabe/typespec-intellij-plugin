package com.example.typespec

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider

class TypeSpecLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (file.fileType == TypeSpecFileType) {
            serverStarter.ensureServerStarted(TypeSpecLspServerDescriptor(project))
        }
    }
}

class TypeSpecLspServerDescriptor(project: Project) : LspServerDescriptor(project, "TypeSpec") {
    override fun isSupportedFile(file: VirtualFile): Boolean = file.fileType == TypeSpecFileType

    override fun createCommandLine(): GeneralCommandLine {
        return GeneralCommandLine()
    }
}
