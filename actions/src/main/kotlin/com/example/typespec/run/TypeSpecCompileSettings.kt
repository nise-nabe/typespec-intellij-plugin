package com.example.typespec.run

import com.example.typespec.workflow.TypeSpecCompileCommandBuilder
import com.example.typespec.workflow.TypeSpecCompileRequest
import com.example.typespec.workflow.TypeSpecCompileRootsResolver
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import org.jdom.Element

class TypeSpecCompileSettings {
    var entrypointPath: String = ""
    var projectRootPath: String = ""
    var emitters: String = ""
    var extraArgs: String = ""
    var watch: Boolean = false
    var dryRun: Boolean = false
    var noEmit: Boolean = false
    var stats: Boolean = false
    var trace: String = ""
    var warnAsError: Boolean = false

    fun readExternal(element: Element) {
        entrypointPath = element.getAttributeValue("entrypoint") ?: ""
        projectRootPath = element.getAttributeValue("projectRoot") ?: ""
        emitters = element.getAttributeValue("emitters") ?: ""
        extraArgs = element.getAttributeValue("extraArgs") ?: ""
        watch = element.getAttributeValue("watch")?.toBooleanStrictOrNull() ?: false
        dryRun = element.getAttributeValue("dryRun")?.toBooleanStrictOrNull() ?: false
        noEmit = element.getAttributeValue("noEmit")?.toBooleanStrictOrNull() ?: false
        stats = element.getAttributeValue("stats")?.toBooleanStrictOrNull() ?: false
        trace = element.getAttributeValue("trace") ?: ""
        warnAsError = element.getAttributeValue("warnAsError")?.toBooleanStrictOrNull() ?: false
    }

    fun writeExternal(element: Element) {
        element.setAttribute("entrypoint", entrypointPath)
        element.setAttribute("projectRoot", projectRootPath)
        element.setAttribute("emitters", emitters)
        element.setAttribute("extraArgs", extraArgs)
        element.setAttribute("watch", watch.toString())
        element.setAttribute("dryRun", dryRun.toString())
        element.setAttribute("noEmit", noEmit.toString())
        element.setAttribute("stats", stats.toString())
        element.setAttribute("trace", trace)
        element.setAttribute("warnAsError", warnAsError.toString())
    }

    fun buildCommandLine(project: Project): GeneralCommandLine? {
        val roots = TypeSpecCompileRootsResolver.resolve(entrypointPath, projectRootPath) ?: return null
        val emitterList = emitters.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        val extraArgList = extraArgs.split(' ').filter { it.isNotEmpty() }
        val request = TypeSpecCompileRequest(
            projectRoot = roots.projectRoot,
            entrypoint = roots.entrypoint,
            emitters = emitterList,
            extraArgs = extraArgList,
            watch = watch,
            dryRun = dryRun,
            noEmit = noEmit,
            stats = stats,
            trace = trace,
            warnAsError = warnAsError,
        )
        return TypeSpecCompileCommandBuilder.buildCommandLine(project, request)
    }
}
