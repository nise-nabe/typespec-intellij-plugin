package com.example.typespec.inspections

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElement

internal data class TypeSpecPackageMetadata(
    val input: TypeSpecPackageRulesInput,
    val psi: TypeSpecPackageJsonPsiAnchors,
) {
    fun evaluateFindings(): List<TypeSpecInspectionFinding> =
        evaluateRules(input).map { rule ->
            TypeSpecInspectionFinding(
                rule = rule,
                anchor = rule.anchor(psi, input),
            )
        }

    fun refresh(): TypeSpecPackageMetadata? {
        val file = psi.rootObject.containingFile as? JsonFile ?: return null
        return fromJsonFile(file)
    }

    companion object {
        fun fromJsonFile(file: JsonFile): TypeSpecPackageMetadata? {
            if (file.name != "package.json") {
                return null
            }

            val rootObject = file.topLevelValue as? JsonObject ?: return null
            val typeProperty = rootObject.findProperty("type")
            val mainProperty = rootObject.findProperty("main")
            val tspMainProperty = rootObject.findProperty("tspMain")
            val exportsProperty = rootObject.findProperty("exports")
            val dependenciesProperty = rootObject.findProperty("dependencies")
            val peerDependenciesProperty = rootObject.findProperty("peerDependencies")
            val devDependenciesProperty = rootObject.findProperty("devDependencies")

            val type = readStringProperty(typeProperty)
            val main = readStringProperty(mainProperty)
            val tspMain = readStringProperty(tspMainProperty)
            val dependencies = readDependencyMap(dependenciesProperty)
            val devDependencies = readDependencyMap(devDependenciesProperty)
            val peerDependencies = readDependencyMap(peerDependenciesProperty)

            val exportsLayout = ExportsLayout.fromExportsProperty(exportsProperty)

            return TypeSpecPackageMetadata(
                input = buildRulesInput(
                    type = type,
                    main = main,
                    tspMain = tspMain,
                    typespecExport = exportsLayout.typespecExport,
                    dependencies = dependencies,
                    devDependencies = devDependencies,
                    peerDependencies = peerDependencies,
                ),
                psi = TypeSpecPackageJsonPsiAnchors(
                    rootObject = rootObject,
                    typeProperty = typeProperty,
                    mainProperty = mainProperty,
                    tspMainProperty = tspMainProperty,
                    exportsProperty = exportsProperty,
                    exportsLayout = exportsLayout,
                    dependenciesProperty = dependenciesProperty,
                    devDependenciesProperty = devDependenciesProperty,
                    peerDependenciesProperty = peerDependenciesProperty,
                    compilerDependencyProperty = findDependencyProperty(dependenciesProperty, TYPESPEC_COMPILER_PACKAGE),
                    devCompilerDependencyProperty = findDependencyProperty(devDependenciesProperty, TYPESPEC_COMPILER_PACKAGE),
                ),
            )
        }

        private fun readDependencyMap(property: JsonProperty?): Map<String, String> {
            val objectValue = property?.value as? JsonObject ?: return emptyMap()
            return objectValue.propertyList.mapNotNull { dependencyProperty ->
                val version = readStringProperty(dependencyProperty) ?: return@mapNotNull null
                dependencyProperty.name to version
            }.toMap()
        }

        private fun findDependencyProperty(
            dependenciesProperty: JsonProperty?,
            packageName: String,
        ): JsonProperty? {
            val objectValue = dependenciesProperty?.value as? JsonObject ?: return null
            return objectValue.findProperty(packageName)
        }
    }
}

internal fun metadataFromElement(element: PsiElement): TypeSpecPackageMetadata? {
    val file = element.containingFile as? JsonFile ?: return null
    return TypeSpecPackageMetadata.fromJsonFile(file)
}
