package com.example.typespec.workflow

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import java.awt.Font
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class TypeSpecOutputService {
    private val generalArea = createTextArea()
    private val traceArea = createTextArea()
    private val tabbedPane = JBTabbedPane().apply {
        addTab(TypeSpecBundle.message("toolWindow.typespecOutput.tab.general"), JBScrollPane(generalArea))
        addTab(TypeSpecBundle.message("toolWindow.typespecOutput.tab.trace"), JBScrollPane(traceArea))
    }

    fun append(line: String) {
        appendTo(generalArea, line)
    }

    fun appendTrace(line: String) {
        appendTo(traceArea, line)
    }

    fun clear() {
        SwingUtilities.invokeLater {
            generalArea.text = ""
            traceArea.text = ""
        }
    }

    fun consoleComponent(): JComponent = tabbedPane

    fun show(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return
        toolWindow.activate(null)
    }

    private fun appendTo(area: JBTextArea, line: String) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val entry = "[$timestamp] $line"
        SwingUtilities.invokeLater {
            if (area.text.isNotEmpty()) {
                area.append("\n")
            }
            area.append(entry)
            area.caretPosition = area.document.length
        }
    }

    private fun createTextArea(): JBTextArea =
        JBTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        }

    companion object {
        const val TOOL_WINDOW_ID = "TypeSpec Output"

        fun getInstance(project: Project): TypeSpecOutputService = project.getService(TypeSpecOutputService::class.java)
    }
}
