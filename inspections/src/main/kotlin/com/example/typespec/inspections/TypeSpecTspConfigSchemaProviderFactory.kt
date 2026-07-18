package com.example.typespec.inspections

import com.example.typespec.TSP_CONFIG_FILE_NAME
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class TypeSpecTspConfigSchemaProviderFactory : JsonSchemaProviderFactory, DumbAware {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> =
        mutableListOf(TypeSpecTspConfigSchemaProvider())
}

private class TypeSpecTspConfigSchemaProvider : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean = file.name == TSP_CONFIG_FILE_NAME

    override fun getName(): String = "TypeSpec tspconfig.yaml"

    override fun getSchemaFile(): VirtualFile? =
        JsonSchemaProviderFactory.getResourceFile(javaClass, SCHEMA_RESOURCE_PATH)

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun isUserVisible(): Boolean = true

    companion object {
        private const val SCHEMA_RESOURCE_PATH = "/schemas/tspconfig.schema.json"
    }
}
