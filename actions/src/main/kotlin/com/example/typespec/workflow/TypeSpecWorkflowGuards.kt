package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecWorkflowGuards {
    fun confirmWriteToNonEmptyDirectory(
        project: Project,
        targetFolder: Path,
        warningMessageKey: String,
        titleKey: String,
    ): Boolean {
        if (!directoryHasEntries(targetFolder)) {
            return true
        }
        return Messages.showYesNoDialog(
            project,
            TypeSpecBundle.message(warningMessageKey),
            TypeSpecBundle.message(titleKey),
            Messages.getWarningIcon(),
        ) == Messages.YES
    }

    fun ensureTargetDirectory(targetPath: Path): TargetDirectoryPrep {
        val existedBefore = Files.exists(targetPath)
        if (!existedBefore) {
            Files.createDirectories(targetPath)
        }
        return TargetDirectoryPrep(targetPath, createdForJob = !existedBefore)
    }

    fun rollbackCreatedDirectory(prep: TargetDirectoryPrep, jobResult: TypeSpecCliJobResult) {
        if (!prep.createdForJob) {
            return
        }
        if (jobResult is TypeSpecCliJobResult.Finished && jobResult.exitCode == 0) {
            return
        }
        TypeSpecOpenApiPreview.deleteRecursivelyQuietly(prep.path)
    }

    private fun directoryHasEntries(targetFolder: Path): Boolean {
        if (!Files.exists(targetFolder)) {
            return false
        }
        if (!Files.isDirectory(targetFolder)) {
            return true
        }
        return Files.newDirectoryStream(targetFolder).use { stream -> stream.iterator().hasNext() }
    }
}

internal data class TargetDirectoryPrep(
    val path: Path,
    val createdForJob: Boolean,
)
