package com.example.typespec.run

import com.example.typespec.workflow.TypeSpecCliResolver
import com.example.typespec.workflow.TypeSpecProjectContext
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
        val projectRoot = Paths.get(projectRootPath.ifEmpty { entrypointPath }.ifEmpty { return null })
        val resolvedRoot = if (Files.isDirectory(projectRoot)) {
            TypeSpecProjectContext.findProjectRoot(projectRoot) ?: projectRoot
        } else {
            TypeSpecProjectContext.findProjectRoot(projectRoot.parent) ?: projectRoot.parent
        }
        val entrypoint = when {
            entrypointPath.isNotBlank() -> Paths.get(entrypointPath)
            else -> TypeSpecProjectContext.resolveEntrypointFile(resolvedRoot, null) ?: return null
        }
        val cli = TypeSpecCliResolver.resolveTspCli(project, resolvedRoot) ?: return null
        val emitterList = emitters.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        val args = buildList {
            addAll(cli.args)
            add("compile")
            add(entrypoint.toString())
            emitterList.forEach { emitter ->
                add("--emit")
                add(emitter)
            }
            if (watch) {
                add("--watch")
            }
            if (extraArgs.isNotBlank()) {
                addAll(extraArgs.split(' ').filter { it.isNotEmpty() })
            }
        }
        return GeneralCommandLine()
            .withExePath(cli.executable)
            .withParameters(args)
            .withWorkDirectory(resolvedRoot.toFile())
            .withEnvironment("TYPESPEC_SKIP_COMPILER_RESOLVE", "1")
    }
}
