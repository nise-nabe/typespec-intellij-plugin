package com.example.typespec

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class TypeSpecTemplateContextType : TemplateContextType("TYPESPEC", "TypeSpec") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean =
        templateActionContext.file.fileType == TypeSpecFileType
}
