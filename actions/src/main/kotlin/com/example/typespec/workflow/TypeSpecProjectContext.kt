package com.example.typespec.workflow

import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object TypeSpecProjectContext {
    const val TSP_CONFIG_FILE_NAME = "tspconfig.yaml"
    private val DEFAULT_ENTRYPOINT_CANDIDATES = listOf("client.tsp", "entrypoint.tsp", "main.tsp")

    fun findProjectRoot(startDirectory: Path): Path? {
        var current: Path? = startDirectory.toAbsolutePath().normalize()
        while (current != null) {
            val config = current.resolve(TSP_CONFIG_FILE_NAME)
            if (Files.isRegularFile(config)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    fun resolveEntrypointFile(projectRoot: Path, preferredFile: Path?): Path? {
        if (preferredFile != null && Files.isRegularFile(preferredFile)) {
            return preferredFile.toAbsolutePath().normalize()
        }
        for (name in DEFAULT_ENTRYPOINT_CANDIDATES) {
            val candidate = projectRoot.resolve(name)
            if (Files.isRegularFile(candidate)) {
                return candidate
            }
        }
        return null
    }

    fun resolveFromVirtualFile(file: VirtualFile): TypeSpecProjectResolution? {
        if (!file.isInLocalFileSystem) {
            return null
        }
        val path = Paths.get(file.path)
        if (file.extension == "tsp") {
            val projectRoot = findProjectRoot(path.parent) ?: path.parent
            val entrypoint = resolveEntrypointFile(projectRoot, path)
            return TypeSpecProjectResolution(projectRoot, entrypoint, path)
        }
        if (file.name == TSP_CONFIG_FILE_NAME) {
            val projectRoot = path.parent
            val entrypoint = resolveEntrypointFile(projectRoot, null)
            return TypeSpecProjectResolution(projectRoot, entrypoint, path)
        }
        return null
    }
}

internal data class TypeSpecProjectResolution(
    val projectRoot: Path,
    val entrypointFile: Path?,
    val contextFile: Path,
)
