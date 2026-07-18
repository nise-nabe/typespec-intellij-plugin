package com.example.typespec.highlighting

import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeSpecLexerTest {
    @Test
    fun tokenizesKeywordsDecoratorsStringsCommentsAndNumbers() {
        val source = """
            |// line comment
            |/* block comment */
            |@service
            |namespace Demo {
            |  model Pet { id: 42, name: "Rex" }
            |}
        """.trimMargin()

        val tokens = tokenize(source)

        assertContainsType(tokens, TypeSpecTokenTypes.LINE_COMMENT)
        assertContainsType(tokens, TypeSpecTokenTypes.BLOCK_COMMENT)
        assertContainsType(tokens, TypeSpecTokenTypes.DECORATOR)
        assertContainsType(tokens, TypeSpecTokenTypes.KEYWORD)
        assertContainsType(tokens, TypeSpecTokenTypes.IDENTIFIER)
        assertContainsType(tokens, TypeSpecTokenTypes.NUMBER)
        assertContainsType(tokens, TypeSpecTokenTypes.STRING)
        assertContainsType(tokens, TypeSpecTokenTypes.OPERATION_SIGN)

        assertEquals(
            listOf(
                TypeSpecTokenTypes.LINE_COMMENT,
                TypeSpecTokenTypes.WHITESPACE,
                TypeSpecTokenTypes.BLOCK_COMMENT,
                TypeSpecTokenTypes.WHITESPACE,
                TypeSpecTokenTypes.DECORATOR,
                TypeSpecTokenTypes.WHITESPACE,
                TypeSpecTokenTypes.KEYWORD,
            ),
            tokens.take(7).map { it.type },
        )
        assertEquals("@service", tokens.first { it.type == TypeSpecTokenTypes.DECORATOR }.text)
        assertEquals("namespace", tokens.first { it.type == TypeSpecTokenTypes.KEYWORD }.text)
        assertEquals("42", tokens.first { it.type == TypeSpecTokenTypes.NUMBER }.text)
        assertEquals("\"Rex\"", tokens.first { it.type == TypeSpecTokenTypes.STRING }.text)
    }

    @Test
    fun tokenizesEscapedQuotesInsideStrings() {
        val tokens = tokenize("""name: "say \"hi\"" """)
        val stringToken = tokens.first { it.type == TypeSpecTokenTypes.STRING }
        assertEquals("\"say \\\"hi\\\"\"", stringToken.text)
    }

    private data class Token(val type: IElementType, val text: String)

    private fun tokenize(source: String): List<Token> {
        val lexer = TypeSpecLexer()
        lexer.start(source)
        val tokens = mutableListOf<Token>()
        while (lexer.tokenType != null) {
            tokens += Token(lexer.tokenType!!, lexer.tokenText)
            lexer.advance()
        }
        return tokens
    }

    private fun assertContainsType(tokens: List<Token>, type: IElementType) {
        assertEquals(
            true,
            tokens.any { it.type == type },
            "Expected token type $type in ${tokens.map { "${it.type}=${it.text}" }}",
        )
    }
}
