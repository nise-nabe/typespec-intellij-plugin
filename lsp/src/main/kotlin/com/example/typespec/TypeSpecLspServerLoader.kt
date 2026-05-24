package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.typescript.lsp.LspServerLoader
import com.intellij.openapi.project.Project

@Suppress("UnstableApiUsage")
object TypeSpecLspServerLoader : LspServerLoader(TypeSpecPackageDescriptors.lspServer) {
    override fun getSelectedPackage(project: Project): NodePackage =
        TypeSpecCompilerPackageResolver.getSelectedPackage(project)

    fun isSelectedPackageResolvable(project: Project): Boolean =
        TypeSpecPackageResolutionCache.getInstance(project).getOrCompute(project).lspServerResolvable
}
