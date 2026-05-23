package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

internal object TypeSpecPackageJsonEditor {
    fun applyRecommendedMetadata(project: Project, metadata: TypeSpecPackageMetadata) {
        val rules = metadata.rules
        val psi = metadata.psi
        val generator = JsonElementGenerator(project)

        if (rules.type != RECOMMENDED_TYPE_MODULE) {
            upsertStringProperty(
                container = psi.rootObject,
                existing = psi.typeProperty,
                name = "type",
                value = RECOMMENDED_TYPE_MODULE,
                generator = generator,
            )
        }
        if (rules.main.isNullOrBlank()) {
            upsertStringProperty(
                container = psi.rootObject,
                existing = psi.mainProperty,
                name = "main",
                value = RECOMMENDED_MAIN,
                generator = generator,
            )
        }
        if (rules.typespecExport.isNullOrBlank()) {
            psi.exportsDot.applyRecommendedTypespecExport(psi.rootObject, generator)
        }
    }

    fun moveCompilerToPeerDependencies(project: Project, metadata: TypeSpecPackageMetadata) {
        val rules = metadata.rules
        val compilerVersion = rules.dependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: rules.devDependencies[TYPESPEC_COMPILER_PACKAGE]
            ?: return
        if (rules.peerDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE)) {
            return
        }

        val psi = metadata.psi
        val generator = JsonElementGenerator(project)
        val peerDependenciesSnippet =
            """{ ${jsonString(TYPESPEC_COMPILER_PACKAGE)}: ${jsonString(compilerVersion)} }"""

        when (val peerDependenciesProperty = psi.peerDependenciesProperty) {
            null -> psi.rootObject.add(
                generator.createProperty("peerDependencies", peerDependenciesSnippet),
            )
            else -> {
                val peerDependenciesObject = peerDependenciesProperty.value as? JsonObject
                if (peerDependenciesObject == null) {
                    peerDependenciesProperty.replace(
                        generator.createProperty("peerDependencies", peerDependenciesSnippet),
                    )
                } else {
                    upsertStringProperty(
                        container = peerDependenciesObject,
                        existing = peerDependenciesObject.findProperty(TYPESPEC_COMPILER_PACKAGE),
                        name = TYPESPEC_COMPILER_PACKAGE,
                        value = compilerVersion,
                        generator = generator,
                    )
                }
            }
        }

        psi.compilerDependencyProperty?.delete()
        psi.devCompilerDependencyProperty?.delete()
    }
}

internal fun metadataFromElement(element: PsiElement): TypeSpecPackageMetadata? {
    val file = element.containingFile as? JsonFile ?: return null
    return TypeSpecPackageMetadata.fromJsonFile(file)
}
