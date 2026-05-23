package com.example.typespec.inspections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecPackageRulesTest {
    @Test
    fun typespecDependenciesAloneDoNotClassifyPackage() {
        val input = buildRulesInput(
            typespecExport = null,
            main = null,
            tspMain = null,
            dependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to "~1.0.0"),
        )

        assertFalse(input.isLikelyTypeSpecExtensionPackage)
        assertTrue(evaluateRules(input).isEmpty())
    }

    @Test
    fun exportsTypespecClassifiesAndReportsMissingMetadata() {
        val input = buildRulesInput(
            typespecExport = "./lib/main.tsp",
            main = null,
            tspMain = null,
        )

        assertTrue(input.isLikelyTypeSpecExtensionPackage)
        assertEquals(
            setOf(
                TypeSpecPackageJsonRule.TPKG001,
                TypeSpecPackageJsonRule.TPKG002,
            ),
            evaluateRules(input).toSet(),
        )
    }

    @Test
    fun peerDependenciesClassifyPackageAndReportMissingTypespecExport() {
        val input = buildRulesInput(
            typespecExport = null,
            main = null,
            tspMain = null,
            peerDependencies = mapOf("@typespec/http" to "~1.0.0"),
        )

        assertTrue(input.isLikelyTypeSpecExtensionPackage)
        assertEquals(
            setOf(
                TypeSpecPackageJsonRule.TPKG001,
                TypeSpecPackageJsonRule.TPKG002,
                TypeSpecPackageJsonRule.TPKG003,
            ),
            evaluateRules(input).toSet(),
        )
        assertEquals(
            TypeSpecFindingSeverity.WARNING,
            TypeSpecPackageJsonRule.TPKG003.severity(input),
        )
        assertEquals(
            "inspection.tpkg003",
            TypeSpecPackageJsonRule.TPKG003.messageKey(input),
        )
    }

    @Test
    fun tspMainFallbackReportsInformationalRecommendation() {
        val input = buildRulesInput(
            typespecExport = null,
            main = null,
            tspMain = "./lib/main.tsp",
        )

        assertEquals(
            setOf(
                TypeSpecPackageJsonRule.TPKG001,
                TypeSpecPackageJsonRule.TPKG002,
                TypeSpecPackageJsonRule.TPKG003,
            ),
            evaluateRules(input).toSet(),
        )
        assertEquals(
            TypeSpecFindingSeverity.INFORMATION,
            TypeSpecPackageJsonRule.TPKG003.severity(input),
        )
        assertEquals(
            "inspection.tpkg005",
            TypeSpecPackageJsonRule.TPKG003.messageKey(input),
        )
    }

    @Test
    fun mainWithTypespecDependencyUsesFallbackInsteadOfMissingExportWarning() {
        val input = buildRulesInput(
            typespecExport = null,
            main = "dist/custom.js",
            tspMain = null,
            peerDependencies = mapOf("@typespec/http" to "~1.0.0"),
        )

        assertEquals(
            setOf(
                TypeSpecPackageJsonRule.TPKG001,
                TypeSpecPackageJsonRule.TPKG003,
            ),
            evaluateRules(input).toSet(),
        )
        assertEquals(
            TypeSpecFindingSeverity.INFORMATION,
            TypeSpecPackageJsonRule.TPKG003.severity(input),
        )
    }

    @Test
    fun devDependencyCompilerReportsCompilerWarning() {
        val input = buildRulesInput(
            typespecExport = "./lib/main.tsp",
            main = null,
            tspMain = null,
            devDependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to "~1.0.0"),
        )

        assertEquals(
            setOf(
                TypeSpecPackageJsonRule.TPKG001,
                TypeSpecPackageJsonRule.TPKG002,
                TypeSpecPackageJsonRule.TPKG004,
            ),
            evaluateRules(input).toSet(),
        )
    }

    @Test
    fun existingPeerDependencySuppressesCompilerWarning() {
        val input = buildRulesInput(
            typespecExport = "./lib/main.tsp",
            main = null,
            tspMain = null,
            dependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to "~1.0.0"),
            peerDependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to "~2.0.0"),
        )

        assertEquals(
            setOf(
                TypeSpecPackageJsonRule.TPKG001,
                TypeSpecPackageJsonRule.TPKG002,
            ),
            evaluateRules(input).toSet(),
        )
    }

    @Test
    fun preferredLayoutDoesNotReportFallbackOrMissingExportWarnings() {
        val input = buildRulesInput(
            type = RECOMMENDED_TYPE_MODULE,
            main = RECOMMENDED_MAIN,
            tspMain = null,
            typespecExport = RECOMMENDED_TYPESPEC_EXPORT,
        )

        assertTrue(evaluateRules(input).isEmpty())
    }

    @Test
    fun metadataRulesOfferCombinedRecommendedFix() {
        val input = buildRulesInput(
            typespecExport = "./lib/main.tsp",
            main = null,
            tspMain = null,
        )

        assertEquals(
            setOf(TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA),
            evaluateRules(input).map { it.fixAction }.toSet(),
        )
    }

    @Test
    fun compilerRuleOffersMoveToPeerDependenciesFix() {
        val input = buildRulesInput(
            typespecExport = "./lib/main.tsp",
            main = null,
            tspMain = null,
            devDependencies = mapOf(TYPESPEC_COMPILER_PACKAGE to "~1.0.0"),
        )

        assertEquals(
            TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES,
            evaluateRules(input).single { it == TypeSpecPackageJsonRule.TPKG004 }.fixAction,
        )
    }
}
