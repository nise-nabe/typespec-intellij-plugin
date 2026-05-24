package com.example.typespec.workflow

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal data class TypeSpecCompileRoots(
    val projectRoot: Path,
    val entrypoint: Path,
)

internal object TypeSpecCompileRootsResolver {
    fun resolve(entrypointPath: String, projectRootPath: String): TypeSpecCompileRoots? {
        if (entrypointPath.isBlank() && projectRootPath.isBlank()) {
            return null
        }
        val projectRootCandidate = when {
            projectRootPath.isNotBlank() -> Paths.get(projectRootPath)
            entrypointPath.isNotBlank() -> Paths.get(entrypointPath)
            else -> return null
        }
        val resolvedRoot = if (Files.isDirectory(projectRootCandidate)) {
            TypeSpecProjectContext.findProjectRoot(projectRootCandidate) ?: projectRootCandidate
        } else {
            TypeSpecProjectContext.findProjectRoot(projectRootCandidate.parent) ?: projectRootCandidate.parent
        }
        val entrypoint = when {
            entrypointPath.isNotBlank() -> Paths.get(entrypointPath)
            else -> TypeSpecProjectContext.resolveEntrypointFile(resolvedRoot, null) ?: return null
        }
        return TypeSpecCompileRoots(resolvedRoot, entrypoint)
    }
}
