package com.example.typespec.highlighting

import com.example.typespec.TypeSpecLanguage
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class TypeSpecSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        TypeSpecSyntaxHighlighter()
}

internal class TypeSpecSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = TypeSpecLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            TypeSpecTokenTypes.KEYWORD -> KEYWORD_KEYS
            TypeSpecTokenTypes.DECORATOR -> DECORATOR_KEYS
            TypeSpecTokenTypes.STRING -> STRING_KEYS
            TypeSpecTokenTypes.LINE_COMMENT -> COMMENT_KEYS
            TypeSpecTokenTypes.NUMBER -> NUMBER_KEYS
            TypeSpecTokenTypes.OPERATION_SIGN -> BRACES_KEYS
            else -> EMPTY_KEYS
        }

    companion object {
        private val KEYWORD_KEYS = arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
        private val DECORATOR_KEYS = arrayOf(DefaultLanguageHighlighterColors.METADATA)
        private val STRING_KEYS = arrayOf(DefaultLanguageHighlighterColors.STRING)
        private val COMMENT_KEYS = arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
        private val NUMBER_KEYS = arrayOf(DefaultLanguageHighlighterColors.NUMBER)
        private val BRACES_KEYS = arrayOf(DefaultLanguageHighlighterColors.BRACES)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
