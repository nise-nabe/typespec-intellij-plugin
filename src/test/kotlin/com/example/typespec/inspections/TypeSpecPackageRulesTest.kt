package com.example.typespec.inspections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecPackageRulesTest {
    @Test
    fun supportingSignalsAloneDoNotClassifyPackage() {
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
            evaluateRules(input).map { it.rule }.toSet(),
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
                TypeSpecPackageJsonRule.TPKG005,
            ),
            evaluateRules(input).map { it.rule }.toSet(),
        )
        assertFalse(
            evaluateRules(input).any { it.rule == TypeSpecPackageJsonRule.TPKG003 },
        )
    }

    @Test
    fun mainWithSupportingSignalUsesFallbackInsteadOfMissingExportWarning() {
        val input = buildRulesInput(
            typespecExport = null,
            main = "dist/custom.js",
            tspMain = null,
            peerDependencies = mapOf("@typespec/http" to "~1.0.0"),
        )

        assertEquals(
            setOf(
                TypeSpecPackageJsonRule.TPKG001,
                TypeSpecPackageJsonRule.TPKG005,
            ),
            evaluateRules(input).map { it.rule }.toSet(),
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
            evaluateRules(input).map { it.rule }.toSet(),
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
            evaluateRules(input).map { it.rule }.toSet(),
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
    fun combinedRecommendedFixEligibilityRequiresAtLeastOneMissingRecommendedField() {
        val metadata = buildRulesInput(
            type = RECOMMENDED_TYPE_MODULE,
            main = RECOMMENDED_MAIN,
            typespecExport = RECOMMENDED_TYPESPEC_EXPORT,
            tspMain = null,
        )

        assertFalse(hasRecommendedMetadataFix(metadata))

        val missingMetadata = buildRulesInput(
            typespecExport = "./lib/main.tsp",
            main = null,
            tspMain = null,
        )
        assertTrue(hasRecommendedMetadataFix(missingMetadata))
    }
}

private fun hasRecommendedMetadataFix(input: TypeSpecPackageRulesInput): Boolean =
    input.type != RECOMMENDED_TYPE_MODULE ||
        input.main.isNullOrBlank() ||
        input.typespecExport.isNullOrBlank()
