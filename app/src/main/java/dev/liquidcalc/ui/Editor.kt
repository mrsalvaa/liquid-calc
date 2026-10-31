// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

/**
 * La expresión que se va escribiendo y qué hace cada tecla. Es inmutable y no depende de Android,
 * así se prueba en la JVM. La expresión usa los símbolos de pantalla (× ÷ − √ π) y [separator]
 * como separador decimal; el motor entiende los dos.
 *
 * [evaluated] = se acaba de tocar "=": un número empieza una cuenta nueva y un operador sigue
 * desde el resultado (ANS).
 */
data class Editor(
    val expression: String = "",
    val evaluated: Boolean = false,
    val separator: Char = '.',
) {
    private val last: Char? get() = expression.lastOrNull()

    /** Termina en algo que puede llevar un operador después: número, ), π, e, ANS, % o !. */
    private val endsWithOperand: Boolean
        get() = last?.let { it.isDigit() || it in ")πe%!" || expression.endsWith(ANS) } ?: false

    private fun fresh(text: String) = copy(expression = text, evaluated = false)

    private fun append(text: String) = copy(expression = expression + text, evaluated = false)

    fun digit(d: Char): Editor = if (evaluated) fresh(d.toString()) else append(d.toString())

    fun decimal(): Editor {
        if (evaluated) return fresh("0$separator")
        val number = expression.takeLastWhile { it.isDigit() || it == '.' || it == ',' }
        return when {
            number.any { it == '.' || it == ',' } -> this
            number.isEmpty() -> append("0$separator")
            else -> append(separator.toString())
        }
    }

    /** + − × ÷ ^: reemplaza al operador anterior, salvo un − después de × ÷ ^ (número negativo). */
    fun operator(op: Char): Editor {
        if (evaluated) return fresh(ANS + op)
        val l = last ?: return if (op == MINUS) append(op.toString()) else this
        return when {
            l in BINARY && op == MINUS && l in "×÷^" -> append(op.toString())
            l in BINARY -> {
                // "×−" también se reemplaza entero: 5×− y luego + da 5+.
                val trimmed = expression.dropLastWhile { it in BINARY }
                if (trimmed.isEmpty() || trimmed.last() == '(' || trimmed.last() in PREFIX) {
                    if (op == MINUS) copy(expression = trimmed + op) else this
                } else {
                    copy(expression = trimmed + op)
                }
            }
            l == '(' || l in PREFIX -> if (op == MINUS) append(op.toString()) else this
            else -> append(op.toString())
        }
    }

    /** % y !: solo después de un operando. */
    fun postfix(c: Char): Editor = when {
        evaluated -> fresh(ANS + c)
        endsWithOperand -> append(c.toString())
        else -> this
    }

    /** x², 1/x: agrega una potencia al operando (^2, ^(−1)). */
    fun power(suffix: String): Editor = when {
        evaluated -> fresh(ANS + suffix)
        endsWithOperand -> append(suffix)
        else -> this
    }

    /** sin(, √, π, e, ANS, un número de la memoria…: lo que empieza un operando nuevo. */
    fun operand(text: String): Editor = if (evaluated) fresh(text) else append(text)

    fun open(): Editor = operand("(")

    fun close(): Editor =
        if (!evaluated && openParens > 0 && endsWithOperand) append(")") else this

    /** El ( ) del teclado básico: cierra si hay algo para cerrar, si no abre. */
    fun parenthesis(): Editor =
        if (!evaluated && openParens > 0 && endsWithOperand) append(")") else open()

    private val openParens: Int get() = expression.count { it == '(' } - expression.count { it == ')' }

    /** Borra el último elemento entero: "sin(" o "ANS" de una vez. */
    fun backspace(): Editor {
        val token = MULTI_CHAR.firstOrNull { expression.endsWith(it) }
        return copy(expression = expression.dropLast(token?.length ?: 1), evaluated = false)
    }

    fun clear(): Editor = fresh("")

    /** Para reutilizar una cuenta del historial o pegar texto. */
    fun set(text: String): Editor = fresh(text)

    companion object {
        const val ANS = "ANS"
        const val MINUS = '−'
        private const val BINARY = "+−×÷^"
        private const val PREFIX = "√∛"
        private val MULTI_CHAR = listOf("asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "ln(", "log(", "exp(", "abs(", ANS, "^(−1)")

        /**
         * Texto del teclado físico o del portapapeles con los símbolos de pantalla: * → ×,
         * / → ÷, - → −, pi → π, sqrt → √ y el separador decimal de la app.
         */
        fun normalize(text: String, separator: Char): String = text
            .replace("sqrt", "√", ignoreCase = true)
            .replace("cbrt", "∛", ignoreCase = true)
            .replace("pi", "π", ignoreCase = true)
            .replace(Regex("ans", RegexOption.IGNORE_CASE), ANS)
            .replace('*', '×').replace('/', '÷').replace('-', MINUS).replace('–', MINUS)
            .replace('.', separator).replace(',', separator)
            .filter { it.isLetterOrDigit() || it in "+−×÷^%!()√∛π$separator" }
    }
}
