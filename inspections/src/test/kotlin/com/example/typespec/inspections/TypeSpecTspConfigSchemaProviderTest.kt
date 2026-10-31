package com.example.typespec.inspections

import com.example.typespec.TypeSpecBasePlatformTestCase
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
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
class TypeSpecTspConfigSchemaProviderTest : TypeSpecBasePlatformTestCase() {
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
    fun embeddedSchemaSuggestsKnownFeaturesAndAllowsCompilerNulls() {
        val root = loadSchemaRoot()
        val comment = stringValue(root, "\$comment")
        assertTrue(comment.contains("1.17.0"))
        assertTrue(comment.contains("features"))
        assertFalse(comment.contains("Unchanged from 1.13.0"))

        val properties = root.findProperty("properties")?.value as JsonObject
        val features = objectValue(properties, "features")
        assertTrue(acceptsNull(features))
        val featureBranches = arrayValue(objectValue(features, "items"), "anyOf")
        val knownFeatures = featureBranches.valueList[0] as JsonObject
        assertEquals(
            listOf(
                "function-declarations",
                "declaration-expressions",
                "auto-decorators",
                "type-info-provider",
                "union-extends",
            ),
            stringValues(arrayValue(knownFeatures, "enum")),
        )
        assertEquals("true", knownFeatures.findProperty("x-intellij-enum-order-sensitive")?.value?.text)
        val metadata = objectValue(knownFeatures, "x-intellij-enum-metadata")
        assertEquals(
            "Allows use of function declarations without experimental warnings in project code.",
            stringValue(objectValue(metadata, "function-declarations"), "description"),
        )
        assertEquals(
            "Allows use of declaration expressions (named or anonymous model, scalar, enum and union declarations in expression position) in project code.",
            stringValue(objectValue(metadata, "declaration-expressions"), "description"),
        )
        assertEquals(
            "Allows use of auto decorator declarations without experimental warnings in project code.",
            stringValue(objectValue(metadata, "auto-decorators"), "description"),
        )
        assertEquals(
            "Enables the experimental `\$provideTypeInfo` provider allowing libraries to contribute extra information about types to IDE hover and tooling (queried via `program.getTypeInfo`).",
            stringValue(objectValue(metadata, "type-info-provider"), "description"),
        )
        assertEquals(
            "Enables experimental union `extends` clauses in project code.",
            stringValue(objectValue(metadata, "union-extends"), "description"),
        )
        val freeFormFeature = featureBranches.valueList[1] as JsonObject
        assertEquals("string", stringValue(freeFormFeature, "type"))
        assertNull(freeFormFeature.findProperty("enum"))

        for (name in listOf(
            "extends",
            "entrypoint",
            "parameters",
            "environment-variables",
            "output-dir",
            "warn-as-error",
            "imports",
            "emit",
            "options",
            "linter",
        )) {
            assertTrue("$name should accept null", acceptsNull(objectValue(properties, name)))
        }
        assertTrue(acceptsNull(objectValue(properties, "kind")))
        assertTrue(
            stringValues(arrayValue(kindEnumBranch(objectValue(properties, "kind")), "enum")).contains("project"),
        )

        val linterProperties = objectValue(objectValue(properties, "linter"), "properties")
        for (name in listOf("extends", "enable", "disable")) {
            assertTrue("linter.$name should accept null", acceptsNull(objectValue(linterProperties, name)))
        }
        val emitterOutputDir = objectValue(
            objectValue(
                objectValue(objectValue(properties, "options"), "additionalProperties"),
                "properties",
            ),
            "emitter-output-dir",
        )
        assertTrue(acceptsNull(emitterOutputDir))

        val trace = objectValue(properties, "trace")
        assertFalse(acceptsNull(trace))
        for (option in arrayValue(trace, "oneOf").valueList.filterIsInstance<JsonObject>()) {
            assertFalse(acceptsNull(option))
        }
        val parameterDefault = objectValue(
            objectValue(
                objectValue(objectValue(properties, "parameters"), "additionalProperties"),
                "properties",
            ),
            "default",
        )
        assertFalse(acceptsNull(parameterDefault))
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

    private fun loadSchemaRoot(): JsonObject {
        val schemaText = requireNotNull(
            javaClass.getResourceAsStream("/schemas/tspconfig.schema.json"),
        ) {
            "tspconfig.schema.json should be on the test classpath"
        }.bufferedReader().use { it.readText() }
        myFixture.configureByText("tspconfig.schema.json", schemaText)
        return (myFixture.file as JsonFile).allTopLevelValues.single() as JsonObject
    }

    private fun objectValue(parent: JsonObject, name: String): JsonObject =
        parent.findProperty(name)?.value as JsonObject

    private fun arrayValue(parent: JsonObject, name: String): JsonArray =
        parent.findProperty(name)?.value as JsonArray

    private fun stringValue(parent: JsonObject, name: String): String =
        (parent.findProperty(name)?.value as JsonStringLiteral).value

    private fun stringValues(array: JsonArray): List<String> =
        array.valueList.filterIsInstance<JsonStringLiteral>().map { it.value }

    private fun acceptsNull(schema: JsonObject): Boolean {
        if (typeIncludesNull(schema)) {
            return true
        }
        val anyOf = schema.findProperty("anyOf")?.value as? JsonArray ?: return false
        return anyOf.valueList.filterIsInstance<JsonObject>().any { typeIncludesNull(it) }
    }

    private fun typeIncludesNull(schema: JsonObject): Boolean {
        val typeValue = schema.findProperty("type")?.value ?: return false
        return when (typeValue) {
            is JsonStringLiteral -> typeValue.value == "null"
            is JsonArray -> typeValue.valueList.filterIsInstance<JsonStringLiteral>().any { it.value == "null" }
            else -> false
        }
    }

    private fun kindEnumBranch(kindSchema: JsonObject): JsonObject =
        (kindSchema.findProperty("anyOf")?.value as JsonArray).valueList
            .filterIsInstance<JsonObject>()
            .single { it.findProperty("enum") != null }

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
