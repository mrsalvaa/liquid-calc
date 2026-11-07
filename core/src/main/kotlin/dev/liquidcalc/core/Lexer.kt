// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal

internal enum class TokenKind {
    NUMBER, PLUS, MINUS, TIMES, DIVIDE, POWER, LPAREN, RPAREN, FACTORIAL, PERCENT,
    SQRT, CBRT, FUNCTION, PI, E, ANS, SEMICOLON, END,
}

internal enum class Function { SIN, COS, TAN, ASIN, ACOS, ATAN, LN, LOG, SQRT, CBRT, EXP, ABS, ROOT }

internal data class Token(
    val kind: TokenKind,
    val position: Int,
    val number: BigDecimal? = null,
    val function: Function? = null,
)

/**
 * Convierte el texto en tokens. Acepta tanto los símbolos de la calculadora (× ÷ − √ π) como los
 * del teclado físico (* / - sqrt pi), y punto o coma como separador decimal.
 */
internal class Lexer(private val text: String) {
    fun tokens(): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' || c == ',' -> i = number(i, out)
                c.isLetter() && c != 'π' -> i = word(i, out)
                else -> {
                    val kind = when (c) {
                        '+' -> TokenKind.PLUS
                        '-', '−', '–' -> TokenKind.MINUS
                        '*', '×', '·' -> TokenKind.TIMES
                        '/', '÷' -> TokenKind.DIVIDE
                        '^' -> TokenKind.POWER
                        '(' -> TokenKind.LPAREN
                        ')' -> TokenKind.RPAREN
                        '!' -> TokenKind.FACTORIAL
                        '%' -> TokenKind.PERCENT
                        ';' -> TokenKind.SEMICOLON
                        '√' -> TokenKind.SQRT
                        '∛' -> TokenKind.CBRT
                        'π' -> TokenKind.PI
                        else -> fail(CalcError.Syntax(i))
                    }
                    out += Token(kind, i)
                    i++
                }
            }
        }
        out += Token(TokenKind.END, text.length)
        return out
    }

    private fun number(start: Int, out: MutableList<Token>): Int {
        var i = start
        var separators = 0
        var digits = 0
        while (i < text.length && (text[i].isDigit() || text[i] == '.' || text[i] == ',')) {
            if (text[i].isDigit()) digits++ else separators++
            i++
        }
        if (digits == 0 || separators > 1) fail(CalcError.Syntax(start))
        val literal = text.substring(start, i).replace(',', '.')
        out += Token(TokenKind.NUMBER, start, number = BigDecimal(literal))
        return i
    }

    private fun word(start: Int, out: MutableList<Token>): Int {
        var i = start
        while (i < text.length && text[i].isLetter() && text[i] != 'π') i++
        val token = when (text.substring(start, i).lowercase()) {
            "pi" -> Token(TokenKind.PI, start)
            "e" -> Token(TokenKind.E, start)
            "ans" -> Token(TokenKind.ANS, start)
            "sqrt" -> Token(TokenKind.SQRT, start)
            "cbrt" -> Token(TokenKind.CBRT, start)
            else -> {
                val f = FUNCTIONS[text.substring(start, i).lowercase()] ?: fail(CalcError.Syntax(start))
                Token(TokenKind.FUNCTION, start, function = f)
            }
        }
        out += token
        return i
    }

    private companion object {
        val FUNCTIONS = mapOf(
            "sin" to Function.SIN, "cos" to Function.COS, "tan" to Function.TAN,
            "asin" to Function.ASIN, "acos" to Function.ACOS, "atan" to Function.ATAN,
            "ln" to Function.LN, "log" to Function.LOG, "exp" to Function.EXP, "abs" to Function.ABS,
            "root" to Function.ROOT,
        )
    }
}
