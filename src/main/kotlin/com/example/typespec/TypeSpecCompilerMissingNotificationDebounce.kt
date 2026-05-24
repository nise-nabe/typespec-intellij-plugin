package com.example.typespec

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm

@Service(Service.Level.PROJECT)
internal class TypeSpecCompilerMissingNotificationDebounce(
    private val project: Project,
) : Disposable {
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    fun scheduleRecheck() {
        alarm.cancelAllRequests()
        alarm.addRequest(
            {
                if (project.isDisposed) {
                    return@addRequest
                }
                TypeSpecLspPackageResolutionCoordinator.scheduleDeferredCompilerMissingRecheck(project)
            },
            debounceDelayMs(),
        )
    }

    fun cancel() {
        alarm.cancelAllRequests()
    }

    override fun dispose() {
        cancel()
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 2_000L

        fun debounceDelayMs(): Int =
            if (ApplicationManager.getApplication().isUnitTestMode) 0 else DEBOUNCE_DELAY_MS.toInt()

        fun getInstance(project: Project): TypeSpecCompilerMissingNotificationDebounce = project.service()
    }
}
