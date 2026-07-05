package com.example.typespec

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object TypeSpecContentRootResolver {
    fun probeDirectories(project: Project): List<Path> {
        val directories = linkedSetOf<Path>()
        project.basePath?.let { directories.add(Paths.get(it)) }
        if (!project.isDefault) {
            ProjectRootManager.getInstance(project).contentRoots.forEach { root ->
                directories.add(Paths.get(root.path))
            }
        }
        return directories.toList()
    }

    fun findNearestTypeSpecProjectRoot(startDirectory: Path, probeDirectories: List<Path>): Path? {
        val normalizedStart = startDirectory.toAbsolutePath().normalize()
        val candidates = probeDirectories.map { it.toAbsolutePath().normalize() }
            .filter { normalizedStart.startsWith(it) || it.startsWith(normalizedStart) }
        for (root in candidates) {
            val config = root.resolve("tspconfig.yaml")
            if (Files.isRegularFile(config)) {
                return root
            }
        }
        return null
    }
}
