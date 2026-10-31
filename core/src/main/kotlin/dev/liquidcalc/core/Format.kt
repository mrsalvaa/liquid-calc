// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Cómo se muestra un resultado. */
object Format {
    /** Cifras significativas en pantalla: suficientes y sin el ruido de las últimas cifras. */
    const val DISPLAY_DIGITS = 15

    /**
     * Texto para mostrar: hasta [digits] cifras significativas, sin ceros de más y con
     * [decimalSeparator]. Los números muy grandes o muy chicos van en notación científica
     * ("1.5e20", "2e-9"); el resto, con todas sus cifras ("123456789", "0.000012").
     */
    fun display(value: BigDecimal, decimalSeparator: Char = '.', digits: Int = DISPLAY_DIGITS): String {
        val r = value.round(MathContext(digits, RoundingMode.HALF_EVEN)).normalized()
        if (r.signum() == 0) return "0"
        val exponent = r.precision() - r.scale() - 1
        val text = if (exponent in -6 until digits) {
            r.toPlainString()
        } else {
            r.movePointLeft(exponent).normalized().toPlainString() + "e" + exponent
        }
        return if (decimalSeparator == '.') text else text.replace('.', decimalSeparator)
    }
}
