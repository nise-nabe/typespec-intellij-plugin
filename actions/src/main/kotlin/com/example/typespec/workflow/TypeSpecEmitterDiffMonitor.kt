package com.example.typespec.workflow

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecEmitterDiffMonitor {
    fun readIfRegularFile(artifact: Path): String? {
        if (!Files.isRegularFile(artifact)) {
            return null
        }
        return try {
            Files.readString(artifact)
        } catch (_: IOException) {
            null
        }
    }

    fun showDiff(project: Project, artifact: Path, before: String, after: String) {
        val factory = DiffContentFactory.getInstance()
        val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(artifact.fileName.toString())
        val request = SimpleDiffRequest(
            "TypeSpec emitter output",
            factory.create(before, fileType),
            factory.create(after, fileType),
            "Before compile",
            "After compile",
        )
        DiffManager.getInstance().showDiff(project, request)
    }
}
