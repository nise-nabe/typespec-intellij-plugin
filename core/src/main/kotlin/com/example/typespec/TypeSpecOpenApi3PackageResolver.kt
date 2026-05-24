package com.example.typespec

import java.nio.file.Files
import java.nio.file.Path

object TypeSpecOpenApi3PackageResolver {
    fun findPackageDirectory(probeDirectory: Path, compilerPackageDirectory: Path): Path? =
        candidatePackageDirectories(probeDirectory, compilerPackageDirectory)
            .firstOrNull { hasCliScript(it) }

    fun isResolvable(probeDirectory: Path, compilerPackageDirectory: Path): Boolean =
        findPackageDirectory(probeDirectory, compilerPackageDirectory) != null

    fun hasCliScript(packageDirectory: Path): Boolean {
        if (!Files.isDirectory(packageDirectory)) {
            return false
        }
        return Files.isRegularFile(packageDirectory.resolve(TYPESPEC_OPENAPI3_CLI_SCRIPT))
    }

    fun candidatePackageDirectories(probeDirectory: Path, compilerPackageDirectory: Path): List<Path> =
        listOfNotNull(
            probeDirectory.resolve("node_modules").resolve(TYPESPEC_OPENAPI3_PACKAGE_NAME),
            compilerPackageDirectory.parent?.resolve(TYPESPEC_OPENAPI3_PACKAGE_NAME),
        )

    fun openApi3PackagePathUnderProject(projectBasePath: String): String? {
        val normalizedBase = normalizePackageRoot(projectBasePath) ?: return null
        return "$normalizedBase/node_modules/$TYPESPEC_OPENAPI3_PACKAGE_NAME"
    }
}

fun vfsEventAffectsOpenApi3Package(eventPath: String, projectBasePath: String?): Boolean {
    val openApiRoot = projectBasePath?.let { TypeSpecOpenApi3PackageResolver.openApi3PackagePathUnderProject(it) }
        ?: return false
    return vfsPathIsUnderPackageRoot(eventPath, openApiRoot)
}
