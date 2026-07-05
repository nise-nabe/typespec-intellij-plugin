package com.example.typespec.inspections

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class TypeSpecTspConfigSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> =
        mutableListOf(TypeSpecTspConfigSchemaProvider())
}

private class TypeSpecTspConfigSchemaProvider : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean = file.name == "tspconfig.yaml"

    override fun getName(): String = "TypeSpec tspconfig.yaml"

    override fun getSchemaFile(): VirtualFile? =
        LocalFileSystem.getInstance().findFileByPath(
            javaClass.getResource("/schemas/tspconfig.schema.json")?.toString()?.removePrefix("file:").orEmpty(),
        ) ?: run {
            val url = javaClass.getResource("/schemas/tspconfig.schema.json") ?: return null
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(java.io.File(url.toURI()))
        }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun isUserVisible(): Boolean = true
}
