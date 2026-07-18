package com.example.typespec.highlighting

import com.example.typespec.TypeSpecLanguage
import com.intellij.psi.tree.IElementType

internal object TypeSpecTokenTypes {
    val LINE_COMMENT = IElementType("TSP_LINE_COMMENT", TypeSpecLanguage)
    val BLOCK_COMMENT = IElementType("TSP_BLOCK_COMMENT", TypeSpecLanguage)
    val STRING = IElementType("TSP_STRING", TypeSpecLanguage)
    val KEYWORD = IElementType("TSP_KEYWORD", TypeSpecLanguage)
    val DECORATOR = IElementType("TSP_DECORATOR", TypeSpecLanguage)
    val IDENTIFIER = IElementType("TSP_IDENTIFIER", TypeSpecLanguage)
    val NUMBER = IElementType("TSP_NUMBER", TypeSpecLanguage)
    val OPERATION_SIGN = IElementType("TSP_OPERATION_SIGN", TypeSpecLanguage)
    val WHITESPACE = IElementType("TSP_WHITESPACE", TypeSpecLanguage)
}
