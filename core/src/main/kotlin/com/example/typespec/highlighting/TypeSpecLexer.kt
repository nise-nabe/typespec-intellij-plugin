package com.example.typespec.highlighting

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Highlighting lexer for TypeSpec. Scanning uses a typed [LexState] tree so nested
 * string templates keep full fidelity inside a buffer. [getState]/[start] pack that
 * tree into an Int for IntelliJ incremental re-lex (with a finite continue capacity).
 *
 * Double-quoted (non-triple) strings end at a newline, matching the compiler.
 * `${` in a string opens a brace-tracked interpolation so nested quotes inside
 * the expression highlight correctly; the string resumes after the matching `}`.
 * Backtick-escaped identifiers (`` `enum` ``) tokenize as identifiers, not keywords.
 */
internal class TypeSpecLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentType: IElementType? = null
    private var lexState: LexState = LexState.Default

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        lexState = LexStatePacking.unpack(initialState)
        advance()
    }

    override fun getState(): Int = LexStatePacking.pack(lexState)

    override fun getTokenType(): IElementType? = currentType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            currentType = null
            return
        }

        when (val state = lexState) {
            LexState.Default -> advanceDefault(LexState.Default)
            is LexState.BlockComment -> emit(
                TypeSpecTokenTypes.BLOCK_COMMENT,
                scanBlockComment(tokenStart, afterStar = state.afterStar, continueState = state.continueState),
            )
            is LexState.InString -> {
                if (state.afterDollar) {
                    advanceAfterDollar(state)
                } else {
                    emit(
                        TypeSpecTokenTypes.STRING,
                        scanQuoted(
                            tokenStart,
                            triple = state.triple,
                            pendingQuotes = state.pendingQuotes,
                            afterEscape = state.afterEscape,
                            continueState = state.continueState,
                        ),
                    )
                }
            }
            is LexState.EscapedIdent -> emit(
                if (state.asDecorator) TypeSpecTokenTypes.DECORATOR else TypeSpecTokenTypes.IDENTIFIER,
                scanEscapedIdent(
                    tokenStart,
                    afterEscape = state.afterEscape,
                    asDecorator = state.asDecorator,
                    continueState = state.continueState,
                ),
            )
            is LexState.Template -> advanceTemplate(state)
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun advanceDefault(continueState: LexState) {
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
                    scanQuoted(
                        tokenStart + 3,
                        triple = true,
                        pendingQuotes = 0,
                        afterEscape = false,
                        continueState = continueState,
                    ),
                )
            }
            current == '"' -> {
                emit(
                    TypeSpecTokenTypes.STRING,
                    scanQuoted(
                        tokenStart + 1,
                        triple = false,
                        pendingQuotes = 0,
                        afterEscape = false,
                        continueState = continueState,
                    ),
                )
            }
            current == '`' -> {
                emit(
                    TypeSpecTokenTypes.IDENTIFIER,
                    scanEscapedIdent(
                        tokenStart + 1,
                        afterEscape = false,
                        asDecorator = false,
                        continueState = continueState,
                    ),
                )
            }
            current.isWhitespace() -> {
                var end = tokenStart + 1
                while (end < endOffset && buffer[end].isWhitespace()) {
                    end++
                }
                emit(TypeSpecTokenTypes.WHITESPACE, Scan(end, continueState))
            }
            current == '@' -> advanceDecorator(continueState)
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

    private fun advanceDecorator(continueState: LexState) {
        // Highlight `@name` / `@@name` / `@`name`` as one decorator token for editor UX.
        var end = tokenStart + 1
        if (end < endOffset && buffer[end] == '@') {
            end++
        }
        if (end < endOffset && buffer[end] == '`') {
            emit(
                TypeSpecTokenTypes.DECORATOR,
                scanEscapedIdent(
                    end + 1,
                    afterEscape = false,
                    asDecorator = true,
                    continueState = continueState,
                ),
            )
            return
        }
        while (end < endOffset && isIdentifierPart(buffer[end])) {
            end++
        }
        emit(TypeSpecTokenTypes.DECORATOR, Scan(end, continueState))
    }

    private fun advanceAfterDollar(state: LexState.InString) {
        if (buffer[tokenStart] == '{') {
            emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(
                    tokenStart + 1,
                    LexState.Template(
                        depth = 1,
                        resume = state.copy(afterDollar = false),
                    ),
                ),
            )
            return
        }
        emit(
            TypeSpecTokenTypes.STRING,
            scanQuoted(
                tokenStart,
                triple = state.triple,
                pendingQuotes = 0,
                afterEscape = false,
                continueState = state.continueState,
            ),
        )
    }

    private fun advanceTemplate(state: LexState.Template) {
        when (buffer[tokenStart]) {
            '{' -> emit(
                TypeSpecTokenTypes.OPERATION_SIGN,
                Scan(tokenStart + 1, LexState.Template(state.depth + 1, state.resume)),
            )
            '}' -> {
                val next = if (state.depth <= 1) {
                    state.resume
                } else {
                    LexState.Template(state.depth - 1, state.resume)
                }
                emit(TypeSpecTokenTypes.OPERATION_SIGN, Scan(tokenStart + 1, next))
            }
            else -> advanceDefault(continueState = state)
        }
    }

    private fun emit(type: IElementType, scan: Scan) {
        // LexerBase requires positive-length tokens. A resume state can land on a
        // terminator that must be re-lexed in the continue state (e.g. `$` split
        // across buffers, then a newline ending a double-quoted string).
        if (scan.end == tokenStart) {
            lexState = scan.nextState
            advance()
            return
        }
        tokenEnd = scan.end
        currentType = type
        lexState = scan.nextState
    }

    private fun isIdentifierPart(ch: Char): Boolean =
        ch.isLetterOrDigit() || ch == '_' || ch == '$'

    private fun isTripleQuoteAt(offset: Int): Boolean =
        offset + 2 < endOffset &&
            buffer[offset] == '"' &&
            buffer[offset + 1] == '"' &&
            buffer[offset + 2] == '"'

    private fun findLineEnd(start: Int): Int {
        var index = start
        while (index < endOffset && buffer[index] != '\n' && buffer[index] != '\r') {
            index++
        }
        return index
    }

    private fun scanBlockComment(start: Int, afterStar: Boolean, continueState: LexState): Scan {
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
        return Scan(endOffset, LexState.BlockComment(afterStar = sawStar, continueState = continueState))
    }

    private fun scanQuoted(
        start: Int,
        triple: Boolean,
        pendingQuotes: Int,
        afterEscape: Boolean,
        continueState: LexState,
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
                    val dollar = scanDollarInString(index, triple = triple, continueState = continueState)
                    if (dollar != null) return dollar
                    index++
                }
                ch == '"' -> {
                    if (!triple) return Scan(index + 1, continueState)
                    quotes++
                    index++
                    if (quotes == 3) return Scan(index, continueState)
                }
                !triple && (ch == '\n' || ch == '\r') -> return Scan(index, continueState)
                else -> {
                    quotes = 0
                    index++
                }
            }
        }
        return Scan(
            endOffset,
            LexState.InString(
                triple = triple,
                pendingQuotes = if (triple) quotes else 0,
                afterEscape = escape,
                afterDollar = false,
                continueState = continueState,
            ),
        )
    }

    /**
     * Handles `$` / `${` / `$` at buffer end inside a string. Returns null when `$`
     * is ordinary string content (caller should advance past it).
     */
    private fun scanDollarInString(index: Int, triple: Boolean, continueState: LexState): Scan? {
        if (index + 1 < endOffset && buffer[index + 1] == '{') {
            return Scan(
                index + 2,
                LexState.Template(
                    depth = 1,
                    resume = LexState.InString(triple = triple, continueState = continueState),
                ),
            )
        }
        if (index + 1 >= endOffset) {
            return Scan(
                endOffset,
                LexState.InString(triple = triple, afterDollar = true, continueState = continueState),
            )
        }
        return null
    }

    /** Compiler pattern: `` `(?:[^`\\]|\\.)*` `` */
    private fun scanEscapedIdent(
        start: Int,
        afterEscape: Boolean,
        asDecorator: Boolean,
        continueState: LexState,
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
                ch == '`' -> return Scan(index + 1, continueState)
                else -> index++
            }
        }
        return Scan(
            endOffset,
            LexState.EscapedIdent(
                afterEscape = escape,
                asDecorator = asDecorator,
                continueState = continueState,
            ),
        )
    }

    private data class Scan(val end: Int, val nextState: LexState)

    companion object {
        /** Packed Int for [LexState.Default] — used by tests and incremental resume. */
        const val STATE_DEFAULT = 0
        val STATE_BLOCK_COMMENT: Int = LexStatePacking.pack(LexState.BlockComment(afterStar = false))
        val STATE_BLOCK_COMMENT_AFTER_STAR: Int = LexStatePacking.pack(LexState.BlockComment(afterStar = true))
        val STATE_STRING: Int = LexStatePacking.pack(LexState.InString(triple = false))
        val STATE_STRING_AFTER_ESCAPE: Int =
            LexStatePacking.pack(LexState.InString(triple = false, afterEscape = true))
        val STATE_TRIPLE_STRING: Int = LexStatePacking.pack(LexState.InString(triple = true))
        val STATE_TRIPLE_STRING_QUOTE1: Int =
            LexStatePacking.pack(LexState.InString(triple = true, pendingQuotes = 1))
        val STATE_TRIPLE_STRING_QUOTE2: Int =
            LexStatePacking.pack(LexState.InString(triple = true, pendingQuotes = 2))
        val STATE_STRING_AFTER_DOLLAR: Int =
            LexStatePacking.pack(LexState.InString(triple = false, afterDollar = true))
        val STATE_TRIPLE_STRING_AFTER_DOLLAR: Int =
            LexStatePacking.pack(LexState.InString(triple = true, afterDollar = true))
        val STATE_TRIPLE_STRING_AFTER_ESCAPE: Int =
            LexStatePacking.pack(LexState.InString(triple = true, afterEscape = true))
        val STATE_ESCAPED_IDENT: Int = LexStatePacking.pack(LexState.EscapedIdent())
        val STATE_ESCAPED_IDENT_AFTER_ESCAPE: Int =
            LexStatePacking.pack(LexState.EscapedIdent(afterEscape = true))
        val STATE_DECORATOR_ESCAPED_IDENT: Int =
            LexStatePacking.pack(LexState.EscapedIdent(asDecorator = true))
        val STATE_DECORATOR_ESCAPED_IDENT_AFTER_ESCAPE: Int =
            LexStatePacking.pack(LexState.EscapedIdent(afterEscape = true, asDecorator = true))

        fun isTemplateState(state: Int): Boolean = LexStatePacking.unpack(state) is LexState.Template

        fun templateState(depth: Int, resumeStringState: Int): Int =
            LexStatePacking.pack(
                LexState.Template(depth, LexStatePacking.unpack(resumeStringState)),
            )
    }
}
