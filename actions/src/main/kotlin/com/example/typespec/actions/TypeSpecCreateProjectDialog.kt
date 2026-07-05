package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent

internal class TypeSpecCreateProjectDialog : DialogWrapper(true) {
    private var targetDirectory: String = ""
    private var template: String = DEFAULT_TEMPLATE
    private var templateUrl: String = ""
    private var autoAccept: Boolean = false

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
                .onChanged { combo -> template = combo.selectedItem as? String ?: DEFAULT_TEMPLATE }
                .apply { component.selectedItem = template }
        }
        row(TypeSpecBundle.message("action.createProject.templateUrl")) {
            textField().bindText(::templateUrl)
        }
        row {
            checkBox(TypeSpecBundle.message("action.createProject.autoAccept"))
                .bindSelected(::autoAccept)
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

    fun templateUrl(): String? = templateUrl.trim().takeIf { it.isNotEmpty() }

    fun autoAcceptPrompts(): Boolean = autoAccept

    fun buildInitArgs(): List<String> = buildList {
        add("init")
        templateUrl()?.let { url ->
            add(url)
        } ?: run {
            if (template != DEFAULT_TEMPLATE) {
                add("--template")
                add(template)
            }
        }
        if (autoAccept) {
            add("-y")
        }
    }

    companion object {
        private const val DEFAULT_TEMPLATE = "default"
        private val TEMPLATES = arrayOf(
            "default",
            "generic-rest-api",
            "library-ts",
            "emitter-ts",
        )
    }
}
