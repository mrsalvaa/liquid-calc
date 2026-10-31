// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal

/**
 * Recorre el árbol. Sumas, restas y multiplicaciones son exactas (solo se redondea a 50 cifras
 * para que no crezcan sin límite); el resto trabaja con [BigMath.MC].
 */
internal class Evaluator(private val angle: AngleUnit, private val ans: BigDecimal) {
    private val hundred = BigDecimal(100)

    fun eval(node: Node): BigDecimal = BigMath.checked(
        when (node) {
            is Node.Number -> node.value
            Node.Pi -> BigMath.PI.round(BigMath.MC)
            Node.E -> BigMath.E
            Node.Ans -> ans
            is Node.Negate -> eval(node.operand).negate()
            is Node.Percent -> eval(node.operand).divide(hundred)
            is Node.Factorial -> BigMath.factorial(eval(node.operand))
            is Node.Call -> call(node.function, eval(node.argument))
            is Node.Binary -> binary(node)
        },
    )

    private fun binary(node: Node.Binary): BigDecimal {
        val left = eval(node.left)
        val right = node.right
        // Porcentaje de calculadora: 200 + 10% = 220 y 200 − 10% = 180 (el % es de lo de la izquierda).
        if (right is Node.Percent && (node.op == Node.Op.ADD || node.op == Node.Op.SUBTRACT)) {
            val share = left.multiply(eval(right.operand), BigMath.MC).divide(hundred)
            return if (node.op == Node.Op.ADD) left.add(share, BigMath.MC) else left.subtract(share, BigMath.MC)
        }
        val r = eval(right)
        return when (node.op) {
            Node.Op.ADD -> left.add(r, BigMath.MC)
            Node.Op.SUBTRACT -> left.subtract(r, BigMath.MC)
            Node.Op.MULTIPLY -> left.multiply(r, BigMath.MC)
            Node.Op.DIVIDE -> {
                if (r.signum() == 0) fail(CalcError.DivisionByZero)
                left.divide(r, BigMath.MC)
            }
            Node.Op.POWER -> BigMath.pow(left, r)
        }
    }

    private fun call(function: Function, x: BigDecimal): BigDecimal {
        val degrees = angle == AngleUnit.DEGREES
        return when (function) {
            Function.SIN -> if (degrees) BigMath.sinDegrees(x) else BigMath.sin(x)
            Function.COS -> if (degrees) BigMath.cosDegrees(x) else BigMath.cos(x)
            Function.TAN -> if (degrees) BigMath.tanDegrees(x) else BigMath.tan(x)
            Function.ASIN -> BigMath.asin(x).let { if (degrees) BigMath.toDegrees(it) else it }
            Function.ACOS -> BigMath.acos(x).let { if (degrees) BigMath.toDegrees(it) else it }
            Function.ATAN -> BigMath.atan(x).let { if (degrees) BigMath.toDegrees(it) else it }
            Function.LN -> BigMath.ln(x)
            Function.LOG -> BigMath.log10(x)
            Function.SQRT -> BigMath.sqrt(x)
            Function.CBRT -> BigMath.cbrt(x)
            Function.EXP -> BigMath.exp(x)
            Function.ABS -> x.abs()
        }
    }
}
