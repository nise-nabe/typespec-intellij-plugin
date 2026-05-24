package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object TypeSpecCompilerPackageResolver {
    private const val SERVER_SCRIPT_RELATIVE_PATH = "cmd/tsp-server.js"

    fun hasProjectLocalCompilerPackage(project: Project): Boolean =
        hasProjectLocalCompilerPackage(project.basePath)

    internal fun hasProjectLocalCompilerPackage(basePath: String?): Boolean =
        findProjectLocalPackageDirectory(basePath) != null

    fun isCompilerPackageResolvable(project: Project): Boolean =
        isPackageWithServerScript(TypeSpecLspServerLoader.getSelectedPackage(project))

    internal fun findProjectLocalPackageDirectory(basePath: String?): Path? {
        if (basePath == null) {
            return null
        }
        val packageDirectory = Paths.get(basePath, "node_modules", TYPESPEC_COMPILER_PACKAGE_NAME)
        return packageDirectory.takeIf { Files.isDirectory(it) && Files.exists(it.resolve("package.json")) }
    }

    internal fun isPackageWithServerScript(nodePackage: NodePackage): Boolean {
        val packageDirectory = Paths.get(nodePackage.systemDependentPath)
        if (!Files.isDirectory(packageDirectory)) {
            return false
        }
        return Files.exists(packageDirectory.resolve(SERVER_SCRIPT_RELATIVE_PATH))
    }
}
