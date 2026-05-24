package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

internal object TypeSpecWorkflowOutcomes {
    fun presentWarningOnEdt(project: Project, messageKey: String, titleKey: String, exitCode: Int) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showWarningDialog(
                project,
                TypeSpecBundle.message(messageKey, exitCode),
                TypeSpecBundle.message(titleKey),
            )
        }
    }

    fun presentErrorOnEdt(project: Project, message: String, titleKey: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                project,
                message,
                TypeSpecBundle.message(titleKey),
            )
        }
    }
}
