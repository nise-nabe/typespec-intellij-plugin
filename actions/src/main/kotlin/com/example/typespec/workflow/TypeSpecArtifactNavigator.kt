package com.example.typespec.workflow

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
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
            ?.trim()
            ?.ifEmpty { null }
            ?: return projectRoot.resolve("tsp-output").normalize()
        val substituted = configured
            .replace("{project-root}", projectRoot.toString())
            .replace("{cwd}", projectRoot.toString())
            .removePrefix("./")
        return projectRoot.resolve(substituted).normalize()
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
        ApplicationManager.getApplication().executeOnPooledThread {
            val outputDirectory = resolveOutputDirectory(projectRoot)
            val artifact = findPrimaryArtifact(outputDirectory) ?: outputDirectory
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(artifact)
                ?: return@executeOnPooledThread
            val isFile = Files.isRegularFile(artifact)
            ApplicationManager.getApplication().invokeLater {
                if (isFile) {
                    FileEditorManager.getInstance(project).openFile(virtualFile, true)
                } else {
                    ProjectView.getInstance(project).select(null, virtualFile, true)
                }
            }
        }
    }
}
