package com.example.typespec

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeSpecLspServerActivationRuleTest : BasePlatformTestCase() {
    fun testIsFileAcceptableReturnsFalseForNonTypeSpecFile() {
        val file = myFixture.configureByText("main.json", "{}").virtualFile

        assertFalse(TypeSpecLspServerActivationRule.isFileAcceptable(file))
    }

    fun testIsEnabledIsFalseInUnitTestModeForTypeSpecFile() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile

        assertFalse(TypeSpecLspServerActivationRule.isEnabled(project, file))
    }

    fun testIsEnabledAndAvailableIsFalseWhenPackageIsNotResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile

        assertFalse(TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file))
    }
}
