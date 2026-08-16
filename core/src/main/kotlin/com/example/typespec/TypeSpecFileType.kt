package com.example.typespec

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.icons.AllIcons
import javax.swing.Icon

object TypeSpecFileType : LanguageFileType(TypeSpecLanguage) {
    private fun readResolve(): Any = TypeSpecFileType

    override fun getName(): String = "TypeSpec"
    override fun getDescription(): String = "TypeSpec description"
    override fun getDefaultExtension(): String = "tsp"
    override fun getIcon(): Icon = AllIcons.FileTypes.Any_type
}
