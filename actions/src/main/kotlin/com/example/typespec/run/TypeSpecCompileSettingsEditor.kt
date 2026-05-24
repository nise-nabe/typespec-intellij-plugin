package com.example.typespec.run

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TypeSpecCompileSettingsEditor(
    private val project: Project,
) : SettingsEditor<TypeSpecCompileRunConfiguration>() {
    private var entrypointPath: String = ""
    private var projectRootPath: String = ""
    private var emitters: String = ""
    private var extraArgs: String = ""
    private var watch: Boolean = false

    override fun createEditor(): JComponent = panel {
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.projectRoot")) {
            textField().bindText(::projectRootPath)
        }
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.entrypoint")) {
            textField().bindText(::entrypointPath)
        }
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.emitters")) {
            textField().bindText(::emitters)
        }
        row(TypeSpecBundle.message("runConfiguration.typespecCompile.extraArgs")) {
            textField().bindText(::extraArgs)
        }
        row {
            checkBox(TypeSpecBundle.message("runConfiguration.typespecCompile.watch"))
                .bindSelected(::watch)
        }
    }

    override fun resetEditorFrom(configuration: TypeSpecCompileRunConfiguration) {
        val settings = configuration.settings()
        entrypointPath = settings.entrypointPath
        projectRootPath = settings.projectRootPath
        emitters = settings.emitters
        extraArgs = settings.extraArgs
        watch = settings.watch
    }

    override fun applyEditorTo(configuration: TypeSpecCompileRunConfiguration) {
        val settings = configuration.settings()
        settings.entrypointPath = entrypointPath
        settings.projectRootPath = projectRootPath
        settings.emitters = emitters
        settings.extraArgs = extraArgs
        settings.watch = watch
    }
}
