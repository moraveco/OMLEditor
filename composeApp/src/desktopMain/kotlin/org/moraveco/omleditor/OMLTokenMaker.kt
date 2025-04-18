package org.moraveco.omleditor

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenMap
import javax.swing.text.Segment


class OMLTokenMaker : AbstractTokenMaker() {

    private val keywords = setOf("funk", "vrat", "pro", "men")
    private val types = setOf("celocislo", "pole")
    private val functions = setOf("vytiskni", "vytisknird")

    override fun getTokenList(text: Segment, startTokenType: Int, startOffset: Int): Token {
        resetTokenList()

        val array = text.array
        val offset = text.offset
        val count = text.count
        val end = offset + count
        val newStartOffset = startOffset - offset

        var currentTokenStart = offset
        var currentTokenType = startTokenType

        var i = offset
        while (i < end) {
            val c = array[i]

            when (currentTokenType) {
                Token.NULL -> {
                    currentTokenStart = i
                    currentTokenType = when (c) {
                        ' ', '\t' -> Token.WHITESPACE
                        '"' -> Token.LITERAL_STRING_DOUBLE_QUOTE
                        '#' -> Token.COMMENT_EOL
                        else -> when {
                            RSyntaxUtilities.isDigit(c) -> Token.LITERAL_NUMBER_DECIMAL_INT
                            isWordChar(c) -> Token.IDENTIFIER
                            else -> {
                                addToken(text, i, i, Token.IDENTIFIER, newStartOffset + i)
                                currentTokenType = Token.NULL
                                i++
                                continue
                            }
                        }
                    }
                }

                Token.WHITESPACE -> {
                    when (c) {
                        ' ', '\t' -> {}
                        '"' -> {
                            addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart)
                            currentTokenStart = i
                            currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE
                        }
                        '#' -> {
                            addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart)
                            currentTokenStart = i
                            currentTokenType = Token.COMMENT_EOL
                        }
                        else -> {
                            addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart)
                            currentTokenStart = i
                            currentTokenType = when {
                                RSyntaxUtilities.isDigit(c) -> Token.LITERAL_NUMBER_DECIMAL_INT
                                isWordChar(c) -> Token.IDENTIFIER
                                else -> {
                                    addToken(text, i, i, Token.IDENTIFIER, newStartOffset + i)
                                    currentTokenType = Token.NULL
                                    i++
                                    continue
                                }
                            }
                        }
                    }
                }

                Token.IDENTIFIER -> {
                    if (!isWordChar(c)) {
                        val type = wordsToHighlight.get(array, currentTokenStart, i - 1)
                        addToken(text, currentTokenStart, i - 1, type.takeIf { it != -1 } ?: Token.IDENTIFIER, newStartOffset + currentTokenStart)
                        currentTokenStart = i
                        currentTokenType = Token.NULL
                        i--
                    }
                }

                Token.LITERAL_NUMBER_DECIMAL_INT -> {
                    when {
                        RSyntaxUtilities.isDigit(c) -> {}
                        else -> {
                            addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset + currentTokenStart)
                            currentTokenStart = i
                            currentTokenType = Token.NULL
                            i--
                        }
                    }
                }

                Token.COMMENT_EOL -> {
                    i = end - 1
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart)
                    currentTokenType = Token.NULL
                }

                Token.LITERAL_STRING_DOUBLE_QUOTE -> {
                    if (c == '"') {
                        addToken(text, currentTokenStart, i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset + currentTokenStart)
                        currentTokenType = Token.NULL
                    }
                }
            }

            i++
        }

        // Final token
        when (currentTokenType) {
            Token.LITERAL_STRING_DOUBLE_QUOTE -> addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart)
            Token.NULL -> addNullToken()
            else -> {
                val type = if (currentTokenType == Token.IDENTIFIER) wordsToHighlight.get(array, currentTokenStart, end - 1) else -1
                addToken(text, currentTokenStart, end - 1, type.takeIf { it != -1 } ?: currentTokenType, newStartOffset + currentTokenStart)
                addNullToken()
            }
        }

        return firstToken
    }


    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || c == '/'
    }


    override fun addToken(segment: Segment, start: Int, end: Int, tokenType: Int, startOffset: Int) {
        // This assumes all keywords, etc. were parsed as "identifiers."
        var actualTokenType = tokenType
        if (tokenType == Token.IDENTIFIER) {
            val value = wordsToHighlight.get(segment, start, end)
            if (value != -1) {
                actualTokenType = value
            }
        }
        super.addToken(segment, start, end, actualTokenType, startOffset)
    }








    override fun getWordsToHighlight(): TokenMap {
        val tokenMap = TokenMap()

        // Keywords
        tokenMap.put("funk", Token.RESERVED_WORD)
        tokenMap.put("vrat", Token.RESERVED_WORD)
        tokenMap.put("pro", Token.RESERVED_WORD)
        tokenMap.put("men", Token.RESERVED_WORD)
        tokenMap.put("pokud", Token.RESERVED_WORD)
        tokenMap.put("jinak", Token.RESERVED_WORD)

        // Data types
        tokenMap.put("celocislo", Token.DATA_TYPE)
        tokenMap.put("pole", Token.DATA_TYPE)
        tokenMap.put("txtret", Token.DATA_TYPE)
        tokenMap.put("bool", Token.DATA_TYPE)
        tokenMap.put("znak", Token.DATA_TYPE)
        tokenMap.put("descislo", Token.DATA_TYPE)

        // Functions
        tokenMap.put("vytiskni", Token.FUNCTION)
        tokenMap.put("vytisknird", Token.FUNCTION)

        // Optional keywords

        return tokenMap
    }
}