package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Files
import java.nio.file.Path

internal object TypeSpecWorkflowGuards {
    /**
     * Must be called on the EDT before starting a background CLI job. Scans [targetFolder] and shows
     * a confirmation dialog when the directory already has entries.
     */
    fun confirmWriteToNonEmptyDirectory(
        project: Project,
        targetFolder: Path,
        warningMessageKey: String,
        titleKey: String,
    ): Boolean {
        check(ApplicationManager.getApplication().isDispatchThread) {
            "confirmWriteToNonEmptyDirectory must be called on the EDT"
        }
        if (!directoryHasEntries(targetFolder)) {
            return true
        }
        return showConfirmDialog(project, warningMessageKey, titleKey)
    }

    private fun showConfirmDialog(
        project: Project,
        warningMessageKey: String,
        titleKey: String,
    ): Boolean =
        Messages.showYesNoDialog(
            project,
            TypeSpecBundle.message(warningMessageKey),
            TypeSpecBundle.message(titleKey),
            Messages.getWarningIcon(),
        ) == Messages.YES

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
