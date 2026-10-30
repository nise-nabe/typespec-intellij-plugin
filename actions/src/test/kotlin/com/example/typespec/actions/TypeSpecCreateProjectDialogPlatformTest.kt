package com.example.typespec.actions

import com.example.typespec.TypeSpecBasePlatformTestCase
import com.intellij.openapi.util.Disposer

class TypeSpecCreateProjectDialogPlatformTest : TypeSpecBasePlatformTestCase() {
    fun testDialogInitializes() {
        val dialog = TypeSpecCreateProjectDialog()
        try {
            assertNotNull(dialog.title)
        } finally {
            Disposer.dispose(dialog.disposable)
        }
    }
}
