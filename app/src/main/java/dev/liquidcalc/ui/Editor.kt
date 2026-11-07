// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

/**
 * La expresión que se va escribiendo, el cursor y qué hace cada tecla. Es inmutable y no depende
 * de Android, así se prueba en la JVM.
 *
 * La expresión usa los símbolos de pantalla (× ÷ − π) y [separator] como separador decimal. Lo
 * que se ve en dos dimensiones (fracción, potencia, raíces, valor absoluto) va marcado con
 * caracteres de uso privado: cada estructura abre con su marca, separa sus casillas con [SEP] y
 * cierra con [END]. Por ejemplo x² es `x` [POW] `2` [END] y ³⁄₄ es [FRAC] `3` [SEP] `4` [END].
 * [toEngine] lo pasa al texto que entiende el motor.
 *
 * [cursor] es un índice de [expression]: cada marca cuenta como una posición, así "antes de la
 * fracción" y "al principio del numerador" son lugares distintos.
 *
 * [evaluated] = se acaba de tocar "=": un número empieza una cuenta nueva y un operador sigue
 * desde el resultado (ANS).
 */
data class Editor(
    val expression: String = "",
    val evaluated: Boolean = false,
    val separator: Char = '.',
    val cursor: Int = expression.length,
) {
    private val before: String get() = expression.substring(0, cursor)
    private val after: String get() = expression.substring(cursor)
    private val last: Char? get() = before.lastOrNull()

    /** Lo de antes del cursor puede llevar un operador después: número, ), π, e, ANS, %, ! o una estructura. */
    private val endsWithOperand: Boolean
        get() = last?.let { it.isDigit() || it in ")πe%!" || it == END || before.endsWith(ANS) } ?: false

    private fun fresh(text: String, cursorOffset: Int = text.length) =
        copy(expression = text, evaluated = false, cursor = cursorOffset)

    /** Pone [text] en el cursor; el cursor queda [cursorOffset] caracteres dentro de lo insertado. */
    private fun insert(text: String, cursorOffset: Int = text.length) =
        copy(expression = before + text + after, evaluated = false, cursor = cursor + cursorOffset)

    fun digit(d: Char): Editor = if (evaluated) fresh(d.toString()) else insert(d.toString())

    fun decimal(): Editor {
        if (evaluated) return fresh("0$separator")
        val number = before.takeLastWhile { it.isDigit() || it == '.' || it == ',' } +
            after.takeWhile { it.isDigit() || it == '.' || it == ',' }
        return when {
            number.any { it == '.' || it == ',' } -> this
            before.lastOrNull()?.isDigit() != true -> insert("0$separator")
            else -> insert(separator.toString())
        }
    }

    /** + − × ÷: reemplaza al operador anterior, salvo un − después de × ÷ (número negativo). */
    fun operator(op: Char): Editor {
        if (evaluated) return fresh(ANS + op)
        val l = last ?: return if (op == MINUS) insert(op.toString()) else this
        return when {
            l in BINARY && op == MINUS && l in "×÷" -> insert(op.toString())
            l in BINARY -> {
                // "×−" también se reemplaza entero: 5×− y luego + da 5+.
                val trimmed = before.dropLastWhile { it in BINARY }
                val start = trimmed.lastOrNull()
                if (start == null || start == '(' || start in OPENERS || start == SEP) {
                    if (op == MINUS) replaceBefore(trimmed + op) else this
                } else {
                    replaceBefore(trimmed + op)
                }
            }
            l == '(' || l in OPENERS || l == SEP -> if (op == MINUS) insert(op.toString()) else this
            else -> insert(op.toString())
        }
    }

    private fun replaceBefore(text: String) =
        copy(expression = text + after, evaluated = false, cursor = text.length)

    /** % y !: solo después de un operando. */
    fun postfix(c: Char): Editor = when {
        evaluated -> fresh(ANS + c)
        endsWithOperand -> insert(c.toString())
        else -> this
    }

    /**
     * Potencia del operando de la izquierda: x² ("2"), x⁻¹ ("−1") o xʸ (vacía, con el cursor
     * adentro). Sin operando a la izquierda no hace nada.
     */
    fun power(exponent: String = ""): Editor {
        val text = "$POW$exponent$END"
        val inside = if (exponent.isEmpty()) 1 else text.length
        return when {
            evaluated -> fresh(ANS + text, ANS.length + inside)
            endsWithOperand -> insert(text, inside)
            else -> this
        }
    }

    /** 10ˣ y eˣ: la base y la potencia vacía, con el cursor en el exponente. */
    fun base(base: String): Editor = operand("$base$POW$END", base.length + 1)

    /** Fracción. Si el cursor está después de un número, ese número pasa a ser el numerador. */
    fun fraction(): Editor {
        if (evaluated) return fresh("$FRAC$ANS$SEP$END", ANS.length + 2)
        val number = before.takeLastWhile { it.isDigit() || it == '.' || it == ',' }
        if (number.isNotEmpty()) {
            val head = before.dropLast(number.length)
            val text = head + FRAC + number + SEP + END
            return copy(expression = text + after, evaluated = false, cursor = text.length - 1)
        }
        return insert("$FRAC$SEP$END", 1)
    }

    fun sqrt(): Editor = operand("$SQRT$END", 1)

    /** Raíz n-ésima: el cursor va al índice. */
    fun root(): Editor = operand("$ROOT$SEP$END", 1)

    /** ∛: raíz con el índice 3 ya puesto y el cursor en el radicando. */
    fun cbrt(): Editor = operand("${ROOT}3$SEP$END", 3)

    fun abs(): Editor = operand("$ABS$END", 1)

    /** sin(, π, e, ANS, un número de la memoria…: lo que empieza un operando nuevo. */
    fun operand(text: String, cursorOffset: Int = text.length): Editor =
        if (evaluated) fresh(text, cursorOffset) else insert(text, cursorOffset)

    fun open(): Editor = operand("(")

    /** ")" se puede poner siempre, como en una Casio (si sobra, "=" avisa el error). */
    fun close(): Editor = if (evaluated) this else insert(")")

    /** El ( ) del teclado básico: cierra si hay algo para cerrar, si no abre. */
    fun parenthesis(): Editor {
        val l = last
        // Cierra si hay uno abierto y lo de antes puede terminar un paréntesis: no después de
        // "(", de un operador o al principio de una casilla.
        val canClose = l != null && l != '(' && l !in BINARY && l !in OPENERS && l != SEP
        return if (!evaluated && openParens > 0 && canClose) insert(")") else open()
    }

    /** Paréntesis abiertos antes del cursor, dentro de la casilla donde está. */
    private val openParens: Int
        get() {
            val slot = before.substring(slotStart(cursor))
            var depth = 0
            var open = 0
            for (c in slot) {
                when {
                    c in OPENERS -> depth++
                    c == END -> depth--
                    depth == 0 && c == '(' -> open++
                    depth == 0 && c == ')' -> open--
                }
            }
            return open
        }

    /** Dónde empieza la casilla que contiene a [index] (0 si está en el nivel de afuera). */
    private fun slotStart(index: Int): Int {
        var depth = 0
        for (i in index - 1 downTo 0) {
            val c = expression[i]
            when {
                c == END -> depth++
                c in OPENERS -> if (depth == 0) return i + 1 else depth--
                c == SEP -> if (depth == 0) return i + 1
            }
        }
        return 0
    }

    /**
     * Borra lo que está antes del cursor: un elemento entero ("sin(", "ANS"). Antes de una
     * estructura entra a ella y borra su último carácter; al principio de una casilla vacía
     * saca la estructura entera si está vacía, si no solo mueve el cursor.
     */
    fun backspace(): Editor {
        if (evaluated) return copy(evaluated = false, cursor = expression.length).backspace()
        val l = last ?: return this
        return when {
            // Entra a la casilla de la izquierda y borra desde ahí.
            l == END || l == SEP -> copy(cursor = cursor - 1).backspace()
            l in OPENERS -> {
                val end = structureEnd(cursor - 1)
                val body = expression.substring(cursor, end)
                if (body.all { it == SEP }) {
                    copy(expression = expression.removeRange(cursor - 1, end + 1), cursor = cursor - 1)
                } else {
                    copy(cursor = cursor - 1)
                }
            }
            else -> {
                val token = MULTI_CHAR.firstOrNull { before.endsWith(it) }
                val n = token?.length ?: 1
                copy(expression = expression.removeRange(cursor - n, cursor), cursor = cursor - n)
            }
        }
    }

    /** Índice del [END] que cierra la estructura que abre en [open]. */
    private fun structureEnd(open: Int): Int {
        var depth = 0
        for (i in open + 1 until expression.length) {
            val c = expression[i]
            if (c in OPENERS) depth++
            if (c == END) if (depth == 0) return i else depth--
        }
        return expression.length - 1
    }

    /** Lo que se borraría con [backspace] (para la animación); vacío si solo se mueve el cursor. */
    fun deletedBy(next: Editor): String =
        if (next.expression.length < expression.length && !evaluated) {
            expression.substring(next.cursor, next.cursor + expression.length - next.expression.length)
        } else {
            ""
        }

    fun left(): Editor {
        if (evaluated) return copy(evaluated = false, cursor = expression.length).left()
        if (cursor == 0) return this
        val n = MULTI_CHAR.firstOrNull { before.endsWith(it) }?.length ?: 1
        return copy(cursor = cursor - n)
    }

    fun right(): Editor {
        if (evaluated) return copy(evaluated = false, cursor = expression.length)
        if (cursor == expression.length) return this
        val n = MULTI_CHAR.firstOrNull { after.startsWith(it) }?.length ?: 1
        return copy(cursor = cursor + n)
    }

    /** Toque en la pantalla: el cursor va a [index] y se puede seguir editando la cuenta. */
    fun moveTo(index: Int): Editor = copy(cursor = index.coerceIn(0, expression.length), evaluated = false)

    fun clear(): Editor = fresh("")

    /** Para reutilizar una cuenta del historial o pegar texto. */
    fun set(text: String): Editor = fresh(text)

    /** Lo que se le pasa al motor. */
    fun engineText(): String = toEngine(expression)

    companion object {
        const val ANS = "ANS"
        const val MINUS = '−'

        /** Fracción: numerador [SEP] denominador [END]. */
        const val FRAC = ''
        /** Potencia del operando de la izquierda: exponente [END]. */
        const val POW = ''
        /** Raíz cuadrada: radicando [END]. */
        const val SQRT = ''
        /** Raíz n-ésima: índice [SEP] radicando [END]. */
        const val ROOT = ''
        /** Valor absoluto: contenido [END]. */
        const val ABS = ''
        const val SEP = ''
        const val END = ''
        const val OPENERS = "$FRAC$POW$SQRT$ROOT$ABS"

        private const val BINARY = "+−×÷^"
        private val MULTI_CHAR = listOf("asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "ln(", "log(", "exp(", "abs(", ANS, "^(−1)")

        /** El texto de pantalla en el que entiende el motor: x² → x^(2), ³⁄₄ → ((3)÷(4)). */
        fun toEngine(text: String): String {
            val out = StringBuilder()
            // Qué cierra cada estructura abierta, y qué separa sus casillas.
            val closers = ArrayDeque<String>()
            val seps = ArrayDeque<String>()
            for (c in text) {
                when (c) {
                    FRAC -> { out.append("(("); seps.addLast(")÷("); closers.addLast("))") }
                    POW -> { out.append("^("); seps.addLast(""); closers.addLast(")") }
                    SQRT -> { out.append("√("); seps.addLast(""); closers.addLast(")") }
                    ROOT -> { out.append("root("); seps.addLast(";"); closers.addLast(")") }
                    ABS -> { out.append("abs("); seps.addLast(""); closers.addLast(")") }
                    SEP -> out.append(seps.lastOrNull() ?: "")
                    END -> {
                        out.append(closers.removeLastOrNull() ?: "")
                        seps.removeLastOrNull()
                    }
                    else -> out.append(c)
                }
            }
            // "eANS" o "ANSsin(" se leerían como una sola palabra: se separan.
            return out.toString()
                .replace(Regex("(ANS|(?<![a-zA-Z])e(?!xp\\())(?=[a-zA-Z])"), "$1 ")
        }

        /**
         * Texto del teclado físico o del portapapeles con los símbolos de pantalla: * → ×,
         * / → ÷, - → −, pi → π, sqrt → √. Puntos y comas: si hay de los dos, el último es el
         * decimal y el otro separa miles ("27.000,3", "27,000.3"); si uno se repite, separa
         * miles ("1.000.000"); si no, es el decimal.
         */
        fun normalize(text: String, separator: Char): String {
            val clean = text
                .replace("sqrt", "√", ignoreCase = true)
                .replace("cbrt", "∛", ignoreCase = true)
                .replace("pi", "π", ignoreCase = true)
                .replace(Regex("ans", RegexOption.IGNORE_CASE), ANS)
                .replace('*', '×').replace('/', '÷').replace('-', MINUS).replace('–', MINUS)
            val numbers = Regex("[0-9][0-9.,]*").replace(clean) { m -> number(m.value, separator) }
            return numbers.filter { it.isLetterOrDigit() || it in "+−×÷^%!()√∛π$separator" }
        }

        private fun number(text: String, separator: Char): String {
            val dots = text.count { it == '.' }
            val commas = text.count { it == ',' }
            val decimal = when {
                dots > 0 && commas > 0 -> if (text.lastIndexOf('.') > text.lastIndexOf(',')) '.' else ','
                dots > 1 || commas > 1 -> null
                dots == 1 -> '.'
                commas == 1 -> ','
                else -> null
            }
            return text.filter { it.isDigit() || it == decimal }.replace(decimal ?: ' ', separator)
        }
    }
}
