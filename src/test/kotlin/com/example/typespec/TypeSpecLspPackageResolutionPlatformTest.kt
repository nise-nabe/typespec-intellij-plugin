package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecLspPackageResolutionPlatformTest : BasePlatformTestCase() {
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

    fun testSelectedPackageResolvableReflectsPackageOnDisk() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.lspServerPackage = NodePackage(packageDirectory.toString())

        assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))

        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
    }

    fun testResolvablePackageAllowsCompilerMissingNotificationAgainAfterClear() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
        settings.lspServerPackage = NodePackage(packageDirectory.toString())
        TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()

        val packageKey = packageDirectory.toString()
        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
        assertFalse(tracker.shouldNotifyCompilerMissing(packageKey))

        tracker.clearCompilerMissingNotification()

        assertTrue(tracker.shouldNotifyCompilerMissing(packageKey))
    }

    fun testConfigurationChangeClearsCompilerMissingNotificationWhenPackageBecomesResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val unresolvableDirectory = Files.createTempDirectory("typespec-unresolvable")
        try {
            settings.lspServerPackage = NodePackage(unresolvableDirectory.toString())
            TypeSpecLspPackageResolutionCache.getInstance(project).invalidate()

            val unresolvableKey = unresolvableDirectory.toString()
            assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
            assertTrue(tracker.shouldNotifyCompilerMissing(unresolvableKey))
            assertFalse(tracker.shouldNotifyCompilerMissing(unresolvableKey))

            Files.createDirectories(packageDirectory.resolve("cmd"))
            Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
            settings.lspServerPackage = NodePackage(packageDirectory.toString())

            assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
            assertTrue(tracker.shouldNotifyCompilerMissing(packageDirectory.toString()))
        } finally {
            unresolvableDirectory.toFile().deleteRecursively()
        }
    }
}
