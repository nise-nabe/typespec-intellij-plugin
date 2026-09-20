package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JLabel
import javax.swing.SwingConstants

class TypeSpecApiPreviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val placeholder = JLabel(
            TypeSpecBundle.message("toolWindow.typespecApiPreview.placeholder"),
            SwingConstants.CENTER,
        )
        val content = ContentFactory.getInstance().createContent(
            placeholder,
            TypeSpecBundle.message("toolWindow.typespecApiPreview.title"),
            false,
        )
        toolWindow.contentManager.addContent(content)
    }

    companion object {
        const val TOOL_WINDOW_ID = "TypeSpec API Preview"
    }
}
