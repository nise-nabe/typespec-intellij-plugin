package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JPanel

internal object TypeSpecJcefPreviewSupport {
    fun isSupported(): Boolean =
        try {
            val jcefAppClass = Class.forName("com.intellij.ui.jcef.JBCefApp")
            val isSupported = jcefAppClass.getMethod("isSupported").invoke(null) as Boolean
            isSupported
        } catch (_: Exception) {
            false
        }

    fun showHtml(project: Project, html: String): Boolean {
        if (!isSupported()) {
            return false
        }
        return try {
            val browserClass = Class.forName("com.intellij.ui.jcef.JBCefBrowser")
            val browser = browserClass.getConstructor().newInstance()
            browserClass.getMethod("loadHTML", String::class.java).invoke(browser, html)
            val component = browserClass.getMethod("getComponent").invoke(browser) as java.awt.Component

            ApplicationManager.getApplication().invokeLater {
                val toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow(TypeSpecApiPreviewToolWindowFactory.TOOL_WINDOW_ID) ?: return@invokeLater
                val panel = JPanel(BorderLayout()).apply { add(component, BorderLayout.CENTER) }
                val content = ContentFactory.getInstance().createContent(
                    panel,
                    TypeSpecBundle.message("toolWindow.typespecApiPreview.title"),
                    false,
                )
                toolWindow.contentManager.removeAllContents(true)
                toolWindow.contentManager.addContent(content)
                toolWindow.activate(null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
