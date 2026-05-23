package com.example.typespec.inspections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecPackageRulesTest {
    @Test
    fun typespecDependenciesAloneDoNotClassifyPackage() {
        val input = TypeSpecPackageRulesFixtures.typespecDependenciesOnly()

        assertFalse(input.isLikelyTypeSpecExtensionPackage)
        assertTrue(evaluateRules(input).isEmpty())
    }

    @Test
    fun exportsTypespecClassifiesAndReportsMissingMetadata() {
        val input = TypeSpecPackageRulesFixtures.exportsTypeSpecMissingMetadata()

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
        val input = TypeSpecPackageRulesFixtures.peerDependenciesMissingExport()

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
        val input = TypeSpecPackageRulesFixtures.tspMainFallback()

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
        val input = TypeSpecPackageRulesFixtures.mainWithTypespecDependency()

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
        val input = TypeSpecPackageRulesFixtures.devDependencyCompiler()

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
        val input = TypeSpecPackageRulesFixtures.existingPeerDependencySuppressesCompilerWarning()

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
        val input = TypeSpecPackageRulesFixtures.preferredLayout()

        assertTrue(evaluateRules(input).isEmpty())
    }

    @Test
    fun metadataRulesOfferCombinedRecommendedFix() {
        val input = TypeSpecPackageRulesFixtures.exportsTypeSpecMissingMetadata()

        assertEquals(
            setOf(TypeSpecPackageJsonFixAction.APPLY_RECOMMENDED_METADATA),
            evaluateRules(input).map { it.fixAction }.toSet(),
        )
    }

    @Test
    fun compilerRuleOffersMoveToPeerDependenciesFix() {
        val input = TypeSpecPackageRulesFixtures.devDependencyCompiler()

        assertEquals(
            TypeSpecPackageJsonFixAction.MOVE_COMPILER_TO_PEER_DEPENDENCIES,
            evaluateRules(input).single { it == TypeSpecPackageJsonRule.TPKG004 }.fixAction,
        )
    }
}
