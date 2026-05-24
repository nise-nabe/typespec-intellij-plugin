package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class TypeSpecToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val output = TypeSpecOutputService.getInstance(project)
        val content = ContentFactory.getInstance().createContent(
            output.consoleComponent(),
            TypeSpecBundle.message("toolWindow.typespecOutput.title"),
            false,
        )
        toolWindow.contentManager.addContent(content)
    }
}
