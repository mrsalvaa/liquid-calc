// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Memoria de la calculadora (M+, M−, MR, MC). Es inmutable: cada operación devuelve otra. */
data class Memory(val value: BigDecimal = BigDecimal.ZERO, val isSet: Boolean = false) {
    fun plus(x: BigDecimal) = Memory(value.add(x, STORED).normalized(), isSet = true)

    fun minus(x: BigDecimal) = Memory(value.subtract(x, STORED).normalized(), isSet = true)

    /** MR: lo guardado (0 si no hay nada). */
    fun recall(): BigDecimal = value

    fun clear() = Memory()

    private companion object {
        val STORED = MathContext(Calculator.STORED_DIGITS, RoundingMode.HALF_EVEN)
    }
}
