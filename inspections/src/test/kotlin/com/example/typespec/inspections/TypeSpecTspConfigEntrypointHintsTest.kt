package com.example.typespec.inspections

import com.example.typespec.TypeSpecBasePlatformTestCase
import com.example.typespec.TypeSpecBundle
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeSpecTspConfigEntrypointHintsTest : TypeSpecBasePlatformTestCase() {
    override fun getTestDataPath(): String = ""

    @Test
    fun testReadsTypespecExportTspMainAndMainTsp() {
        myFixture.addFileToProject("proj/main.tsp", "namespace Demo {}\n")
        myFixture.addFileToProject(
            "proj/package.json",
            """
            {
              "tspMain": "lib/main.tsp",
              "exports": {
                ".": {
                  "typespec": "./lib/main.tsp"
                }
              }
            }
            """.trimIndent(),
        )
        val config = myFixture.addFileToProject("proj/tspconfig.yaml", "kind: project\n")

        val hints = entrypointHintsForConfigFile(config)

        assertEquals("./lib/main.tsp", hints.typespecExport)
        assertEquals("lib/main.tsp", hints.tspMain)
        assertTrue(hints.mainTspExists)
        assertTrue(hints.resolvesEntrypoint)
    }

    @Test
    fun testMissingSiblingsDoNotResolveAnEntrypoint() {
        val config = myFixture.addFileToProject("lonely/tspconfig.yaml", "kind: project\n")

        val hints = entrypointHintsForConfigFile(config)

        assertNull(hints.typespecExport)
        assertNull(hints.tspMain)
        assertFalse(hints.mainTspExists)
        assertFalse(hints.resolvesEntrypoint)
    }

    @Test
    fun testInspectionWarnsWhenNoEntrypointResolves() {
        val config = myFixture.addFileToProject(
            "warn/tspconfig.yaml",
            """
            kind: project
            emit:
              - "@typespec/openapi3"
            """.trimIndent(),
        )

        val descriptions = inspectionMessages(config)

        assertTrue(descriptions.contains(TypeSpecBundle.message("inspection.tcfg001")))
    }

    @Test
    fun testInspectionIsQuietWhenMainTspResolvesEntrypoint() {
        myFixture.addFileToProject("quiet/main.tsp", "namespace Demo {}\n")
        val config = myFixture.addFileToProject(
            "quiet/tspconfig.yaml",
            """
            kind: project
            emit:
              - "@typespec/openapi3"
            """.trimIndent(),
        )

        val descriptions = inspectionMessages(config)

        assertFalse(descriptions.contains(TypeSpecBundle.message("inspection.tcfg001")))
    }

    private fun inspectionMessages(config: PsiFile): List<String> {
        val holder = ProblemsHolder(InspectionManager.getInstance(project), config, true)
        TypeSpecTspConfigInspection().buildVisitor(holder, true).visitFile(config)
        return holder.results.map { it.descriptionTemplate }
    }
}
