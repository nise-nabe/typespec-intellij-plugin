package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject

internal fun moveCompilerToPeerDependencies(
    metadata: TypeSpecPackageMetadata,
    generator: JsonElementGenerator,
) {
    val input = metadata.input
    val compilerVersion = input.dependencies[TYPESPEC_COMPILER_PACKAGE]
        ?: input.devDependencies[TYPESPEC_COMPILER_PACKAGE]
        ?: return

    val psi = metadata.psi
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
