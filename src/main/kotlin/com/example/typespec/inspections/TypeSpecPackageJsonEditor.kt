package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

internal object TypeSpecPackageJsonEditor {
    fun applyFix(
        action: TypeSpecPackageJsonFixAction,
        project: Project,
        metadata: TypeSpecPackageMetadata,
    ) {
        runWrite(project) {
            when (action) {
                TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA ->
                    applyRecommendedMetadataChanges(project, metadata)
                TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES ->
                    moveCompilerToPeerDependenciesChanges(project, metadata)
            }
        }
    }

    private fun applyRecommendedMetadataChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val rules = metadata.rules
        if (rules.type != RECOMMENDED_TYPE_MODULE) {
            setTypeModuleChanges(project, metadata)
        }
        if (rules.main.isNullOrBlank()) {
            addMainEntrypointChanges(project, metadata)
        }
        if (rules.typespecExport.isNullOrBlank()) {
            addTypespecExportChanges(project, metadata)
        }
    }

    private fun moveCompilerToPeerDependenciesChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val rules = metadata.rules
        val compilerVersion = rules.dependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: rules.devDependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: return
        if (rules.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)) {
            return
        }

        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        if (psi.peerDependenciesProperty == null) {
            psi.rootObject.add(
                generator.createProperty(
                    "peerDependencies",
                    """{ ${jsonString(TYPESPEC_COMPILER_PACKAGE)}: ${jsonString(compilerVersion)} }""",
                ),
            )
        } else {
            val peerDependenciesObject = psi.peerDependenciesProperty.value as JsonObject
            peerDependenciesObject.add(
                generator.createProperty(TYPESPEC_COMPILER_PACKAGE, jsonString(compilerVersion)),
            )
        }

        psi.compilerDependencyProperty?.delete()
        psi.devCompilerDependencyProperty?.delete()
    }

    private fun setTypeModuleChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        val newProperty = generator.createProperty("type", jsonString(RECOMMENDED_TYPE_MODULE))
        if (psi.typeProperty != null) {
            psi.typeProperty.replace(newProperty)
        } else {
            psi.rootObject.add(newProperty)
        }
    }

    private fun addMainEntrypointChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val generator = JsonElementGenerator(project)
        metadata.psi.rootObject.add(generator.createProperty("main", jsonString(RECOMMENDED_MAIN)))
    }

    private fun addTypespecExportChanges(project: Project, metadata: TypeSpecPackageMetadata) {
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        psi.exportsDot.applyRecommendedTypespecExport(psi.rootObject, generator)
    }

    private fun runWrite(project: Project, action: () -> Unit) {
        WriteCommandAction.runWriteCommandAction(project, Runnable { action() })
    }
}

internal fun metadataFromElement(element: PsiElement): TypeSpecPackageMetadata? {
    val file = element.containingFile as? JsonFile ?: return null
    return TypeSpecPackageMetadata.fromJsonFile(file)
}
