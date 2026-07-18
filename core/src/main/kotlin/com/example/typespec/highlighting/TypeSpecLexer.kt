package com.example.typespec.highlighting

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Highlighting lexer for TypeSpec. Tracks a packed integer state so incremental
 * re-lex can resume inside block comments and (triple-quoted) strings, including
 * when a delimiter or escape is split across a buffer boundary.
 *
 * Double-quoted (non-triple) strings end at a newline, matching the compiler.
 * `${` in a string opens a brace-tracked interpolation so nested quotes inside
 * the expression highlight correctly; the string resumes after the matching `}`.
 * String/comment resume states pack a continue target so nested templates and
 * strings opened inside interpolations return to the correct outer state.
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

        when {
            isTemplateState(state) ->
                advanceTemplate(templateDepthOrZero(state), resumeStringState(state))
            else -> when (val mode = modeOf(state)) {
                STATE_BLOCK_COMMENT -> emit(
                    TypeSpecTokenTypes.BLOCK_COMMENT,
                    scanBlockComment(tokenStart, afterStar = false, continueState = continueOf(state)),
                )
                STATE_BLOCK_COMMENT_AFTER_STAR -> emit(
                    TypeSpecTokenTypes.BLOCK_COMMENT,
                    scanBlockComment(tokenStart, afterStar = true, continueState = continueOf(state)),
                )
                STATE_STRING -> emit(
                    TypeSpecTokenTypes.STRING,
                    scanString(tokenStart, afterEscape = false, continueState = continueOf(state)),
                )
                STATE_STRING_AFTER_ESCAPE -> emit(
                    TypeSpecTokenTypes.STRING,
                    scanString(tokenStart, afterEscape = true, continueState = continueOf(state)),
                )
                STATE_STRING_AFTER_DOLLAR ->
                    advanceAfterDollar(resumeStringMode = STATE_STRING, continueState = continueOf(state))
                STATE_TRIPLE_STRING -> emit(
                    TypeSpecTokenTypes.STRING,
                    scanTripleQuotedString(tokenStart, pendingQuotes = 0, afterEscape = false, continueState = continueOf(state)),
                )
                STATE_TRIPLE_STRING_QUOTE1 -> emit(
                    TypeSpecTokenTypes.STRING,
                    scanTripleQuotedString(tokenStart, pendingQuotes = 1, afterEscape = false, continueState = continueOf(state)),
                )
                STATE_TRIPLE_STRING_QUOTE2 -> emit(
                    TypeSpecTokenTypes.STRING,
                    scanTripleQuotedString(tokenStart, pendingQuotes = 2, afterEscape = false, continueState = continueOf(state)),
                )
                STATE_TRIPLE_STRING_AFTER_ESCAPE -> emit(
                    TypeSpecTokenTypes.STRING,
                    scanTripleQuotedString(tokenStart, pendingQuotes = 0, afterEscape = true, continueState = continueOf(state)),
                )
                STATE_TRIPLE_STRING_AFTER_DOLLAR ->
                    advanceAfterDollar(resumeStringMode = STATE_TRIPLE_STRING, continueState = continueOf(state))
                STATE_DEFAULT -> advanceDefault()
                else -> {
                    // Unknown resume state: recover rather than mis-color forever.
                    state = STATE_DEFAULT
                    advanceDefault()
                }
            }
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun advanceDefault(continueState: Int = STATE_DEFAULT) {
        val current = buffer[tokenStart]
        when {
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '/' -> {
                emit(TypeSpecTokenTypes.LINE_COMMENT, Scan(findLineEnd(tokenStart + 2), continueState))
            }
            current == '/' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == '*' -> {
                emit(
                    TypeSpecTokenTypes.BLOCK_COMMENT,
                    scanBlockComment(tokenStart + 2, afterStar = false, continueState = continueState),
                )
            }
            current == '"' && isTripleQuoteAt(tokenStart) -> {
                emit(
                    TypeSpecTokenTypes.STRING,
                    scanTripleQuotedString(
                        tokenStart + 3,
                        pendingQuotes = 0,
                        afterEscape = false,
                        continueState = continueState,
                    ),
                )
            }
            current == '"' -> {
                emit(
                    TypeSpecTokenTypes.STRING,
                    scanString(tokenStart + 1, afterEscape = false, continueState = continueState),
                )
            }
            current.isWhitespace() -> {
                var end = tokenStart + 1
                while (end < endOffset && buffer[end].isWhitespace()) {
                    end++
                }
                emit(TypeSpecTokenTypes.WHITESPACE, Scan(end, continueState))
            }
            current == '@' -> {
                // Highlight `@name` / `@@name` as one decorator token for editor UX.
                var end = tokenStart + 1
                if (end < endOffset && buffer[end] == '@') {
                    end++
                }
                while (end < endOffset && isIdentifierPart(buffer[end])) {
                    end++
                }
                emit(TypeSpecTokenTypes.DECORATOR, Scan(end, continueState))
            }
            current == '$' || current.isLetter() || current == '_' -> {
                var end = tokenStart + 1
                while (end < endOffset && isIdentifierPart(buffer[end])) {
                    end++
                }
                val word = buffer.subSequence(tokenStart, end).toString()
                val type = if (word in TypeSpecKeywords.KEYWORDS) {
                    TypeSpecTokenTypes.KEYWORD
                } else {
                    TypeSpecTokenTypes.IDENTIFIER
                }
                emit(type, Scan(end, continueState))
            }
            current.isDigit() -> {
                var end = tokenStart + 1
                while (end < endOffset && (buffer[end].isDigit() || buffer[end] == '.')) {
                    end++
                }
                emit(TypeSpecTokenTypes.NUMBER, Scan(end, continueState))
            }
            else -> emit(TypeSpecTokenTypes.OPERATION_SIGN, Scan(tokenStart + 1, continueState))
        }
    }

    private fun advanceAfterDollar(resumeStringMode: Int, continueState: Int) {
        if (buffer[tokenStart] == '{') {
            emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(tokenStart + 1, templateState(1, withContinue(resumeStringMode, continueState))),
            )
        } else if (resumeStringMode == STATE_TRIPLE_STRING) {
            emit(
                TypeSpecTokenTypes.STRING,
                scanTripleQuotedString(
                    tokenStart,
                    pendingQuotes = 0,
                    afterEscape = false,
                    continueState = continueState,
                ),
            )
        } else {
            emit(
                TypeSpecTokenTypes.STRING,
                scanString(tokenStart, afterEscape = false, continueState = continueState),
            )
        }
    }

    private fun advanceTemplate(depth: Int, resumeStringState: Int) {
        val current = buffer[tokenStart]
        when (current) {
            '{' -> emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(tokenStart + 1, templateState(depth + 1, resumeStringState)),
            )
            '}' -> {
                val next = if (depth <= 1) {
                    resumeStringState
                } else {
                    templateState(depth - 1, resumeStringState)
                }
                emit(TypeSpecTokenTypes.OPERATION_SIGN, Scan(tokenStart + 1, next))
            }
            else -> advanceDefault(continueState = templateState(depth, resumeStringState))
        }
    }

    private fun emit(type: IElementType, scan: Scan) {
        // LexerBase requires positive-length tokens. A resume state can land on a
        // terminator that must be re-lexed in the continue state (e.g. `$` split
        // across buffers, then a newline ending a double-quoted string).
        if (scan.end == tokenStart) {
            state = scan.nextState
            advance()
            return
        }
        tokenEnd = scan.end
        currentType = type
        state = scan.nextState
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

    private fun scanBlockComment(start: Int, afterStar: Boolean, continueState: Int = STATE_DEFAULT): Scan {
        var index = start
        var sawStar = afterStar
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                sawStar && ch == '/' -> return Scan(index + 1, continueState)
                ch == '*' -> {
                    sawStar = true
                    index++
                }
                else -> {
                    sawStar = false
                    index++
                }
            }
        }
        val mode = if (sawStar) STATE_BLOCK_COMMENT_AFTER_STAR else STATE_BLOCK_COMMENT
        return Scan(endOffset, withContinue(mode, continueState))
    }

    private fun scanString(
        start: Int,
        afterEscape: Boolean,
        continueState: Int = STATE_DEFAULT,
    ): Scan {
        var index = start
        var escape = afterEscape
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                escape -> {
                    escape = false
                    index++
                }
                ch == '\\' -> {
                    escape = true
                    index++
                }
                ch == '$' -> {
                    val dollar = scanDollarInString(
                        index,
                        stringMode = STATE_STRING,
                        afterDollarMode = STATE_STRING_AFTER_DOLLAR,
                        continueState = continueState,
                    )
                    if (dollar != null) return dollar
                    index++
                }
                ch == '"' -> return Scan(index + 1, continueState)
                ch == '\n' || ch == '\r' -> return Scan(index, continueState)
                else -> index++
            }
        }
        val mode = if (escape) STATE_STRING_AFTER_ESCAPE else STATE_STRING
        return Scan(endOffset, withContinue(mode, continueState))
    }

    private fun scanTripleQuotedString(
        start: Int,
        pendingQuotes: Int,
        afterEscape: Boolean,
        continueState: Int = STATE_DEFAULT,
    ): Scan {
        var index = start
        var quotes = pendingQuotes
        var escape = afterEscape
        while (index < endOffset) {
            val ch = buffer[index]
            when {
                escape -> {
                    escape = false
                    quotes = 0
                    index++
                }
                ch == '\\' -> {
                    escape = true
                    quotes = 0
                    index++
                }
                ch == '$' -> {
                    quotes = 0
                    val dollar = scanDollarInString(
                        index,
                        stringMode = STATE_TRIPLE_STRING,
                        afterDollarMode = STATE_TRIPLE_STRING_AFTER_DOLLAR,
                        continueState = continueState,
                    )
                    if (dollar != null) return dollar
                    index++
                }
                ch == '"' -> {
                    quotes++
                    index++
                    if (quotes == 3) {
                        return Scan(index, continueState)
                    }
                }
                else -> {
                    quotes = 0
                    index++
                }
            }
        }
        val mode = when {
            escape -> STATE_TRIPLE_STRING_AFTER_ESCAPE
            quotes == 1 -> STATE_TRIPLE_STRING_QUOTE1
            quotes == 2 -> STATE_TRIPLE_STRING_QUOTE2
            else -> STATE_TRIPLE_STRING
        }
        return Scan(endOffset, withContinue(mode, continueState))
    }

    /**
     * Handles `$` / `${` / `$` at buffer end inside a string. Returns null when `$`
     * is ordinary string content (caller should advance past it).
     */
    private fun scanDollarInString(
        index: Int,
        stringMode: Int,
        afterDollarMode: Int,
        continueState: Int,
    ): Scan? {
        if (index + 1 < endOffset && buffer[index + 1] == '{') {
            return Scan(index + 2, templateState(1, withContinue(stringMode, continueState)))
        }
        if (index + 1 >= endOffset) {
            return Scan(endOffset, withContinue(afterDollarMode, continueState))
        }
        return null
    }

    private data class Scan(val end: Int, val nextState: Int)

    companion object {
        const val STATE_DEFAULT = 0
        const val STATE_BLOCK_COMMENT = 1
        const val STATE_BLOCK_COMMENT_AFTER_STAR = 2
        const val STATE_STRING = 3
        const val STATE_STRING_AFTER_ESCAPE = 4
        const val STATE_TRIPLE_STRING = 5
        const val STATE_TRIPLE_STRING_QUOTE1 = 6
        const val STATE_TRIPLE_STRING_QUOTE2 = 7
        const val STATE_STRING_AFTER_DOLLAR = 8
        const val STATE_TRIPLE_STRING_AFTER_DOLLAR = 9
        const val STATE_TRIPLE_STRING_AFTER_ESCAPE = 10

        /**
         * Packed layout:
         * - bits 0–7: mode (or template brace depth when [TEMPLATE_BIT] is set)
         * - bit 8: template interpolation marker
         * - bits 9+: continue / resume payload (nested packed state)
         */
        private const val MODE_MASK = 0xFF
        private const val TEMPLATE_BIT = 1 shl 8
        private const val PAYLOAD_SHIFT = 9
        private const val TEMPLATE_MAX_DEPTH = 8

        fun modeOf(state: Int): Int = state and MODE_MASK

        fun continueOf(state: Int): Int {
            val cont = state ushr PAYLOAD_SHIFT
            return if (cont == 0) STATE_DEFAULT else cont
        }

        /**
         * Pack [mode] with a continue target so unfinished scans inside template
         * expressions resume to that template (or nested continue) when closed.
         * Deep continue chains that exceed the payload width are flattened.
         */
        fun withContinue(mode: Int, continueState: Int): Int {
            if (continueState == STATE_DEFAULT) return mode and MODE_MASK
            return (mode and MODE_MASK) or (packContinue(continueState) shl PAYLOAD_SHIFT)
        }

        fun isTemplateState(state: Int): Boolean = (state and TEMPLATE_BIT) != 0

        fun templateState(depth: Int, resumeStringState: Int): Int {
            val clamped = depth.coerceIn(1, TEMPLATE_MAX_DEPTH)
            return TEMPLATE_BIT or clamped or (packContinue(resumeStringState) shl PAYLOAD_SHIFT)
        }

        fun templateDepthOrZero(state: Int): Int =
            if (isTemplateState(state)) state and MODE_MASK else 0

        fun resumeStringState(templateState: Int): Int =
            if (isTemplateState(templateState)) continueOf(templateState) else STATE_STRING

        private fun packContinue(continueState: Int): Int {
            if (continueState == STATE_DEFAULT) return 0
            // Payload lives in bits 9–31 of the parent (23 bits).
            if (continueState ushr (Int.SIZE_BITS - PAYLOAD_SHIFT) == 0) return continueState
            val depth = templateDepthOrZero(continueState)
            if (depth > 0) {
                val resumeMode = modeOf(resumeStringState(continueState)).let { mode ->
                    when (mode) {
                        STATE_TRIPLE_STRING, STATE_TRIPLE_STRING_QUOTE1, STATE_TRIPLE_STRING_QUOTE2,
                        STATE_TRIPLE_STRING_AFTER_DOLLAR, STATE_TRIPLE_STRING_AFTER_ESCAPE,
                        -> STATE_TRIPLE_STRING
                        else -> STATE_STRING
                    }
                }
                return templateState(depth, resumeMode)
            }
            return modeOf(continueState)
        }
    }
}
