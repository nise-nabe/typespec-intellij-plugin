package com.example.typespec

import com.intellij.lang.typescript.lsp.bindPackage
import com.intellij.lang.typescript.lsp.createNodePackageField
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.UiDslUnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind

class TypeSpecSettingsConfigurable(
    private val project: Project,
) : UiDslUnnamedConfigurable.Simple(), Configurable {
    private val settings = TypeSpecServiceSettings.getInstance(project)

    override fun Panel.createContent() {
        group(TypeSpecBundle.message("settings.typespec.service.group")) {
            row(TypeSpecBundle.message("settings.typespec.service.languageServerPackage")) {
                cell(createNodePackageField(project, TypeSpecLspServerLoader.packageDescriptor))
                    .align(AlignX.FILL)
                    .bindPackage(settings::lspServerPackage)
            }

            buttonsGroup {
                row {
                    radioButton(
                        TypeSpecBundle.message("settings.typespec.service.disabled"),
                        TypeSpecServiceMode.DISABLED,
                    ).comment(TypeSpecBundle.message("settings.typespec.service.disabled.help"))
                }
                row {
                    radioButton(
                        TypeSpecBundle.message("settings.typespec.service.enabled"),
                        TypeSpecServiceMode.ENABLED,
                    ).comment(TypeSpecBundle.message("settings.typespec.service.enabled.help"))
                }
            }.apply {
                bind(settings::serviceMode)
            }
        }
    }

    override fun getDisplayName(): String = TypeSpecBundle.message("settings.typespec.title")
}
