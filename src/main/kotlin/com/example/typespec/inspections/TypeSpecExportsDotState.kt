package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral

internal sealed class ExportsDotState {
    abstract val exportsProperty: JsonProperty?
    abstract val dotProperty: JsonProperty?
    abstract val typespecExportProperty: JsonProperty?
    abstract val typespecExport: String?
    abstract val defaultExport: String?

    data class ExportsMissing(
        override val exportsProperty: JsonProperty? = null,
    ) : ExportsDotState() {
        override val dotProperty: JsonProperty? = null
        override val typespecExportProperty: JsonProperty? = null
        override val typespecExport: String? = null
        override val defaultExport: String? = null
    }

    data class ExportsNotObject(
        override val exportsProperty: JsonProperty,
    ) : ExportsDotState() {
        override val dotProperty: JsonProperty? = null
        override val typespecExportProperty: JsonProperty? = null
        override val typespecExport: String? = null
        override val defaultExport: String? = null
    }

    data class DotMissing(
        override val exportsProperty: JsonProperty,
        val exportsObject: JsonObject,
    ) : ExportsDotState() {
        override val dotProperty: JsonProperty? = null
        override val typespecExportProperty: JsonProperty? = null
        override val typespecExport: String? = null
        override val defaultExport: String? = null
    }

    data class StringDefault(
        override val exportsProperty: JsonProperty,
        override val dotProperty: JsonProperty,
        override val defaultExport: String,
    ) : ExportsDotState() {
        override val typespecExportProperty: JsonProperty? = null
        override val typespecExport: String? = null
    }

    data class ObjectDot(
        override val exportsProperty: JsonProperty,
        override val dotProperty: JsonProperty,
        override val typespecExportProperty: JsonProperty?,
        override val typespecExport: String?,
        override val defaultExport: String?,
        val dotObject: JsonObject?,
    ) : ExportsDotState()

    fun applyRecommendedTypespecExport(rootObject: JsonObject, generator: JsonElementGenerator) {
        when (this) {
            is ExportsMissing -> rootObject.add(
                generator.createProperty("exports", recommendedExportsSnippet()),
            )
            is ExportsNotObject -> exportsProperty.replace(
                generator.createProperty("exports", recommendedExportsSnippet()),
            )
            is DotMissing -> exportsObject.add(
                generator.createProperty(".", recommendedDotObjectSnippet()),
            )
            is StringDefault -> dotProperty.replace(
                generator.createProperty(".", dotObjectWithDefaultAndTypespec(defaultExport)),
            )
            is ObjectDot -> applyTypespecToObjectDot(generator)
        }
    }

    private fun ObjectDot.applyTypespecToObjectDot(generator: JsonElementGenerator) {
        val typespecProperty = generator.createProperty(
            "typespec",
            jsonString(RECOMMENDED_TYPESPEC_EXPORT),
        )
        when {
            dotObject != null && typespecExportProperty == null ->
                dotObject.add(typespecProperty)
            typespecExportProperty != null ->
                typespecExportProperty.replace(typespecProperty)
            else ->
                dotProperty.replace(
                    generator.createProperty(".", recommendedDotObjectSnippet()),
                )
        }
    }

    companion object {
        fun fromExportsProperty(exportsProperty: JsonProperty?): ExportsDotState {
            if (exportsProperty == null) {
                return ExportsMissing()
            }

            val exportsObject = exportsProperty.value as? JsonObject
                ?: return ExportsNotObject(exportsProperty)

            val dotProperty = exportsObject.findProperty(".")
                ?: return DotMissing(exportsProperty, exportsObject)

            return when (val dotValue = dotProperty.value) {
                is JsonStringLiteral -> StringDefault(
                    exportsProperty = exportsProperty,
                    dotProperty = dotProperty,
                    defaultExport = dotValue.value,
                )
                is JsonObject -> ObjectDot(
                    exportsProperty = exportsProperty,
                    dotProperty = dotProperty,
                    typespecExportProperty = dotValue.findProperty("typespec"),
                    typespecExport = readStringProperty(dotValue.findProperty("typespec")),
                    defaultExport = readStringProperty(dotValue.findProperty("default")),
                    dotObject = dotValue,
                )
                else -> ObjectDot(
                    exportsProperty = exportsProperty,
                    dotProperty = dotProperty,
                    typespecExportProperty = null,
                    typespecExport = null,
                    defaultExport = null,
                    dotObject = null,
                )
            }
        }
    }
}
