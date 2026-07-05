package com.example.typespec

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile

class TypeSpecTemplateContextType : TemplateContextType("TYPESPEC", "TypeSpec") {
    override fun isInContext(file: PsiFile): Boolean = file.fileType == TypeSpecFileType
}
