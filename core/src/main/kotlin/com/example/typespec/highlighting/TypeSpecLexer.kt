package com.example.typespec.highlighting

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Highlighting lexer for TypeSpec. Tracks a small integer state so incremental
 * re-lex can resume inside block comments and (triple-quoted) strings.
 */
internal class TypeSpecLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentType: IElementType? = null
    private var state: Int = STATE_DEFAULT

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        state = initialState
        advance()
    }

    override fun getState(): Int = state

    override fun getTokenType(): IElementType? = currentType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            currentType = null
            return
        }

        when (state) {
            STATE_BLOCK_COMMENT -> {
                tokenEnd = findBlockCommentEnd(tokenStart)
                currentType = TypeSpecTokenTypes.BLOCK_COMMENT
                state = if (endsWithBlockCommentClose(tokenStart, tokenEnd)) {
                    STATE_DEFAULT
                } else {
                    STATE_BLOCK_COMMENT
                }
            }
            STATE_STRING -> {
                tokenEnd = findStringEnd(tokenStart)
                currentType = TypeSpecTokenTypes.STRING
                state = if (endsWithUnescapedQuote(tokenStart, tokenEnd)) {
                    STATE_DEFAULT
                } else {
                    STATE_STRING
                }
            }
            STATE_TRIPLE_STRING -> {
                tokenEnd = findTripleQuotedStringEnd(tokenStart)
                currentType = TypeSpecTokenTypes.STRING
                state = if (endsWithTripleQuote(tokenStart, tokenEnd)) {
                    STATE_DEFAULT
                } else {
                    STATE_TRIPLE_STRING
                }
            }
            else -> advanceDefault()
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun advanceDefault() {
        val current = buffer[tokenStart]
        when {
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '/' -> {
                tokenEnd = findLineEnd(tokenStart + 2)
                currentType = TypeSpecTokenTypes.LINE_COMMENT
                state = STATE_DEFAULT
            }
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '*' -> {
                tokenEnd = findBlockCommentEnd(tokenStart + 2)
                currentType = TypeSpecTokenTypes.BLOCK_COMMENT
                state = if (endsWithBlockCommentClose(tokenStart, tokenEnd)) {
                    STATE_DEFAULT
                } else {
                    STATE_BLOCK_COMMENT
                }
            }
            current == '"' && isTripleQuoteAt(tokenStart) -> {
                tokenEnd = findTripleQuotedStringEnd(tokenStart + 3)
                currentType = TypeSpecTokenTypes.STRING
                state = if (endsWithTripleQuote(tokenStart, tokenEnd)) {
                    STATE_DEFAULT
                } else {
                    STATE_TRIPLE_STRING
                }
            }
            current == '"' -> {
                tokenEnd = findStringEnd(tokenStart + 1)
                currentType = TypeSpecTokenTypes.STRING
                state = if (endsWithUnescapedQuote(tokenStart, tokenEnd)) {
                    STATE_DEFAULT
                } else {
                    STATE_STRING
                }
            }
            current.isWhitespace() -> {
                tokenEnd = tokenStart + 1
                while (tokenEnd < endOffset && buffer[tokenEnd].isWhitespace()) {
                    tokenEnd++
                }
                currentType = TypeSpecTokenTypes.WHITESPACE
                state = STATE_DEFAULT
            }
            current == '@' -> {
                // Highlight `@name` / `@@name` as one decorator token for editor UX.
                tokenEnd = tokenStart + 1
                if (tokenEnd < endOffset && buffer[tokenEnd] == '@') {
                    tokenEnd++
                }
                while (tokenEnd < endOffset && isIdentifierPart(buffer[tokenEnd])) {
                    tokenEnd++
                }
                currentType = TypeSpecTokenTypes.DECORATOR
                state = STATE_DEFAULT
            }
            current.isLetter() || current == '_' -> {
                tokenEnd = tokenStart + 1
                while (tokenEnd < endOffset && isIdentifierPart(buffer[tokenEnd])) {
                    tokenEnd++
                }
                val word = buffer.subSequence(tokenStart, tokenEnd).toString()
                currentType = if (word in TypeSpecKeywords.KEYWORDS) {
                    TypeSpecTokenTypes.KEYWORD
                } else {
                    TypeSpecTokenTypes.IDENTIFIER
                }
                state = STATE_DEFAULT
            }
            current.isDigit() -> {
                tokenEnd = tokenStart + 1
                while (tokenEnd < endOffset && (buffer[tokenEnd].isDigit() || buffer[tokenEnd] == '.')) {
                    tokenEnd++
                }
                currentType = TypeSpecTokenTypes.NUMBER
                state = STATE_DEFAULT
            }
            else -> {
                tokenEnd = tokenStart + 1
                currentType = TypeSpecTokenTypes.OPERATION_SIGN
                state = STATE_DEFAULT
            }
        }
    }

    private fun isIdentifierPart(ch: Char): Boolean =
        ch.isLetterOrDigit() || ch == '_'

    private fun isTripleQuoteAt(offset: Int): Boolean =
        offset + 2 < endOffset &&
            buffer[offset] == '"' &&
            buffer[offset + 1] == '"' &&
            buffer[offset + 2] == '"'

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

    private fun findStringEnd(start: Int): Int {
        var index = start
        while (index < endOffset) {
            when {
                buffer[index] == '\\' -> index = (index + 2).coerceAtMost(endOffset)
                buffer[index] == '"' -> return index + 1
                else -> index++
            }
        }
        return endOffset
    }

    private fun findTripleQuotedStringEnd(start: Int): Int {
        var index = start
        while (index + 2 < endOffset) {
            if (buffer[index] == '"' && buffer[index + 1] == '"' && buffer[index + 2] == '"') {
                return index + 3
            }
            index++
        }
        return endOffset
    }

    private fun endsWithBlockCommentClose(start: Int, end: Int): Boolean =
        end - start >= 2 && buffer[end - 2] == '*' && buffer[end - 1] == '/'

    private fun endsWithUnescapedQuote(start: Int, end: Int): Boolean {
        if (end <= start || buffer[end - 1] != '"') {
            return false
        }
        var backslashes = 0
        var index = end - 2
        while (index >= start && buffer[index] == '\\') {
            backslashes++
            index--
        }
        return backslashes % 2 == 0
    }

    private fun endsWithTripleQuote(start: Int, end: Int): Boolean =
        end - start >= 3 &&
            buffer[end - 3] == '"' &&
            buffer[end - 2] == '"' &&
            buffer[end - 1] == '"'

    companion object {
        const val STATE_DEFAULT = 0
        const val STATE_BLOCK_COMMENT = 1
        const val STATE_STRING = 2
        const val STATE_TRIPLE_STRING = 3
    }
}
