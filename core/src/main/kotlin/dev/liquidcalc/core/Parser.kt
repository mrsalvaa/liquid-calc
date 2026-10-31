// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal

/** Árbol de una expresión. */
internal sealed interface Node {
    data class Number(val value: BigDecimal) : Node
    data object Pi : Node
    data object E : Node
    data object Ans : Node
    data class Negate(val operand: Node) : Node
    data class Binary(val op: Op, val left: Node, val right: Node) : Node
    data class Call(val function: Function, val argument: Node) : Node
    data class Factorial(val operand: Node) : Node
    /** "x%": vale x/100, salvo a la derecha de + o −, donde es un porcentaje de lo de la izquierda. */
    data class Percent(val operand: Node) : Node

    enum class Op { ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER }
}

/**
 * Parser de descenso recursivo. De menor a mayor precedencia:
 *
 *     sum      = product (("+" | "−") product)*
 *     product  = signed (("×" | "÷") signed | signed-sin-operador)*   multiplicación implícita: 2π, 3(4)
 *     signed   = ("−" | "+") signed | power
 *     power    = postfix ("^" signed)?                                  a la derecha: 2^3^2 = 2^9
 *     postfix  = prefix ("!" | "%")*
 *     prefix   = ("√" | "∛") ("−" | "+")* prefix | primary
 *     primary  = número | π | e | ans | función "(" sum ")" | "(" sum ")"
 *
 * Así −2^2 = −4 (como en matemática) y 2^−1 = 0,5. Los paréntesis que faltan al final se cierran
 * solos, como en cualquier calculadora: "sin(30" vale lo mismo que "sin(30)".
 */
internal class Parser(private val tokens: List<Token>) {
    private var index = 0
    private val current get() = tokens[index]

    fun parse(): Node {
        if (current.kind == TokenKind.END) fail(CalcError.Syntax(0))
        val node = sum()
        if (current.kind != TokenKind.END) fail(CalcError.Syntax(current.position))
        return node
    }

    private fun sum(): Node {
        var left = product()
        while (true) {
            val op = when (current.kind) {
                TokenKind.PLUS -> Node.Op.ADD
                TokenKind.MINUS -> Node.Op.SUBTRACT
                else -> return left
            }
            index++
            left = Node.Binary(op, left, product())
        }
    }

    private fun product(): Node {
        var left = signed()
        while (true) {
            left = when (current.kind) {
                TokenKind.TIMES -> { index++; Node.Binary(Node.Op.MULTIPLY, left, signed()) }
                TokenKind.DIVIDE -> { index++; Node.Binary(Node.Op.DIVIDE, left, signed()) }
                in STARTS_OPERAND -> {
                    // "2 3" no es 6: dos números seguidos son un error, no una multiplicación.
                    if (current.kind == TokenKind.NUMBER && tokens[index - 1].kind == TokenKind.NUMBER) {
                        fail(CalcError.Syntax(current.position))
                    }
                    Node.Binary(Node.Op.MULTIPLY, left, signed())
                }
                else -> return left
            }
        }
    }

    private fun signed(): Node = when (current.kind) {
        TokenKind.MINUS -> { index++; Node.Negate(signed()) }
        TokenKind.PLUS -> { index++; signed() }
        else -> power()
    }

    private fun power(): Node {
        val base = postfix()
        if (current.kind != TokenKind.POWER) return base
        index++
        return Node.Binary(Node.Op.POWER, base, signed())
    }

    private fun postfix(): Node {
        var node = prefix()
        while (true) {
            node = when (current.kind) {
                TokenKind.FACTORIAL -> { index++; Node.Factorial(node) }
                TokenKind.PERCENT -> { index++; Node.Percent(node) }
                else -> return node
            }
        }
    }

    private fun prefix(): Node {
        val function = when (current.kind) {
            TokenKind.SQRT -> Function.SQRT
            TokenKind.CBRT -> Function.CBRT
            else -> return primary()
        }
        index++
        var negative = false
        while (current.kind == TokenKind.MINUS || current.kind == TokenKind.PLUS) {
            if (current.kind == TokenKind.MINUS) negative = !negative
            index++
        }
        val operand = prefix()
        return Node.Call(function, if (negative) Node.Negate(operand) else operand)
    }

    private fun primary(): Node {
        val token = current
        index++
        return when (token.kind) {
            TokenKind.NUMBER -> Node.Number(token.number!!)
            TokenKind.PI -> Node.Pi
            TokenKind.E -> Node.E
            TokenKind.ANS -> Node.Ans
            TokenKind.LPAREN -> group()
            TokenKind.FUNCTION -> {
                if (current.kind != TokenKind.LPAREN) fail(CalcError.Syntax(current.position))
                index++
                Node.Call(token.function!!, group())
            }
            else -> fail(CalcError.Syntax(token.position))
        }
    }

    /** Lo de adentro de un paréntesis ya abierto; el cierre puede faltar al final. */
    private fun group(): Node {
        if (current.kind == TokenKind.RPAREN || current.kind == TokenKind.END) fail(CalcError.Syntax(current.position))
        val inner = sum()
        when (current.kind) {
            TokenKind.RPAREN -> index++
            TokenKind.END -> Unit
            else -> fail(CalcError.Syntax(current.position))
        }
        return inner
    }

    private companion object {
        /** Tokens que empiezan un operando: si aparecen pegados a otro, se multiplican. */
        val STARTS_OPERAND = setOf(
            TokenKind.NUMBER, TokenKind.PI, TokenKind.E, TokenKind.ANS, TokenKind.LPAREN,
            TokenKind.FUNCTION, TokenKind.SQRT, TokenKind.CBRT,
        )
    }
}
