package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

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
        val confirmed = AtomicBoolean(false)
        ApplicationManager.getApplication().invokeAndWait {
            confirmed.set(
                Messages.showYesNoDialog(
                    project,
                    TypeSpecBundle.message(warningMessageKey),
                    TypeSpecBundle.message(titleKey),
                    Messages.getWarningIcon(),
                ) == Messages.YES,
            )
        }
        return confirmed.get()
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
