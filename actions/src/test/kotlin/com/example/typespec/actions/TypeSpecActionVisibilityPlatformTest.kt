package com.example.typespec.actions

import com.example.typespec.TypeSpecBasePlatformTestCase
import com.example.typespec.TypeSpecFileType
import com.example.typespec.TypeSpecPackageResolutionCache
import com.example.typespec.TypeSpecServiceMode
import com.example.typespec.TypeSpecServiceSettings
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.TestActionEvent
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class TypeSpecActionVisibilityPlatformTest : TypeSpecBasePlatformTestCase() {
    private lateinit var packageDirectory: Path

    override fun setUp() {
        super.setUp()
        packageDirectory = Files.createTempDirectory("typespec-compiler")
    }

    override fun tearDown() {
        try {
            packageDirectory.toFile().deleteRecursively()
        } finally {
            super.tearDown()
        }
    }
    fun testShowOutputVisibleForOpenProject() {
        val action = TypeSpecShowOutputAction()
        val event = testEvent(action)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun testRestartServerHiddenWhenServiceDisabled() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.DISABLED
        val action = TypeSpecRestartServerAction()
        val event = testEvent(action)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testRestartServerVisibleWhenServiceEnabled() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
        val action = TypeSpecRestartServerAction()
        val event = testEvent(action)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun testEmitHiddenForNonTypeSpecFile() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
        val action = TypeSpecEmitFromTypeSpecAction()
        val jsonFile = myFixture.configureByText("sample.json", "{}").virtualFile
        val event = testEvent(action, jsonFile)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testEmitHiddenWhenCompilerCliNotResolvable() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
        val action = TypeSpecEmitFromTypeSpecAction()
        val tspFile = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile
        val event = testEvent(action, tspFile)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testEmitHiddenWhenServiceDisabled() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.DISABLED
        val action = TypeSpecEmitFromTypeSpecAction()
        val tspFile = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile
        val event = testEvent(action, tspFile)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testInstallDependenciesHiddenWhenCompilerCliNotResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.DISABLED
        settings.lspServerPackage = NodePackage(packageDirectory.toString())
        TypeSpecPackageResolutionCache.getInstance(project).invalidate()

        val action = TypeSpecInstallDependenciesAction()
        val tspFile = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile
        val event = testEvent(action, tspFile)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testInstallDependenciesVisibleWhenServiceDisabledAndCompilerCliResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.DISABLED
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp.js"), "// compiler")
        settings.lspServerPackage = NodePackage(packageDirectory.toString())
        TypeSpecPackageResolutionCache.getInstance(project).invalidate()

        val action = TypeSpecInstallDependenciesAction()
        val tspFile = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile
        val event = testEvent(action, tspFile)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun testInstallDependenciesHiddenForNonTypeSpecFile() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
        val action = TypeSpecInstallDependenciesAction()
        val jsonFile = myFixture.configureByText("sample.json", "{}").virtualFile
        val event = testEvent(action, jsonFile)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testInstallDependenciesHiddenForNonLocalVirtualFile() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.DISABLED
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp.js"), "// compiler")
        settings.lspServerPackage = NodePackage(packageDirectory.toString())
        TypeSpecPackageResolutionCache.getInstance(project).invalidate()

        val action = TypeSpecInstallDependenciesAction()
        val scratchFile = LightVirtualFile("main.tsp", TypeSpecFileType, "namespace Demo {}")
        val event = testEvent(action, scratchFile)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    private fun testEvent(action: AnAction, file: VirtualFile? = null) =
        TestActionEvent.createTestEvent(action, dataContext(file))

    private fun dataContext(file: VirtualFile? = null): DataContext =
        DataContext { dataId ->
            when {
                CommonDataKeys.PROJECT.`is`(dataId) -> project
                CommonDataKeys.VIRTUAL_FILE.`is`(dataId) -> file
                else -> null
            }
        }
}
