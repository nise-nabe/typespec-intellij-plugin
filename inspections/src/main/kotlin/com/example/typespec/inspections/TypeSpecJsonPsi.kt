package com.example.typespec.inspections

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral

internal fun readStringProperty(property: JsonProperty?): String? {
    val value = property?.value as? JsonStringLiteral ?: return null
    return value.value
}

internal fun jsonString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

internal fun upsertStringProperty(
    container: JsonObject,
    existing: JsonProperty?,
    name: String,
    value: String,
    generator: JsonElementGenerator,
) {
    val newProperty = generator.createProperty(name, jsonString(value))
    if (existing != null) {
        existing.replace(newProperty)
    } else {
        container.add(newProperty)
    }
}

internal fun recommendedExportsSnippet(): String =
    """{ ".": { "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} } }"""

internal fun recommendedDotObjectSnippet(): String =
    """{ "typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} }"""

internal fun dotObjectWithDefaultAndTypespec(defaultExport: String): String = buildString {
    append("{ ")
    append(""""default": ${jsonString(defaultExport)}, """)
    append(""""typespec": ${jsonString(RECOMMENDED_TYPESPEC_EXPORT)} }""")
}
