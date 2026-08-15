package com.example.typespec

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeSpecLspServerActivationRuleTest : BasePlatformTestCase() {
    private var previousTspFileType: FileType? = null

    override fun setUp() {
        super.setUp()
        val manager = FileTypeManager.getInstance()
        val extension = TypeSpecFileType.defaultExtension
        previousTspFileType = manager.getFileTypeByExtension(extension)
        runWriteAction {
            manager.associateExtension(TypeSpecFileType, extension)
        }
    }

    override fun tearDown() {
        try {
            val previous = previousTspFileType
            if (previous != null) {
                val manager = FileTypeManager.getInstance()
                val extension = TypeSpecFileType.defaultExtension
                runWriteAction {
                    manager.removeAssociatedExtension(TypeSpecFileType, extension)
                    if (previous !== FileTypes.UNKNOWN) {
                        manager.associateExtension(previous, extension)
                    }
                }
            }
        } finally {
            super.tearDown()
        }
    }

    fun testIsFileAcceptableReturnsTrueForLocalTypeSpecFile() {
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile

        assertTrue(file.isInLocalFileSystem)
        assertEquals(TypeSpecFileType, file.fileType)
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
