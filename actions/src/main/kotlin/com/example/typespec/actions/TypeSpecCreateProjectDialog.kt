package com.example.typespec.actions

import com.example.typespec.TypeSpecBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.and
import com.intellij.ui.layout.enteredTextSatisfies
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

internal class TypeSpecCreateProjectDialog : DialogWrapper(true) {
    private val emitterChoices = TypeSpecCreateProjectDialog.REST_OPTIONAL_EMITTERS.map { EmitterChoice(it) }

    private lateinit var directoryField: TextFieldWithBrowseButton
    private lateinit var templateCombo: ComboBox<String>
    private lateinit var urlField: JTextField
    private lateinit var projectNameField: JTextField
    private lateinit var additionalEmittersField: JTextField

    init {
        title = TypeSpecBundle.message("action.createProject.title")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row(TypeSpecBundle.message("action.createProject.directory")) {
            directoryField = textFieldWithBrowseButton(
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle(TypeSpecBundle.message("action.createProject.directory")),
            ).component
        }
        row(TypeSpecBundle.message("action.createProject.template")) {
            templateCombo = comboBox(TEMPLATES)
                .applyToComponent {
                    isEditable = true
                    selectedItem = DEFAULT_TEMPLATE
                    (editor.editorComponent as? JTextField)?.text = DEFAULT_TEMPLATE
                }
                .component
        }
        row(TypeSpecBundle.message("action.createProject.templateUrl")) {
            urlField = textField()
                .comment(TypeSpecBundle.message("action.createProject.templateUrl.comment"))
                .component
        }
        row(TypeSpecBundle.message("action.createProject.projectName")) {
            projectNameField = textField()
                .comment(TypeSpecBundle.message("action.createProject.projectName.comment"))
                .component
        }
        val builtInRest = templateCombo.editorTextSatisfies { it.trim() == REST_TEMPLATE }
            .and(urlField.enteredTextSatisfies { it.isBlank() })
        group(TypeSpecBundle.message("action.createProject.emitters")) {
            row {
                checkBox(TypeSpecBundle.message("action.createProject.emitter.openapi3"))
                    .applyToComponent {
                        isSelected = true
                        isEnabled = false
                    }
            }
            for (choice in emitterChoices) {
                row {
                    choice.checkBox = checkBox(TypeSpecBundle.message(choice.option.messageKey)).component
                }
            }
            row {
                comment(TypeSpecBundle.message("action.createProject.emitters.comment"))
            }
        }.visibleIf(builtInRest)
        row(TypeSpecBundle.message("action.createProject.additionalEmitters")) {
            additionalEmittersField = textField()
                .comment(TypeSpecBundle.message("action.createProject.additionalEmitters.comment"))
                .component
        }.visibleIf(urlField.enteredTextSatisfies { it.isNotBlank() })
    }

    override fun doValidate(): ValidationInfo? {
        val directory = directoryField.text.trim()
        if (directory.isEmpty()) {
            return ValidationInfo(
                TypeSpecBundle.message("action.createProject.directoryRequired"),
                directoryField.textField,
            )
        }
        val templateName = currentTemplate()
        if (templateName.isEmpty()) {
            return ValidationInfo(
                TypeSpecBundle.message("action.createProject.templateRequired"),
                templateCombo,
            )
        }
        if (urlField.text.trim().isEmpty() && templateName !in TEMPLATES) {
            return ValidationInfo(
                TypeSpecBundle.message("action.createProject.templateUnknown"),
                templateCombo,
            )
        }
        if (resolvedProjectName().isEmpty()) {
            return ValidationInfo(
                TypeSpecBundle.message("action.createProject.projectNameRequired"),
                projectNameField,
            )
        }
        return null
    }

    fun targetPath(): Path? {
        val path = directoryField.text.trim()
        if (path.isEmpty()) {
            return null
        }
        return Paths.get(path)
    }

    fun buildInitArgs(): List<String> = buildTypeSpecInitArgs(
        template = currentTemplate(),
        templateUrl = urlField.text,
        projectName = resolvedProjectName(),
        emitters = chosenEmitters(),
    )

    private fun currentTemplate(): String {
        val editorComponent = templateCombo.editor?.editorComponent
        if (editorComponent is JTextField) {
            return editorComponent.text.trim()
        }
        return (templateCombo.selectedItem as? String)?.trim().orEmpty()
    }

    private fun resolvedProjectName(): String {
        val explicit = projectNameField.text.trim()
        if (explicit.isNotEmpty()) {
            return explicit
        }
        return defaultProjectName(directoryField.text)
    }

    private fun chosenEmitters(): List<String> {
        val url = urlField.text.trim()
        if (url.isEmpty() && currentTemplate() == REST_TEMPLATE) {
            return emitterChoices.filter { it.checkBox.isSelected }.map { it.option.packageName }
        }
        if (url.isNotEmpty()) {
            return parseTemplateEmitters(additionalEmittersField.text)
        }
        return emptyList()
    }

