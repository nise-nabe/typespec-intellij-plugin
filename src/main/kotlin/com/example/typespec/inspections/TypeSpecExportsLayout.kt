package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElement

internal sealed class ExportsLayout {
    abstract val typespecExport: String?

    fun missingLayoutInspectionAnchor(fallback: PsiElement): PsiElement = when (this) {
        is Absent -> fallback
        is InvalidExports -> exportsProperty
        is MissingDotEntry -> exportsProperty
        is DotStringExport -> dotProperty
        is DotObjectExport -> typespecExportProperty ?: dotProperty
        is InvalidDotEntry -> dotProperty
    }

    fun fallbackLayoutInspectionAnchor(
        tspMainProperty: JsonProperty?,
        mainProperty: JsonProperty?,
        fallback: PsiElement,
    ): PsiElement = when (this) {
        is Absent -> tspMainProperty ?: mainProperty ?: fallback
        is InvalidExports -> exportsProperty
        is MissingDotEntry -> exportsProperty
        is DotStringExport -> exportsProperty
        is DotObjectExport -> exportsProperty
        is InvalidDotEntry -> exportsProperty
    }

    fun applyRecommendedTypespecExport(rootObject: JsonObject, generator: JsonElementGenerator) {
        when (this) {
            is Absent -> rootObject.add(
                generator.createProperty("exports", recommendedExportsSnippet()),
            )
            is InvalidExports -> exportsProperty.replace(
                generator.createProperty("exports", recommendedExportsSnippet()),
            )
            is MissingDotEntry -> exportsObject.add(
                generator.createProperty(".", recommendedDotObjectSnippet()),
            )
            is DotStringExport -> dotProperty.replace(
                generator.createProperty(".", dotObjectWithDefaultAndTypespec(defaultExport)),
            )
            is DotObjectExport -> applyRecommendedTypespecExportToDotObject(generator)
            is InvalidDotEntry -> dotProperty.replace(
                generator.createProperty(".", recommendedDotObjectSnippet()),
            )
        }
    }

    data object Absent : ExportsLayout() {
        override val typespecExport: String? = null
    }

    data class InvalidExports(
        val exportsProperty: JsonProperty,
    ) : ExportsLayout() {
        override val typespecExport: String? = null
    }

    data class MissingDotEntry(
        val exportsProperty: JsonProperty,
        val exportsObject: JsonObject,
    ) : ExportsLayout() {
        override val typespecExport: String? = null
    }

    data class DotStringExport(
        val exportsProperty: JsonProperty,
        val exportsObject: JsonObject,
        val dotProperty: JsonProperty,
        val defaultExport: String,
    ) : ExportsLayout() {
        override val typespecExport: String? = null
    }

    data class DotObjectExport(
        val exportsProperty: JsonProperty,
        val exportsObject: JsonObject,
        val dotProperty: JsonProperty,
        val dotObject: JsonObject,
        val typespecExportProperty: JsonProperty?,
        override val typespecExport: String?,
        val defaultExport: String?,
    ) : ExportsLayout()

    data class InvalidDotEntry(
        val exportsProperty: JsonProperty,
        val exportsObject: JsonObject,
        val dotProperty: JsonProperty,
    ) : ExportsLayout() {
        override val typespecExport: String? = null
    }

    private fun DotObjectExport.applyRecommendedTypespecExportToDotObject(generator: JsonElementGenerator) {
        val typespecProperty = generator.createProperty(
            "typespec",
            jsonString(RECOMMENDED_TYPESPEC_EXPORT),
        )
        when {
            typespecExportProperty != null ->
                typespecExportProperty.replace(typespecProperty)
            else ->
                dotObject.add(typespecProperty)
        }
    }

    companion object {
        fun fromExportsProperty(exportsProperty: JsonProperty?): ExportsLayout {
            if (exportsProperty == null) {
                return Absent
            }

            val exportsObject = exportsProperty.value as? JsonObject
                ?: return InvalidExports(exportsProperty)

            val dotProperty = exportsObject.findProperty(".")
                ?: return MissingDotEntry(
                    exportsProperty = exportsProperty,
                    exportsObject = exportsObject,
                )

            return when (val dotValue = dotProperty.value) {
                is JsonStringLiteral -> DotStringExport(
                    exportsProperty = exportsProperty,
                    exportsObject = exportsObject,
                    dotProperty = dotProperty,
                    defaultExport = dotValue.value,
                )
                is JsonObject -> {
                    val typespecExportProperty = dotValue.findProperty("typespec")
                    DotObjectExport(
                        exportsProperty = exportsProperty,
                        exportsObject = exportsObject,
                        dotProperty = dotProperty,
                        dotObject = dotValue,
                        typespecExportProperty = typespecExportProperty,
                        typespecExport = readStringProperty(typespecExportProperty),
                        defaultExport = readStringProperty(dotValue.findProperty("default")),
                    )
                }
                else -> InvalidDotEntry(
                    exportsProperty = exportsProperty,
                    exportsObject = exportsObject,
                    dotProperty = dotProperty,
                )
            }
        }
    }
}
