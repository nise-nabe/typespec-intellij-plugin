package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths

internal const val TYPESPEC_SERVER_SCRIPT_RELATIVE_PATH = "cmd/tsp-server.js"

internal object TypeSpecPackageResolution {
    fun getSelectedPackage(project: Project): NodePackage =
        TypeSpecServiceSettings.getInstance(project).lspServerPackage

    fun isPackageWithServerScript(nodePackage: NodePackage): Boolean {
        val packageDirectory = Paths.get(nodePackage.systemDependentPath)
        if (!Files.isDirectory(packageDirectory)) {
            return false
        }
        return Files.exists(packageDirectory.resolve(TYPESPEC_SERVER_SCRIPT_RELATIVE_PATH))
    }

    fun isSelectedPackageResolvable(project: Project): Boolean =
        TypeSpecLspPackageResolutionCache.getInstance(project).getOrCompute(project)
}
