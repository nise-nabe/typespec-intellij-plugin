package com.example.typespec.inspections

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import java.io.File

class TypeSpecTspConfigSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> =
        mutableListOf(TypeSpecTspConfigSchemaProvider())
}

private class TypeSpecTspConfigSchemaProvider : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean = file.name == TSP_CONFIG_FILE_NAME

    override fun getName(): String = "TypeSpec tspconfig.yaml"

    override fun getSchemaFile(): VirtualFile? {
        val resource = javaClass.getResource(SCHEMA_RESOURCE_PATH) ?: return null
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(resource.toURI()))
    }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun isUserVisible(): Boolean = true

    companion object {
        private const val TSP_CONFIG_FILE_NAME = "tspconfig.yaml"
        private const val SCHEMA_RESOURCE_PATH = "/schemas/tspconfig.schema.json"
    }
}
