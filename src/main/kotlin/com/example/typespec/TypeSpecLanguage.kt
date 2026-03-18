package com.example.typespec

import com.intellij.lang.Language

object TypeSpecLanguage : Language("TypeSpec") {
    private fun readResolve(): Any = TypeSpecLanguage
}
