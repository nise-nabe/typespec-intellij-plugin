package com.example.typespec.inspections

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElement

internal const val TYPESPEC_COMPILER_PACKAGE = "@typespec/compiler"
internal const val RECOMMENDED_MAIN = "dist/index.js"
internal const val RECOMMENDED_TYPESPEC_EXPORT = "./lib/main.tsp"
internal const val RECOMMENDED_TYPE_MODULE = "module"

enum class TypeSpecRecommendedLayoutStatus {
    PREFERRED,
    VALID_FALLBACK,
    MISSING,
}

enum class TypeSpecPackageJsonRule {
    TPKG001,
    TPKG002,
    TPKG003,
    TPKG004,
    TPKG005,
}

enum class TypeSpecFindingSeverity {
    WARNING,
    INFORMATION,
}

enum class TypeSpecPackageJsonFixAction {
    APPLY_RECOMMENDED_METADATA,
    MOVE_COMPILER_TO_PEER_DEPENDENCIES,
}

data class TypeSpecInspectionFinding(
    val rule: TypeSpecPackageJsonRule,
    val severity: TypeSpecFindingSeverity,
    val fixAction: TypeSpecPackageJsonFixAction,
    val anchor: PsiElement,
)

internal data class TypeSpecPackageJsonPsiAnchors(
    val rootObject: JsonObject,
    val typeProperty: JsonProperty?,
    val mainProperty: JsonProperty?,
    val tspMainProperty: JsonProperty?,
    val exportsProperty: JsonProperty?,
    val exportsDot: ExportsDotState,
    val dependenciesProperty: JsonProperty?,
    val devDependenciesProperty: JsonProperty?,
    val peerDependenciesProperty: JsonProperty?,
    val compilerDependencyProperty: JsonProperty?,
    val devCompilerDependencyProperty: JsonProperty?,
) {
    fun anchorForRule(rule: TypeSpecPackageJsonRule): PsiElement = when (rule) {
        TypeSpecPackageJsonRule.TPKG001 -> typeProperty ?: rootObject
        TypeSpecPackageJsonRule.TPKG002 -> mainProperty ?: rootObject
        TypeSpecPackageJsonRule.TPKG003 -> exportsProperty
            ?: exportsDot.dotProperty
            ?: exportsDot.typespecExportProperty
            ?: rootObject
        TypeSpecPackageJsonRule.TPKG004 -> compilerDependencyProperty
            ?: devCompilerDependencyProperty
            ?: dependenciesProperty
            ?: devDependenciesProperty
            ?: rootObject
        TypeSpecPackageJsonRule.TPKG005 -> exportsProperty ?: tspMainProperty ?: mainProperty ?: rootObject
    }
}

internal data class TypeSpecPackageMetadata(
    val rules: TypeSpecPackageRulesInput,
    val psi: TypeSpecPackageJsonPsiAnchors,
) {
    fun evaluateFindings(): List<TypeSpecInspectionFinding> =
        evaluateRules(rules).map { descriptor ->
            TypeSpecInspectionFinding(
                rule = descriptor.rule,
                severity = descriptor.severity,
                fixAction = descriptor.fixAction,
                anchor = psi.anchorForRule(descriptor.rule),
            )
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

            val exportsDot = ExportsDotState.fromExportsProperty(exportsProperty)

            return TypeSpecPackageMetadata(
                rules = buildRulesInput(
                    type = type,
                    main = main,
                    tspMain = tspMain,
                    typespecExport = exportsDot.typespecExport,
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
                    exportsDot = exportsDot,
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
