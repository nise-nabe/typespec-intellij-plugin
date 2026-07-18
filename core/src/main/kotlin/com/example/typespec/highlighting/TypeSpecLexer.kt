package com.example.typespec.highlighting

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

internal class TypeSpecLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = currentType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            currentType = null
            return
        }

        val current = buffer[tokenStart]
        when {
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '/' -> {
                tokenEnd = findLineEnd(tokenStart + 2)
                currentType = TypeSpecTokenTypes.LINE_COMMENT
            }
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '*' -> {
                tokenEnd = findBlockCommentEnd(tokenStart + 2)
                currentType = TypeSpecTokenTypes.BLOCK_COMMENT
            }
            current == '"' || current == '\'' -> {
                tokenEnd = findStringEnd(tokenStart + 1, current)
                currentType = TypeSpecTokenTypes.STRING
            }
            current.isWhitespace() -> {
                tokenEnd = tokenStart + 1
                while (tokenEnd < endOffset && buffer[tokenEnd].isWhitespace()) {
                    tokenEnd++
                }
                currentType = TypeSpecTokenTypes.WHITESPACE
            }
            current.isLetter() || current == '@' || current == '_' -> {
                tokenEnd = tokenStart + 1
                while (tokenEnd < endOffset && (buffer[tokenEnd].isLetterOrDigit() || buffer[tokenEnd] in "._@")) {
                    tokenEnd++
                }
                val word = buffer.subSequence(tokenStart, tokenEnd).toString()
                currentType = when {
                    TypeSpecKeywords.contains(word) -> TypeSpecTokenTypes.KEYWORD
                    word.startsWith("@") -> TypeSpecTokenTypes.DECORATOR
                    else -> TypeSpecTokenTypes.IDENTIFIER
                }
            }
            current.isDigit() -> {
                tokenEnd = tokenStart + 1
                while (tokenEnd < endOffset && (buffer[tokenEnd].isDigit() || buffer[tokenEnd] == '.')) {
                    tokenEnd++
                }
                currentType = TypeSpecTokenTypes.NUMBER
            }
            else -> {
                tokenEnd = tokenStart + 1
                currentType = TypeSpecTokenTypes.OPERATION_SIGN
            }
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun findLineEnd(start: Int): Int {
        var index = start
        while (index < endOffset && buffer[index] != '\n') {
            index++
        }
        return index
    }

    private fun findBlockCommentEnd(start: Int): Int {
        var index = start
        while (index + 1 < endOffset) {
            if (buffer[index] == '*' && buffer[index + 1] == '/') {
                return index + 2
            }
            index++
        }
        return endOffset
    }

    private fun findStringEnd(start: Int, quote: Char): Int {
        var index = start
        while (index < endOffset) {
            if (buffer[index] == '\\') {
                index += 2
                continue
            }
            if (buffer[index] == quote) {
                return index + 1
            }
            index++
        }
        return endOffset
    }
}
