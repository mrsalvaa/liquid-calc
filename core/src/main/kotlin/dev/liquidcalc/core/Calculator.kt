// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Unidad de los ángulos para las funciones trigonométricas. */
enum class AngleUnit { DEGREES, RADIANS }

/** Por qué no se pudo calcular una expresión. */
sealed interface CalcError {
    /** La expresión está mal escrita o incompleta; [position] es el carácter donde se notó. */
    data class Syntax(val position: Int) : CalcError
    data object DivisionByZero : CalcError
    /** Valor fuera del dominio de la función (raíz de un negativo, log de 0, tan 90°, 2,5!…). */
    data object Domain : CalcError
    /** El resultado es demasiado grande (más de 10^10000). */
    data object Overflow : CalcError
}

sealed interface CalcResult {
    /** [value] tiene hasta [Calculator.STORED_DIGITS] cifras significativas. */
    data class Value(val value: BigDecimal) : CalcResult
    data class Error(val error: CalcError) : CalcResult
}

/**
 * Punto de entrada del motor: interpreta [expression] con un parser propio (sin eval) y la
 * calcula con aritmética decimal exacta (BigDecimal), así que 0.1 + 0.2 da 0.3.
 */
object Calculator {
    /** Cifras con las que se guarda un resultado (como decimal128); es lo que reutiliza ANS. */
    const val STORED_DIGITS = 34

    private val STORED = MathContext(STORED_DIGITS, RoundingMode.HALF_EVEN)

    fun evaluate(expression: String, angle: AngleUnit = AngleUnit.DEGREES, ans: BigDecimal = BigDecimal.ZERO): CalcResult =
        try {
            val node = Parser(Lexer(expression).tokens()).parse()
            CalcResult.Value(Evaluator(angle, ans).eval(node).round(STORED).normalized())
        } catch (e: CalcException) {
            CalcResult.Error(e.error)
        } catch (e: ArithmeticException) {
            // Algún límite de BigDecimal que no se previó: mejor un error que cerrar la app.
            CalcResult.Error(CalcError.Overflow)
        }
}

internal class CalcException(val error: CalcError) : RuntimeException(null, null, false, false)

internal fun fail(error: CalcError): Nothing = throw CalcException(error)

/** Sin ceros de más ni "0E-34": 1.500 → 1.5, 0E-10 → 0. */
internal fun BigDecimal.normalized(): BigDecimal =
    if (signum() == 0) BigDecimal.ZERO else stripTrailingZeros()
