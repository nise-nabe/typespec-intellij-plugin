package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

internal object TypeSpecWorkflowGuards {
    /**
     * Must be called from a background thread. Scans [targetFolder] off EDT; shows the confirmation
     * dialog on EDT when the directory already has entries.
     */
    fun confirmWriteToNonEmptyDirectory(
        project: Project,
        targetFolder: Path,
        warningMessageKey: String,
        titleKey: String,
    ): Boolean {
        if (!directoryHasEntries(targetFolder)) {
            return true
        }
        return showConfirmDialogOnEdt(project, warningMessageKey, titleKey)
    }

    private fun showConfirmDialogOnEdt(
        project: Project,
        warningMessageKey: String,
        titleKey: String,
    ): Boolean {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) {
            return showConfirmDialog(project, warningMessageKey, titleKey)
        }
        val confirmed = AtomicBoolean(false)
        app.invokeAndWait {
            confirmed.set(showConfirmDialog(project, warningMessageKey, titleKey))
        }
        return confirmed.get()
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
