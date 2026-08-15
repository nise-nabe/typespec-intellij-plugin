package com.example.typespec

import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeSpecLspServerActivationRuleTest : BasePlatformTestCase() {
    fun testIsFileAcceptableReturnsTrueForLocalTypeSpecFile() {
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile

        assertTrue(file.isInLocalFileSystem)
        assertTrue(TypeSpecLspServerActivationRule.isFileAcceptable(file))
    }

    fun testIsFileAcceptableReturnsFalseForNonTypeSpecFile() {
        val file = myFixture.configureByText("main.json", "{}").virtualFile

        assertFalse(TypeSpecLspServerActivationRule.isFileAcceptable(file))
    }

    fun testIsFileAcceptableReturnsFalseForNonLocalTypeSpecFile() {
        val file = LightVirtualFile("main.tsp", TypeSpecFileType, "namespace Demo {}")

        assertFalse(file.isInLocalFileSystem)
        assertFalse(TypeSpecLspServerActivationRule.isFileAcceptable(file))
    }

    fun testIsFileAcceptableReturnsFalseForDirectory() {
        val directory = myFixture.tempDirFixture.findOrCreateDir("typespec-dir")

        assertTrue(directory.isDirectory)
        assertFalse(TypeSpecLspServerActivationRule.isFileAcceptable(directory))
    }

    fun testIsEligibleExceptPackageResolutionIsFalseInUnitTestModeForTypeSpecFile() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile

        assertFalse(TypeSpecLspServerActivationRule.isEligibleExceptPackageResolution(project, file))
    }

    fun testIsEnabledAndAvailableIsFalseWhenPackageIsNotResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile

        assertFalse(TypeSpecLspServerActivationRule.isEnabledAndAvailable(project, file))
    }
}
