package com.example.typespec

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor

class TypeSpecLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        TODO("Not implemented")
    }
}

class TypeSpecLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "TypeSpec") {
    override fun isSupportedFile(file: VirtualFile): Boolean = file.fileType == TypeSpecFileType

    override fun createCommandLine(): GeneralCommandLine {
        TODO("Not implemented")
    }
}
