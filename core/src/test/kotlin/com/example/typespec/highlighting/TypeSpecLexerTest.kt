package com.example.typespec.highlighting

import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun tokenizesTripleQuotedStringsWithEmbeddedQuotes() {
        val triple = "\"\"\""
        val source = """
            |alias S = $triple
            |  hello "world"
            |  $triple;
        """.trimMargin()

        val tokens = tokenize(source)
        val stringToken = tokens.first { it.type == TypeSpecTokenTypes.STRING }
        assertTrue(stringToken.text.startsWith(triple), stringToken.text)
        assertTrue(stringToken.text.endsWith(triple), stringToken.text)
        assertTrue(stringToken.text.contains("\"world\""), stringToken.text)
        // Embedded quotes stay inside the triple-quoted string token.
        assertTrue(tokens.none { it.type == TypeSpecTokenTypes.IDENTIFIER && it.text == "world" })
        assertEquals(";", tokens.last { it.type == TypeSpecTokenTypes.OPERATION_SIGN }.text)
    }

    @Test
    fun resumesBlockCommentAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("/* open", 0, 7, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.BLOCK_COMMENT, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_BLOCK_COMMENT, lexer.state)

        lexer.start(" still */ after", 0, 15, TypeSpecLexer.STATE_BLOCK_COMMENT)
        assertEquals(TypeSpecTokenTypes.BLOCK_COMMENT, lexer.tokenType)
        assertEquals(" still */", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.WHITESPACE, lexer.tokenType)
        lexer.advance()
        assertEquals(TypeSpecTokenTypes.IDENTIFIER, lexer.tokenType)
        assertEquals("after", lexer.tokenText)
    }

    @Test
    fun resumesTripleQuotedStringAcrossSegments() {
        val lexer = TypeSpecLexer()
        lexer.start("\"\"\"hello", 0, 8, TypeSpecLexer.STATE_DEFAULT)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(TypeSpecLexer.STATE_TRIPLE_STRING, lexer.state)

        lexer.start(" \"world\" \"\"\";", 0, 13, TypeSpecLexer.STATE_TRIPLE_STRING)
        assertEquals(TypeSpecTokenTypes.STRING, lexer.tokenType)
        assertEquals(" \"world\" \"\"\"", lexer.tokenText)
        assertEquals(TypeSpecLexer.STATE_DEFAULT, lexer.state)
        lexer.advance()
        assertEquals(";", lexer.tokenText)
    }

    @Test
    fun tokenizesAugmentDecoratorAndDottedMemberAccess() {
        val tokens = tokenize("@@TypeSpec.service model Pet { id: int32 }")
        assertEquals("@@TypeSpec", tokens.first { it.type == TypeSpecTokenTypes.DECORATOR }.text)
        assertEquals(".", tokens.first { it.type == TypeSpecTokenTypes.OPERATION_SIGN }.text)
        assertEquals("service", tokens.first { it.text == "service" }.text)
        assertEquals(TypeSpecTokenTypes.IDENTIFIER, tokens.first { it.text == "service" }.type)
        assertTrue("projection" in TypeSpecKeywords.KEYWORDS)
        assertTrue("function" !in TypeSpecKeywords.KEYWORDS)
        assertTrue("project" !in TypeSpecKeywords.KEYWORDS)
    }

    @Test
    fun highlightsCompilerKeywordsNotSpuriousOnes() {
        val tokens = tokenize("const x = valueof int32; projection P { }")
        assertEquals(
            listOf("const", "valueof", "projection"),
            tokens.filter { it.type == TypeSpecTokenTypes.KEYWORD }.map { it.text },
        )
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
