// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.liquidcalc.core.AngleUnit
import dev.liquidcalc.core.CalcError
import dev.liquidcalc.core.CalcResult
import dev.liquidcalc.core.Calculator
import dev.liquidcalc.core.Format
import dev.liquidcalc.core.Memory
import dev.liquidcalc.data.History
import dev.liquidcalc.data.HistoryItem
import java.math.BigDecimal

/**
 * Estado de la calculadora mientras vive el proceso (sobrevive a girar la pantalla): la
 * expresión, el último resultado (ANS), la memoria y el error del último "=".
 */
object Session {
    var editor by mutableStateOf(Editor())
    var ans: BigDecimal by mutableStateOf(BigDecimal.ZERO)
        private set
    var memory by mutableStateOf(Memory())
        private set

    /** Error del último "=", hasta que se toque otra tecla. */
    var error by mutableStateOf<CalcError?>(null)
        private set

    /** Cuántas veces se borró todo manteniendo "borrar": la pantalla lo usa para el "pop". */
    var clears by mutableStateOf(0)
        private set

    /** Lo que había antes de borrar todo, para la animación de salida. */
    var cleared by mutableStateOf("")
        private set

    /** "Borrar": lo de antes del cursor. */
    fun backspace() = edit { it.backspace() }

    /** Borrar todo (mantener "borrar" apretado). */
    fun clearAll() {
        cleared = editor.expression
        edit { it.clear() }
        clears++
    }

    fun edit(change: (Editor) -> Editor) {
        editor = change(editor)
        error = null
    }

    fun evaluate(expression: String, angle: AngleUnit): CalcResult =
        Calculator.evaluate(expression, angle, ans)

    /** Resultado mientras se escribe: solo si hay una cuenta (no un número solo) y no hay error. */
    fun preview(angle: AngleUnit): BigDecimal? {
        val e = editor
        if (e.evaluated || e.expression.isEmpty() || e.expression.all { it.isDigit() || it == e.separator }) return null
        return (evaluate(e.engineText(), angle) as? CalcResult.Value)?.value
    }

    /** "=": guarda el resultado como ANS y en el historial (si está activado). */
    fun equals(angle: AngleUnit) {
        val e = editor
        if (e.expression.isEmpty() || e.evaluated) return
        when (val r = evaluate(e.engineText(), angle)) {
            is CalcResult.Value -> {
                ans = r.value
                editor = e.copy(evaluated = true, cursor = e.expression.length)
                error = null
                History.add(HistoryItem(e.expression, r.value.toPlainString(), System.currentTimeMillis()))
            }
            is CalcResult.Error -> error = r.error
        }
    }

    /** Lo que muestra la pantalla como resultado: el de "=" o el que se ve mientras se escribe. */
    fun currentValue(angle: AngleUnit): BigDecimal? = if (editor.evaluated) ans else preview(angle)

    fun memoryPlus(value: BigDecimal) { memory = memory.plus(value) }

    fun memoryMinus(value: BigDecimal) { memory = memory.minus(value) }

    fun memoryClear() { memory = memory.clear() }

    /** MR: el número guardado como operando (entre paréntesis si es negativo). */
    fun memoryRecall() {
        val text = Format.display(memory.recall(), editor.separator).replace('-', Editor.MINUS)
        edit { it.operand(if (memory.recall().signum() < 0) "($text)" else text) }
    }
}
