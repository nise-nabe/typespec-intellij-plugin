package com.example.typespec.workflow

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecArtifactNavigator {
    private val preferredArtifactNames = listOf(
        "openapi.yaml",
        "openapi.yml",
        "openapi.json",
    )

    fun resolveOutputDirectory(projectRoot: Path): Path {
        val configured = TypeSpecTspConfigReader.readOutputDir(projectRoot)
        val relative = configured?.trim()?.removePrefix("./")?.ifEmpty { null } ?: "tsp-output"
        return projectRoot.resolve(relative).normalize()
    }

    fun findPrimaryArtifact(outputDirectory: Path): Path? {
        if (!Files.isDirectory(outputDirectory)) {
            return null
        }
        for (name in preferredArtifactNames) {
            val candidate = outputDirectory.resolve(name)
            if (Files.isRegularFile(candidate)) {
                return candidate
            }
        }
        return Files.list(outputDirectory).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .sorted()
                .findFirst()
                .orElse(null)
        }
    }

    fun revealOutput(project: Project, projectRoot: Path) {
        val outputDirectory = resolveOutputDirectory(projectRoot)
        val artifact = findPrimaryArtifact(outputDirectory) ?: outputDirectory
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(artifact.toString()) ?: return
        if (Files.isRegularFile(artifact)) {
            OpenFileDescriptor(project, virtualFile).navigate(true)
        } else {
            com.intellij.openapi.wm.ex.ToolWindowManagerEx.getInstanceEx(project)
                .activateToolWindow("Project", true, true)
        }
    }
}
