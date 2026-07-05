package com.example.typespec.workflow

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecEmitterDiffMonitor {
    private val snapshots = mutableMapOf<String, String>()

    fun captureBefore(projectKey: String, artifact: Path) {
        if (!Files.isRegularFile(artifact)) {
            snapshots.remove(projectKey)
            return
        }
        snapshots[projectKey] = Files.readString(artifact)
    }

    fun showDiffIfChanged(project: Project, projectKey: String, artifact: Path) {
        val before = snapshots.remove(projectKey) ?: return
        if (!Files.isRegularFile(artifact)) {
            return
        }
        val after = Files.readString(artifact)
        if (before == after) {
            return
        }
        ApplicationManager.getApplication().invokeLater {
            val factory = DiffContentFactory.getInstance()
            val request = SimpleDiffRequest(
                "TypeSpec emitter output",
                factory.create(project, before),
                factory.create(project, after),
                "Before compile",
                "After compile",
            )
            DiffManager.getInstance().showDiff(project, request)
        }
    }
}
