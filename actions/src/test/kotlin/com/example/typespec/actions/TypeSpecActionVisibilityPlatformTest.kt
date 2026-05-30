package com.example.typespec.actions

import com.example.typespec.TypeSpecServiceMode
import com.example.typespec.TypeSpecServiceSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class TypeSpecActionVisibilityPlatformTest : BasePlatformTestCase() {
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
