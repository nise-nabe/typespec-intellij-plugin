package com.example.typespec.highlighting

/**
 * In-memory lexer state. Packed to Int only at the [TypeSpecLexer.getState] /
 * [TypeSpecLexer.start] boundary for incremental highlighting.
 */
internal sealed interface LexState {
    data object Default : LexState

    data class BlockComment(
        val afterStar: Boolean,
        val continueState: LexState = Default,
    ) : LexState

    data class InString(
        val triple: Boolean,
        val pendingQuotes: Int = 0,
        val afterEscape: Boolean = false,
        val afterDollar: Boolean = false,
        val continueState: LexState = Default,
    ) : LexState

    /**
     * Inside a backtick-escaped identifier (`` `...` ``). [asDecorator] is true when the
     * open started after `@` / `@@` so resume keeps decorator highlighting.
     */
    data class EscapedIdent(
        val afterEscape: Boolean = false,
        val asDecorator: Boolean = false,
        val continueState: LexState = Default,
    ) : LexState

    data class Template(
        val depth: Int,
        val resume: LexState,
    ) : LexState
}
