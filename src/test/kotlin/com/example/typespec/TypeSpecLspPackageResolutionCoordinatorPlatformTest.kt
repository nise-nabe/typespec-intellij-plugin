package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.concurrency.AppExecutorUtil
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class TypeSpecLspPackageResolutionCoordinatorPlatformTest : BasePlatformTestCase() {
    private lateinit var packageDirectory: Path

    override fun setUp() {
        super.setUp()
        packageDirectory = Files.createTempDirectory("typespec-coordinator")
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().unwatch(project)
        resetTypeSpecLspRestartRequestCountForTests()
    }

    override fun tearDown() {
        try {
            resetTypeSpecLspRestartRequestCountForTests()
        } finally {
            try {
                packageDirectory.toFile().deleteRecursively()
            } finally {
                super.tearDown()
            }
        }
    }

    fun testOnConfigurationChangedClearsCompilerMissingTrackerWhenPackageIsResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspCoordinatorTestSupport.seedCompilerMissingNotification(tracker, packageKey)

        TypeSpecLspPackageResolutionCoordinator.onConfigurationChanged(project)
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testOnPackageRootAffectedClearsTrackerWhenPackageBecomesResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        TypeSpecLspCoordinatorTestSupport.seedCompilerMissingNotification(tracker, packageKey)

        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testOnPackageRootAffectedFromBackgroundThreadAppliesOnEdt() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")

        val failure = AtomicReference<Throwable>()
        val task = AppExecutorUtil.getAppExecutorService().submit {
            try {
                TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }
        task.get(10, TimeUnit.SECONDS)
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertNull(failure.get())
        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
    }

    fun testWatcherNotifiesCoordinatorWhenPackageRootChanges() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().watchPackageRoot(project, packageKey)
        TypeSpecLspCoordinatorTestSupport.seedCompilerMissingNotification(tracker, packageKey)

        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")

        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project).onPackageRootAffected()
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testMultiplexerRoutesPackageRootEventsToWatcher() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.lspServerPackage = NodePackage(packageKey)
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().watchPackageRoot(project, packageKey)

        val serverScriptPath = packageDirectory.resolve("cmd/tsp-server.js")
        Files.createDirectories(serverScriptPath.parent)
        Files.writeString(serverScriptPath, "// server")

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(serverScriptPath)
            ?: error("Expected server script to be visible in VFS")
        val changeEvent = VFileContentChangeEvent(
            this,
            virtualFile,
            virtualFile.modificationStamp - 1,
            virtualFile.modificationStamp,
            false,
        )
        val multiplexer = TypeSpecLspPackageRootVfsMultiplexer.getInstance()
        val applier = multiplexer.prepareChangeForTest(listOf(changeEvent))
        assertNotNull(applier)
        applier!!.afterVfsChange()
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
    }

    fun testOnTypeSpecFileOpenedDoesNotStartServerWhenActivationIsDisabledInUnitTestMode() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile
        var ensureServerStartedCalled = false
        val serverStarter = object : LspServerSupportProvider.LspServerStarter {
            override fun ensureServerStarted(descriptor: LspServerDescriptor) {
                ensureServerStartedCalled = true
            }
        }

        TypeSpecLspPackageResolutionCoordinator.onTypeSpecFileOpened(project, file, serverStarter)
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertFalse(ensureServerStartedCalled)
    }

    fun testOnPackageRootAffectedDoesNotRestartWhenResolvableStateWasUncached() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.lspServerPackage = NodePackage(packageDirectory.toString())
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertEquals(0, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testApplyResolutionSnapshotRestartsWhenPackageBecomesResolvable() {
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.applyResolutionSnapshotForTests(
            project,
            ResolutionSnapshot(isResolvable = true, wasResolvable = false),
            RestartPolicy.OnResolvableChange,
        )

        assertEquals(1, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testApplyResolutionSnapshotRestartsWhenPackageBecomesUnresolvable() {
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.applyResolutionSnapshotForTests(
            project,
            ResolutionSnapshot(isResolvable = false, wasResolvable = true),
            RestartPolicy.OnResolvableChange,
        )

        assertEquals(1, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testApplyResolutionSnapshotDoesNotRestartWhenResolvableStateIsUnchanged() {
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.applyResolutionSnapshotForTests(
            project,
            ResolutionSnapshot(isResolvable = false, wasResolvable = false),
            RestartPolicy.OnResolvableChange,
        )

        assertEquals(0, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testApplyResolutionSnapshotDoesNotRestartWhenPreviousResolvableStateWasUncached() {
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.applyResolutionSnapshotForTests(
            project,
            ResolutionSnapshot(isResolvable = false, wasResolvable = null),
            RestartPolicy.OnResolvableChange,
        )

        assertEquals(0, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testOnPackageRootAffectedDoesNotRestartWhenResolvableStateIsUnchanged() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        settings.lspServerPackage = NodePackage(packageDirectory.toString())
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()
        TypeSpecPackageResolution.isSelectedPackageResolvable(project)
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertEquals(0, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testOnConfigurationChangedClearsCompilerMissingTrackerWhenServiceDisabled() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = packageDirectory.toString()

        settings.serviceMode = TypeSpecServiceMode.ENABLED
        settings.lspServerPackage = NodePackage(packageKey)
        assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        TypeSpecLspCoordinatorTestSupport.seedCompilerMissingNotification(tracker, packageKey)

        settings.serviceMode = TypeSpecServiceMode.DISABLED
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testSettingsChangeUsesCoordinatorToClearTrackerWhenPackageBecomesResolvable() {
        val settings = TypeSpecServiceSettings.getInstance(project)
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val unresolvableDirectory = Files.createTempDirectory("typespec-unresolvable")
        try {
            settings.lspServerPackage = NodePackage(unresolvableDirectory.toString())
            val unresolvableKey = unresolvableDirectory.toString()
            TypeSpecLspCoordinatorTestSupport.seedCompilerMissingNotification(tracker, unresolvableKey)

            Files.createDirectories(packageDirectory.resolve("cmd"))
            Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
            settings.lspServerPackage = NodePackage(packageDirectory.toString())
            TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()

            assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
            assertTrue(tracker.tryAcquireCompilerMissingNotification(packageDirectory.toString()))
        } finally {
            unresolvableDirectory.toFile().deleteRecursively()
        }
    }
}