    private class EmitterChoice(val option: TypeSpecRestEmitterOption) {
        lateinit var checkBox: JBCheckBox
    }

    companion object {
        /**
         * Built-in `tsp init` template ids from `@typespec/compiler` 1.17.0 `templates/scaffolding.json`.
         * The REST scaffold key is `rest` (not `generic-rest-api`).
         */
        internal const val DEFAULT_TEMPLATE = "empty"
        internal const val REST_TEMPLATE = "rest"

        /**
         * Emitter marked `selected` in the 1.17.0 `rest` template. `tsp init -y` always includes it;
         * `--template-emitters` cannot turn it off.
         */
        internal const val REST_SELECTED_EMITTER = "@typespec/openapi3"

        internal val TEMPLATES = listOf(
            "empty",
            REST_TEMPLATE,
            "library-ts",
            "emitter-ts",
        )

        /**
         * Emitters from the 1.17.0 `rest` template that are not selected by default.
         * Checked boxes are passed as `--template-emitters`.
         */
        internal val REST_OPTIONAL_EMITTERS = listOf(
            TypeSpecRestEmitterOption(
                "@typespec/http-client-csharp",
                "action.createProject.emitter.httpClientCsharp",
            ),
            TypeSpecRestEmitterOption(
                "@typespec/http-client-java",
                "action.createProject.emitter.httpClientJava",
            ),
            TypeSpecRestEmitterOption(
                "@typespec/http-client-js",
                "action.createProject.emitter.httpClientJs",
            ),
            TypeSpecRestEmitterOption(
                "@typespec/http-client-python",
                "action.createProject.emitter.httpClientPython",
            ),
            TypeSpecRestEmitterOption(
                "@typespec/http-server-csharp",
                "action.createProject.emitter.httpServerCsharp",
            ),
            TypeSpecRestEmitterOption(
                "@typespec/http-server-js",
                "action.createProject.emitter.httpServerJs",
            ),
        )
    }
}

internal data class TypeSpecRestEmitterOption(
    val packageName: String,
    val messageKey: String,
)

/**
 * Folder name used as `--project-name` when the project name field is left blank.
 * Returns an empty string when [targetDirectory] has no usable file name.
 */
internal fun defaultProjectName(targetDirectory: String): String {
    val trimmed = targetDirectory.trim()
    if (trimmed.isEmpty()) {
        return ""
    }
    val fileName = Paths.get(trimmed).fileName?.toString()?.trim().orEmpty()
    if (fileName.isEmpty() || fileName == "." || fileName == "..") {
        return ""
    }
    return fileName
}

/** Splits a comma-separated emitter list. Blank entries are dropped. */
internal fun parseTemplateEmitters(value: String): List<String> =
    value.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

/**
 * Builds `tsp init` arguments that always run non-interactively.
 *
 * `@typespec/compiler` 1.17 throws when `-y` is set and `--template` is omitted, including when
 * [templateUrl] points at a template index (`dist/src/init/init.js`). The CLI runner never writes
 * to stdin, and without `-y` the compiler blocks on prompts.
 *
 * [emitters] are repeated `--template-emitters` options. Yargs treats a comma-separated value as
 * one name, so split with [parseTemplateEmitters] first. Emitters marked `selected` in the template
 * are included by the compiler under `-y`; this list only adds more.
 */
internal fun buildTypeSpecInitArgs(
    template: String,
    templateUrl: String = "",
    projectName: String,
    emitters: List<String> = emptyList(),
): List<String> {
    val templateName = template.trim()
    require(templateName.isNotEmpty()) {
        "template name is required when tsp init runs with --no-prompt"
    }
    val resolvedProjectName = projectName.trim()
    require(resolvedProjectName.isNotEmpty()) {
        "project name is required when tsp init runs with --no-prompt"
    }
    val url = templateUrl.trim().takeIf { it.isNotEmpty() }
    val emitterNames = emitters.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    return buildList {
        add("init")
        if (url != null) {
            add(url)
        }
        add("--template")
        add(templateName)
        add("-y")
        add("--project-name")
        add(resolvedProjectName)
        for (emitter in emitterNames) {
            add("--template-emitters")
            add(emitter)
        }
    }
}

private fun JComboBox<*>.editorTextSatisfies(predicate: (String) -> Boolean): ComponentPredicate {
    val comboBox = this
    return object : ComponentPredicate() {
        private val textField: JTextField
            get() = comboBox.editor.editorComponent as JTextField

        override fun invoke(): Boolean = predicate(textField.text)

        override fun addListener(listener: (Boolean) -> Unit) {
            textField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(event: DocumentEvent) {
                    listener(invoke())
                }
            })
        }
    }
}
