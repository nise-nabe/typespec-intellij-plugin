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

    fun readExternal(element: Element) {
        entrypointPath = element.getAttributeValue("entrypoint") ?: ""
        projectRootPath = element.getAttributeValue("projectRoot") ?: ""
        emitters = element.getAttributeValue("emitters") ?: ""
        extraArgs = element.getAttributeValue("extraArgs") ?: ""
        watch = element.getAttributeValue("watch")?.toBooleanStrictOrNull() ?: false
    }

    fun writeExternal(element: Element) {
        element.setAttribute("entrypoint", entrypointPath)
        element.setAttribute("projectRoot", projectRootPath)
        element.setAttribute("emitters", emitters)
        element.setAttribute("extraArgs", extraArgs)
        element.setAttribute("watch", watch.toString())
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
        )
        return TypeSpecCompileCommandBuilder.buildCommandLine(project, request)
    }
}
