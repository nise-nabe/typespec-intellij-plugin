package com.example.typespec.run

import com.example.typespec.TypeSpecFileType
import com.example.typespec.workflow.TypeSpecProjectContext
import com.example.typespec.workflow.TypeSpecTspConfigReader
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class TypeSpecCompileRunConfigurationProducer : LazyRunConfigurationProducer<TypeSpecCompileRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        TypeSpecCompileRunConfigurationType.INSTANCE.configurationFactories[0]

    override fun setupConfigurationFromContext(
        configuration: TypeSpecCompileRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val virtualFile = context.location?.virtualFile ?: return false
        if (virtualFile.fileType != TypeSpecFileType && virtualFile.name != TypeSpecProjectContext.TSP_CONFIG_FILE_NAME) {
            return false
        }
        val resolution = TypeSpecProjectContext.resolveFromVirtualFile(virtualFile) ?: return false
        val entrypoint = resolution.entrypointFile ?: return false
        val settings = configuration.settings()
        settings.projectRootPath = resolution.projectRoot.toString()
        settings.entrypointPath = entrypoint.toString()
        settings.emitters = TypeSpecTspConfigReader.readEmitters(resolution.projectRoot).joinToString(",")
        configuration.name = entrypoint.fileName.toString()
        return true
    }

    override fun isConfigurationFromContext(
        configuration: TypeSpecCompileRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val virtualFile = context.location?.virtualFile ?: return false
        val path = virtualFile.path
        return path == configuration.settings().entrypointPath ||
            path == configuration.settings().projectRootPath
    }
}
