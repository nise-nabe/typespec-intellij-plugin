package com.example.typespec.workflow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JLabel
import javax.swing.SwingConstants

class TypeSpecApiPreviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val placeholder = JLabel("Generate an OpenAPI preview to display documentation here.", SwingConstants.CENTER)
        val content = ContentFactory.getInstance().createContent(placeholder, "", false)
        toolWindow.contentManager.addContent(content)
    }

    companion object {
        const val TOOL_WINDOW_ID = "TypeSpec API Preview"
    }
}
