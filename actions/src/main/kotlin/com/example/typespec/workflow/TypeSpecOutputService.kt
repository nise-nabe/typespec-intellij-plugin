package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.Font
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class TypeSpecOutputService {
    private val textArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }

    @Synchronized
    fun append(line: String) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val entry = "[$timestamp] $line"
        SwingUtilities.invokeLater {
            if (textArea.text.isNotEmpty()) {
                textArea.append("\n")
            }
            textArea.append(entry)
            textArea.caretPosition = textArea.document.length
        }
    }

    fun clear() {
        SwingUtilities.invokeLater {
            textArea.text = ""
        }
    }

    fun consoleComponent(): JComponent = JBScrollPane(textArea)

    fun show(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return
        toolWindow.activate(null)
    }

    companion object {
        const val TOOL_WINDOW_ID = "TypeSpec Output"

        fun getInstance(project: Project): TypeSpecOutputService = project.getService(TypeSpecOutputService::class.java)
    }
}
