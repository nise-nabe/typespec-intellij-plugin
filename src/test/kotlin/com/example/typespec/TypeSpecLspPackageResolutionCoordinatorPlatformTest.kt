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
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
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

    fun testSupersededGenerationDoesNotCommitStaleResolvableState() {
        val cache = TypeSpecLspPackageResolutionCache.getInstance(project)
        val initialGeneration = cache.nextResolutionUpdateGeneration()
        assertTrue(
            TypeSpecLspPackageResolutionCoordinator.tryApplyResolutionSnapshotForTests(
                project,
                initialGeneration,
                ResolutionSnapshot(isResolvable = true),
                RestartPolicy.Never,
            ),
        )
        assertTrue(cache.peekResolvable()!!)

        val supersededGeneration = cache.nextResolutionUpdateGeneration()
        val latestGeneration = cache.nextResolutionUpdateGeneration()

        assertFalse(
            TypeSpecLspPackageResolutionCoordinator.tryApplyResolutionSnapshotForTests(
                project,
                supersededGeneration,
                ResolutionSnapshot(isResolvable = false),
                RestartPolicy.Never,
            ),
        )
        assertTrue(cache.peekResolvable()!!)

        assertTrue(
            TypeSpecLspPackageResolutionCoordinator.tryApplyResolutionSnapshotForTests(
                project,
                latestGeneration,
                ResolutionSnapshot(isResolvable = false),
                RestartPolicy.Never,
            ),
        )
        assertFalse(cache.peekResolvable()!!)
    }

    fun testOnConfigurationChangedClearsCompilerMissingTrackerWhenPackageIsResolvable() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = selectedPackageKey

        writeTypeSpecServerScript()
        configureSelectedPackage()
        seedCompilerMissingNotification(tracker, packageKey)

        TypeSpecLspPackageResolutionCoordinator.onConfigurationChanged(project)
        drainCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testOnPackageRootAffectedClearsTrackerWhenPackageBecomesResolvable() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = selectedPackageKey

        configureSelectedPackage()
        assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        seedCompilerMissingNotification(tracker, packageKey)

        writeTypeSpecServerScript()

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        drainCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testOnPackageRootAffectedFromBackgroundThreadAppliesOnEdt() {
        configureSelectedPackage()
        writeTypeSpecServerScript()

        val failure = AtomicReference<Throwable>()
        val task = AppExecutorUtil.getAppExecutorService().submit {
            try {
                TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }
        task.get(10, TimeUnit.SECONDS)
        drainCoordinatorQueues()

        assertNull(failure.get())
        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
    }

    fun testWatcherNotifiesCoordinatorWhenPackageRootChanges() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = selectedPackageKey

        configureSelectedPackage()
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().watchPackageRoot(project, packageKey)
        seedCompilerMissingNotification(tracker, packageKey)

        writeTypeSpecServerScript()

        TypeSpecLspPackageResolutionCacheWatcher.getInstance(project).onPackageRootAffected()
        drainCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testMultiplexerRoutesPackageRootEventsToWatcher() {
        configureSelectedPackage()
        TypeSpecLspPackageRootVfsMultiplexer.getInstance().watchPackageRoot(project, selectedPackageKey)

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
        drainCoordinatorQueues()

        assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
    }

    fun testOnTypeSpecFileOpenedDoesNotStartServerWhenActivationIsDisabledInUnitTestMode() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
        val file = myFixture.configureByText("main.tsp", "namespace Demo {}").virtualFile
        var ensureServerStartedCalled = false
        val serverStarter = object : LspServerSupportProvider.LspServerStarter {
            override fun ensureServerStarted(descriptor: LspServerDescriptor) {
                ensureServerStartedCalled = true
            }
        }

        TypeSpecLspPackageResolutionCoordinator.onTypeSpecFileOpened(project, file, serverStarter)
        drainCoordinatorQueues()

        assertFalse(ensureServerStartedCalled)
    }

    fun testOnPackageRootAffectedDoesNotRestartWhenResolvableStateWasUncached() {
        configureSelectedPackage()
        drainCoordinatorQueues()
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        drainCoordinatorQueues()

        assertEquals(0, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testApplyResolutionSnapshotDoesNotRestartOnResolvableChangeWhenServiceDisabled() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.DISABLED
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.applyResolutionSnapshotForTests(
            project,
            ResolutionSnapshot(isResolvable = true, wasResolvable = false),
            RestartPolicy.OnResolvableChange,
        )

        assertEquals(0, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testOnPackageRootAffectedDoesNotRestartWhenServiceDisabled() {
        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
        configureSelectedPackage()
        drainCoordinatorQueues()
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.DISABLED
        writeTypeSpecServerScript()

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        drainCoordinatorQueues()

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
        configureSelectedPackage()
        drainCoordinatorQueues()
        TypeSpecPackageResolution.isSelectedPackageResolvable(project)
        resetTypeSpecLspRestartRequestCountForTests()

        TypeSpecLspPackageResolutionCoordinator.onPackageRootAffected(project)
        drainCoordinatorQueues()

        assertEquals(0, typeSpecLspRestartRequestCountForTests.get())
    }

    fun testOnConfigurationChangedClearsCompilerMissingTrackerWhenServiceDisabled() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val packageKey = selectedPackageKey

        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.ENABLED
        configureSelectedPackage()
        assertFalse(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
        seedCompilerMissingNotification(tracker, packageKey)

        TypeSpecServiceSettings.getInstance(project).serviceMode = TypeSpecServiceMode.DISABLED
        drainCoordinatorQueues()

        assertTrue(tracker.tryAcquireCompilerMissingNotification(packageKey))
    }

    fun testSettingsChangeUsesCoordinatorToClearTrackerWhenPackageBecomesResolvable() {
        val tracker = TypeSpecLspNotificationTracker.getInstance(project)
        val unresolvableDirectory = Files.createTempDirectory("typespec-unresolvable")
        try {
            val unresolvableKey = unresolvableDirectory.toString()
            configureLspPackage(unresolvableKey)
            seedCompilerMissingNotification(tracker, unresolvableKey)

            writeTypeSpecServerScript()
            configureSelectedPackage()
            drainCoordinatorQueues()

            assertTrue(TypeSpecPackageResolution.isSelectedPackageResolvable(project))
            assertTrue(tracker.tryAcquireCompilerMissingNotification(selectedPackageKey))
        } finally {
            unresolvableDirectory.toFile().deleteRecursively()
        }
    }

    private val selectedPackageKey: String
        get() = packageDirectory.toString()

    private fun configureSelectedPackage() {
        configureLspPackage(selectedPackageKey)
    }

    private fun configureLspPackage(packageKey: String) {
        TypeSpecServiceSettings.getInstance(project).lspServerPackage = NodePackage(packageKey)
    }

    private fun writeTypeSpecServerScript() {
        TypeSpecLspCoordinatorTestSupport.writeTypeSpecServerScript(packageDirectory)
    }

    private fun drainCoordinatorQueues() {
        TypeSpecLspCoordinatorTestSupport.drainResolutionCoordinatorQueues()
    }

    private fun seedCompilerMissingNotification(tracker: TypeSpecLspNotificationTracker, packageKey: String) {
        TypeSpecLspCoordinatorTestSupport.seedCompilerMissingNotification(tracker, packageKey)
    }
}
