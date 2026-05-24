package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.typescript.lsp.LspServerLoader
import com.intellij.lang.typescript.lsp.LspServerPackageDescriptor
import com.intellij.lang.typescript.lsp.PackageVersion
import com.intellij.openapi.project.Project
private const val TYPESPEC_SERVER_SCRIPT_PATH = "/cmd/tsp-server.js"

@Suppress("UnstableApiUsage")
object TypeSpecLspServerLoader : LspServerLoader(TypeSpecLspServerPackageDescriptor) {
    override fun getSelectedPackage(project: Project): NodePackage =
        TypeSpecCompilerPackageResolver.getSelectedPackage(project)

    fun isSelectedPackageResolvable(project: Project): Boolean =
        TypeSpecPackageResolutionCache.getInstance(project).getOrCompute(project).lspServerResolvable
}

@Suppress("UnstableApiUsage")
internal object TypeSpecLspServerPackageDescriptor : LspServerPackageDescriptor(
    TYPESPEC_COMPILER_PACKAGE_NAME,
    PackageVersion.downloadable("1.10.0"),
    TYPESPEC_SERVER_SCRIPT_PATH,
) {
    override val registryVersion: String
        get() = ""
}
