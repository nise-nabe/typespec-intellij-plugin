package com.example.typespec.inspections

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
                VfsRootAccess.allowRootAccess(testRootDisposable, resourcePath.removePrefix("file:"))
            }
        }
    }
}
