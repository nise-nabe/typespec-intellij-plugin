package com.example.typespec.workflow

import com.example.typespec.TypeSpecContentRootResolver
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

internal object TypeSpecProjectContextExtensions {
    fun resolveFromVirtualFile(project: Project, file: VirtualFile): TypeSpecProjectResolution? {
        if (!file.isInLocalFileSystem) {
            return null
        }
        val path = Paths.get(file.path)
        val contentRoots = TypeSpecContentRootResolver.probeDirectories(project)
        if (file.extension == "tsp") {
            val projectRoot = TypeSpecContentRootResolver.findNearestTypeSpecProjectRoot(path.parent, contentRoots)
                ?: TypeSpecProjectContext.findProjectRoot(path.parent)
                ?: path.parent
            val entrypoint = TypeSpecProjectContext.resolveEntrypointFile(projectRoot, path)
            return TypeSpecProjectResolution(projectRoot, entrypoint, path)
        }
        if (file.name == TypeSpecProjectContext.TSP_CONFIG_FILE_NAME) {
            val projectRoot = path.parent
            val entrypoint = TypeSpecProjectContext.resolveEntrypointFile(projectRoot, null)
            return TypeSpecProjectResolution(projectRoot, entrypoint, path)
        }
        return null
    }
}
