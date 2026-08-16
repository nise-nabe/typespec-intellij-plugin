package com.example.typespec.run

import com.example.typespec.TypeSpecBundle
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.icons.AllIcons

class TypeSpecCompileRunConfigurationType : ConfigurationTypeBase(
    "TypeSpecCompileRunConfigurationType",
    TypeSpecBundle.message("runConfiguration.typespecCompile.displayName"),
    TypeSpecBundle.message("runConfiguration.typespecCompile.description"),
    AllIcons.FileTypes.Any_type,
) {
    init {
        addFactory(TypeSpecCompileRunConfigurationFactory(this))
    }

    companion object {
        fun getInstance(): TypeSpecCompileRunConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(TypeSpecCompileRunConfigurationType::class.java)
    }
}
