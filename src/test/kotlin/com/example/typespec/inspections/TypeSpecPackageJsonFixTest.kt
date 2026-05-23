package com.example.typespec.inspections

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeSpecPackageJsonFixTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = ""

    override fun isWriteActionRequired(): Boolean = true

    @Test
    fun testApplyRecommendedMetadataFromTspMainFallback() {
        val metadata = configurePackageJson(
            """
            {
              "tspMain": "./lib/main.tsp"
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA)

        val updated = metadata()
        assertEquals(RECOMMENDED_TYPE_MODULE, updated.input.type)
        assertEquals(RECOMMENDED_MAIN, updated.input.main)
        assertEquals(RECOMMENDED_TYPESPEC_EXPORT, updated.input.typespecExport)
        assertTrue(evaluateRules(updated.input).isEmpty())
    }

    @Test
    fun testApplyRecommendedMetadataAddsExportsWhenMissing() {
        val metadata = configurePackageJson(
            """
            {
              "peerDependencies": {
                "@typespec/http": "~1.0.0"
              }
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA)

        val updated = metadata()
        assertEquals(RECOMMENDED_TYPESPEC_EXPORT, updated.input.typespecExport)
        assertTrue(evaluateRules(updated.input).none { it == TypeSpecPackageJsonRule.TPKG003 })
    }

    @Test
    fun testApplyRecommendedMetadataUpgradesDotStringExport() {
        val metadata = configurePackageJson(
            """
            {
              "exports": {
                ".": "./dist/custom.js"
              },
              "tspMain": "./lib/main.tsp"
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA)

        val updated = metadata()
        assertEquals(RECOMMENDED_TYPESPEC_EXPORT, updated.input.typespecExport)
        assertEquals("./dist/custom.js", updated.psi.exportsLayout.let { layout ->
            (layout as ExportsLayout.DotObjectExport).defaultExport
        })
        assertTrue(evaluateRules(updated.input).isEmpty())
    }

    @Test
    fun testApplyRecommendedMetadataAddsTypespecToDotObjectExport() {
        val metadata = configurePackageJson(
            """
            {
              "exports": {
                ".": {
                  "default": "./dist/custom.js"
                }
              },
              "tspMain": "./lib/main.tsp"
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA)

        val updated = metadata()
        assertEquals(RECOMMENDED_TYPESPEC_EXPORT, updated.input.typespecExport)
        assertEquals("./dist/custom.js", (updated.psi.exportsLayout as ExportsLayout.DotObjectExport).defaultExport)
    }

    @Test
    fun testApplyRecommendedMetadataAddsMissingDotEntry() {
        val metadata = configurePackageJson(
            """
            {
              "exports": {
                "./other": "./other.js"
              },
              "tspMain": "./lib/main.tsp"
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA)

        val updated = metadata()
        assertEquals(RECOMMENDED_TYPESPEC_EXPORT, updated.input.typespecExport)
    }

    @Test
    fun testApplyRecommendedMetadataReplacesInvalidExports() {
        val metadata = configurePackageJson(
            """
            {
              "exports": "./invalid",
              "tspMain": "./lib/main.tsp"
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA)

        val updated = metadata()
        assertEquals(RECOMMENDED_TYPESPEC_EXPORT, updated.input.typespecExport)
        assertTrue(updated.psi.exportsLayout is ExportsLayout.DotObjectExport)
    }

    @Test
    fun testMoveCompilerToPeerDependenciesFromDependencies() {
        val metadata = configurePackageJson(
            """
            {
              "tspMain": "./lib/main.tsp",
              "dependencies": {
                "@typespec/compiler": "~1.0.0"
              }
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES)

        val updated = metadata()
        assertEquals("~1.0.0", updated.input.peerDependencies[TYPESPEC_COMPILER_PACKAGE])
        assertFalse(updated.input.dependencies.containsKey(TYPESPEC_COMPILER_PACKAGE))
        assertFalse(evaluateRules(updated.input).any { it == TypeSpecPackageJsonRule.TPKG004 })
    }

    @Test
    fun testMoveCompilerToPeerDependenciesFromDevDependenciesIntoExistingPeerBlock() {
        val metadata = configurePackageJson(
            """
            {
              "tspMain": "./lib/main.tsp",
              "devDependencies": {
                "@typespec/compiler": "^2.0.0"
              },
              "peerDependencies": {
                "@typespec/http": "~1.0.0"
              }
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES)

        val updated = metadata()
        assertEquals("^2.0.0", updated.input.peerDependencies[TYPESPEC_COMPILER_PACKAGE])
        assertEquals("~1.0.0", updated.input.peerDependencies["@typespec/http"])
        assertFalse(updated.input.devDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE))
    }

    @Test
    fun testMoveCompilerToPeerDependenciesReplacesInvalidPeerDependencies() {
        val metadata = configurePackageJson(
            """
            {
              "tspMain": "./lib/main.tsp",
              "devDependencies": {
                "@typespec/compiler": "~1.0.0"
              },
              "peerDependencies": "invalid"
            }
            """.trimIndent(),
        )

        applyFix(metadata, TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES)

        val updated = metadata()
        assertEquals("~1.0.0", updated.input.peerDependencies[TYPESPEC_COMPILER_PACKAGE])
        assertFalse(updated.input.devDependencies.containsKey(TYPESPEC_COMPILER_PACKAGE))
    }

    private fun applyFix(metadata: TypeSpecPackageMetadata, fixAction: TypeSpecPackageJsonFixAction) {
        applyViolatedRules(
            metadata = metadata,
            fixAction = fixAction,
            generator = JsonElementGenerator(project),
        )
    }

    private fun configurePackageJson(json: String): TypeSpecPackageMetadata {
        myFixture.configureByText("package.json", json)
        return metadata()
    }

    private fun metadata(): TypeSpecPackageMetadata {
        val file = myFixture.file as JsonFile
        return TypeSpecPackageMetadata.fromJsonFile(file)
            ?: error("Expected package.json metadata")
    }
}
