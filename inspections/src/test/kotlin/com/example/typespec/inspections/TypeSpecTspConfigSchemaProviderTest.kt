package com.example.typespec.inspections

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RunWith(JUnit4::class)
class TypeSpecTspConfigSchemaProviderTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = ""

    @Test
    fun providerMatchesTspConfigYamlByName() {
        val provider = TypeSpecTspConfigSchemaProviderFactory().getProviders(project).single()

        val tspConfig = myFixture.configureByText("tspconfig.yaml", "kind: project\n").virtualFile
        assertTrue(provider.isAvailable(tspConfig))

        val other = myFixture.configureByText("other.yaml", "kind: project\n").virtualFile
        assertFalse(provider.isAvailable(other))
    }

    @Test
    fun schemaResourceResolvesFromClasspath() {
        val resourceUrl = requireNotNull(javaClass.getResource("/schemas/tspconfig.schema.json"))
        allowResourceRootAccess(resourceUrl.toExternalForm())

        val provider = TypeSpecTspConfigSchemaProviderFactory().getProviders(project).single()
        val schemaFile = provider.getSchemaFile()
        assertNotNull(schemaFile)
        assertTrue(schemaFile!!.isValid)
    }

    @Test
    fun providerUsesDraft7SchemaVersion() {
        val provider = TypeSpecTspConfigSchemaProviderFactory().getProviders(project).single()
        assertEquals(JsonSchemaVersion.SCHEMA_7, provider.schemaVersion)
    }

    @Test
    fun embeddedSchemaAlignsWithTypeSpecCompilerSchema() {
        val schemaText = requireNotNull(
            javaClass.getResourceAsStream("/schemas/tspconfig.schema.json"),
        ) {
            "tspconfig.schema.json should be on the test classpath"
        }.bufferedReader().use { it.readText() }

        myFixture.configureByText("tspconfig.schema.json", schemaText)
        val root = (myFixture.file as JsonFile).allTopLevelValues.single() as JsonObject
        val properties = root.findProperty("properties")?.value as JsonObject

        assertEquals(
            "Schema should reject unknown top-level keys (match @typespec/compiler)",
            "false",
            root.findProperty("additionalProperties")?.value?.text,
        )
        assertNull("dry-run is not loaded by @typespec/compiler config loader", properties.findProperty("dry-run"))
        assertNotNull("parameters should be documented in schema", properties.findProperty("parameters"))
        assertNotNull(
            "environment-variables should be documented in schema",
            properties.findProperty("environment-variables"),
        )

        val parametersSchema = properties.findProperty("parameters")?.value as JsonObject
        val parameterValueSchema = parametersSchema.findProperty("additionalProperties")?.value as JsonObject
        val parameterRequired = parameterValueSchema.findProperty("required")?.value?.text.orEmpty()
        assertTrue(
            "parameter entries should require default (match @typespec/compiler)",
            parameterRequired.contains("default"),
        )
    }

    @Test
    fun embeddedSchemaIsValidJson() {
        val schemaText = requireNotNull(
            javaClass.getResourceAsStream("/schemas/tspconfig.schema.json"),
        ) {
            "tspconfig.schema.json should be on the test classpath"
        }.bufferedReader().use { it.readText() }

        myFixture.configureByText("tspconfig.schema.json", schemaText)
        val jsonFile = myFixture.file as JsonFile
        val root = jsonFile.allTopLevelValues.singleOrNull()
        assertNotNull("Schema should parse as a single JSON value", root)
        assertTrue("Schema root should be a JSON object", root is JsonObject)
    }

    private fun allowResourceRootAccess(resourcePath: String) {
        when {
            resourcePath.startsWith("jar:file:") -> {
                val jarPath = URLDecoder.decode(
                    resourcePath.removePrefix("jar:file:").substringBefore("!"),
                    StandardCharsets.UTF_8,
                )
                VfsRootAccess.allowRootAccess(testRootDisposable, jarPath)
            }
            resourcePath.startsWith("file:") -> {
                val filePath = URLDecoder.decode(
                    resourcePath.removePrefix("file:"),
                    StandardCharsets.UTF_8,
                )
                VfsRootAccess.allowRootAccess(testRootDisposable, filePath)
            }
        }
    }
}
