package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.application.ApplicationManager
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
        if (!Files.exists(targetFolder)) {
            return true
        }
        val hasEntries = Files.list(targetFolder).use { stream -> stream.findAny().isPresent }
        if (!hasEntries) {
            return true
        }
        var proceed = false
        ApplicationManager.getApplication().invokeAndWait {
            proceed = Messages.showYesNoDialog(
                project,
                TypeSpecBundle.message(warningMessageKey),
                TypeSpecBundle.message(titleKey),
                Messages.getWarningIcon(),
            ) == Messages.YES
        }
        return proceed
    }
}
