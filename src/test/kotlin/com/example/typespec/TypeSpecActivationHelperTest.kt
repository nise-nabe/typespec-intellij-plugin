package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import com.intellij.javascript.nodejs.util.NodePackage
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecActivationHelperTest {
    @Test
    fun isEnvironmentSupportedReturnsFalseInUnitTestMode() {
        assertFalse(TypeSpecActivationHelper.isEnvironmentSupported(isUnitTestMode = true))
    }

    @Test
    fun isEnvironmentSupportedReturnsTrueOutsideUnitTestMode() {
        assertTrue(TypeSpecActivationHelper.isEnvironmentSupported(isUnitTestMode = false))
    }
}

class TypeSpecCompilerPackageResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun hasProjectLocalCompilerPackageReturnsFalseWhenMissing() {
        assertFalse(TypeSpecCompilerPackageResolver.hasProjectLocalCompilerPackage(tempDir.toString()))
    }

    @Test
    fun hasProjectLocalCompilerPackageReturnsFalseWhenBasePathIsNull() {
        assertFalse(TypeSpecCompilerPackageResolver.hasProjectLocalCompilerPackage(null))
    }

    @Test
    fun hasProjectLocalCompilerPackageReturnsTrueWhenPackageJsonExists() {
        val packageDirectory = tempDir.resolve("node_modules").resolve(TYPESPEC_COMPILER_PACKAGE_NAME)
        Files.createDirectories(packageDirectory)
        Files.writeString(packageDirectory.resolve("package.json"), "{}")

        assertTrue(TypeSpecCompilerPackageResolver.hasProjectLocalCompilerPackage(tempDir.toString()))
    }

    @Test
    fun isPackageWithServerScriptReturnsTrueWhenScriptExists() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
        val nodePackage = NodePackage(packageDirectory.toString())

        assertTrue(TypeSpecCompilerPackageResolver.isPackageWithServerScript(nodePackage))
    }

    @Test
    fun isPackageWithServerScriptReturnsFalseWhenScriptMissing() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory)
        val nodePackage = NodePackage(packageDirectory.toString())

        assertFalse(TypeSpecCompilerPackageResolver.isPackageWithServerScript(nodePackage))
    }
}

class TypeSpecLspNotificationTrackerLogicTest {
    @Test
    fun shouldNotifyCompilerMissingOnlyOnceForSamePackageKey() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertFalse(tracker.shouldNotifyCompilerMissing("default"))
    }

    @Test
    fun shouldNotifyCompilerMissingAgainAfterPackageKeyChanges() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        assertTrue(tracker.shouldNotifyCompilerMissing("custom-path"))
    }

    @Test
    fun clearCompilerMissingNotificationAllowsSubsequentNotification() {
        val tracker = TypeSpecLspNotificationTracker()

        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
        tracker.clearCompilerMissingNotification()
        assertTrue(tracker.shouldNotifyCompilerMissing("default"))
    }
}
