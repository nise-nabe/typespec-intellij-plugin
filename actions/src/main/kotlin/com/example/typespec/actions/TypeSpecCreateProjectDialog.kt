package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent

internal class TypeSpecCreateProjectDialog : DialogWrapper(true) {
    private var targetDirectory: String = ""
    private var template: String = DEFAULT_TEMPLATE

    init {
        title = TypeSpecBundle.message("action.createProject.title")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row(TypeSpecBundle.message("action.createProject.directory")) {
            textFieldWithBrowseButton(
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle(TypeSpecBundle.message("action.createProject.directory")),
            ).bindText(::targetDirectory)
        }
        row(TypeSpecBundle.message("action.createProject.template")) {
            comboBox(TEMPLATES.toList())
                .onChanged { template = it?.toString() ?: DEFAULT_TEMPLATE }
                .apply { component.selectedItem = template }
        }
    }

    fun targetPath(): Path? {
        val path = targetDirectory.trim()
        if (path.isEmpty()) {
            return null
        }
        return Paths.get(path)
    }

    fun selectedTemplate(): String = template

    companion object {
        private const val DEFAULT_TEMPLATE = "default"
        private val TEMPLATES = arrayOf("default", "library-ts", "emitter-ts")
    }
}
