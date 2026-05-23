package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElement

internal sealed class DotKind {
    data object Missing : DotKind()

    data class StringDefault(
        val defaultExport: String,
    ) : DotKind()

    data class ObjectDot(
        val dotObject: JsonObject,
        val typespecExportProperty: JsonProperty?,
        val typespecExport: String?,
        val defaultExport: String?,
    ) : DotKind()

    data class Invalid(
        val dotProperty: JsonProperty,
    ) : DotKind()
}

internal sealed class ExportsDotState {
    abstract val typespecExport: String?

    fun missingLayoutInspectionAnchor(fallback: PsiElement): PsiElement = when (this) {
        is Missing -> fallback
        is InvalidExports -> exportsProperty
        is Ready -> when (dotKind) {
            DotKind.Missing -> exportsProperty
            is DotKind.StringDefault -> dotPropertyElement ?: exportsProperty
            is DotKind.ObjectDot -> dotKind.typespecExportProperty
                ?: dotPropertyElement
                ?: exportsProperty
            is DotKind.Invalid -> dotPropertyElement ?: exportsProperty
        }
    }

    fun fallbackLayoutInspectionAnchor(
        tspMainProperty: JsonProperty?,
        mainProperty: JsonProperty?,
        fallback: PsiElement,
    ): PsiElement = when (this) {
        is Missing -> tspMainProperty ?: mainProperty ?: fallback
        is InvalidExports -> exportsProperty
        is Ready -> exportsProperty
    }

    data object Missing : ExportsDotState() {
        override val typespecExport: String? = null
    }

    data class InvalidExports(
        val exportsProperty: JsonProperty,
    ) : ExportsDotState() {
        override val typespecExport: String? = null
    }

    data class Ready(
        val exportsProperty: JsonProperty,
        val exportsObject: JsonObject,
        val dotPropertyElement: JsonProperty?,
        val dotKind: DotKind,
    ) : ExportsDotState() {
        override val typespecExport: String?
            get() = (dotKind as? DotKind.ObjectDot)?.typespecExport
    }

    fun applyRecommendedTypespecExport(rootObject: JsonObject, generator: JsonElementGenerator) {
        when (this) {
            is Missing -> rootObject.add(
                generator.createProperty("exports", recommendedExportsSnippet()),
            )
            is InvalidExports -> exportsProperty.replace(
                generator.createProperty("exports", recommendedExportsSnippet()),
            )
            is Ready -> applyRecommendedTypespecExportToReady(generator)
        }
    }

    private fun Ready.applyRecommendedTypespecExportToReady(generator: JsonElementGenerator) {
        when (dotKind) {
            DotKind.Missing -> exportsObject.add(
                generator.createProperty(".", recommendedDotObjectSnippet()),
            )
            is DotKind.StringDefault -> checkNotNull(dotPropertyElement).replace(
                generator.createProperty(".", dotObjectWithDefaultAndTypespec(dotKind.defaultExport)),
            )
            is DotKind.ObjectDot -> applyTypespecToObjectDot(generator, dotKind)
            is DotKind.Invalid -> dotKind.dotProperty.replace(
                generator.createProperty(".", recommendedDotObjectSnippet()),
            )
        }
    }

    private fun Ready.applyTypespecToObjectDot(
        generator: JsonElementGenerator,
        objectDot: DotKind.ObjectDot,
    ) {
        val typespecProperty = generator.createProperty(
            "typespec",
            jsonString(RECOMMENDED_TYPESPEC_EXPORT),
        )
        when {
            objectDot.typespecExportProperty != null ->
                objectDot.typespecExportProperty.replace(typespecProperty)
            else ->
                objectDot.dotObject.add(typespecProperty)
        }
    }

    companion object {
        fun fromExportsProperty(exportsProperty: JsonProperty?): ExportsDotState {
            if (exportsProperty == null) {
                return Missing
            }

            val exportsObject = exportsProperty.value as? JsonObject
                ?: return InvalidExports(exportsProperty)

            val dotPropertyElement = exportsObject.findProperty(".")
            if (dotPropertyElement == null) {
                return Ready(
                    exportsProperty = exportsProperty,
                    exportsObject = exportsObject,
                    dotPropertyElement = null,
                    dotKind = DotKind.Missing,
                )
            }

            val dotKind = when (val dotValue = dotPropertyElement.value) {
                is JsonStringLiteral -> DotKind.StringDefault(dotValue.value)
                is JsonObject -> {
                    val typespecExportProperty = dotValue.findProperty("typespec")
                    DotKind.ObjectDot(
                        dotObject = dotValue,
                        typespecExportProperty = typespecExportProperty,
                        typespecExport = readStringProperty(typespecExportProperty),
                        defaultExport = readStringProperty(dotValue.findProperty("default")),
                    )
                }
                else -> DotKind.Invalid(dotPropertyElement)
            }

            return Ready(
                exportsProperty = exportsProperty,
                exportsObject = exportsObject,
                dotPropertyElement = dotPropertyElement,
                dotKind = dotKind,
            )
        }
    }
}
