// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/**
 * Funciones matemáticas en BigDecimal, sin dependencias: series de Taylor y de arcotangente
 * hiperbólica con reducción de argumento. Trabajan con [DIGITS] cifras para que el resultado
 * guardado (34 cifras) y el que se muestra (15) salgan exactos.
 */
internal object BigMath {
    const val DIGITS = 50

    /** Exponente decimal máximo de un resultado (|x| < 10^10000). */
    const val MAX_EXPONENT = 10_000

    val MC = MathContext(DIGITS, RoundingMode.HALF_EVEN)
    private val WORK = MathContext(DIGITS + 10, RoundingMode.HALF_EVEN)
    private val WIDE = MathContext(130, RoundingMode.HALF_EVEN)
    private val TWO = BigDecimal(2)
    private val HALF = BigDecimal("0.5")
    private val EPSILON = BigDecimal.ONE.movePointLeft(DIGITS + 8)

    /** Con más cifras que el resto: hace falta para reducir ángulos grandes sin perder precisión. */
    val PI: BigDecimal by lazy {
        // Machin: π = 16·atan(1/5) − 4·atan(1/239).
        val fifth = BigDecimal.ONE.divide(BigDecimal(5), WIDE)
        val part = BigDecimal.ONE.divide(BigDecimal(239), WIDE)
        (atanSeries(fifth, WIDE).multiply(BigDecimal(16)) - atanSeries(part, WIDE).multiply(BigDecimal(4))).round(WIDE)
    }
    val LN2: BigDecimal by lazy { atanhSeries(BigDecimal.ONE.divide(BigDecimal(3), WIDE), WIDE).multiply(TWO).round(WIDE) }

    /** ln 10 = 3·ln 2 + ln 1,25 y ln 1,25 = 2·atanh(1/9). */
    val LN10: BigDecimal by lazy {
        (LN2.multiply(BigDecimal(3)) + atanhSeries(BigDecimal.ONE.divide(BigDecimal(9), WIDE), WIDE).multiply(TWO)).round(WIDE)
    }
    val E: BigDecimal by lazy { exp(BigDecimal.ONE) }

    /** Exponente decimal: 1234 → 3, 0,05 → −2. */
    fun exponent(x: BigDecimal): Int = x.precision() - x.scale() - 1

    /** Falla si |x| ≥ 10^10000; lo menor que 10^−10000 se toma como 0. */
    fun checked(x: BigDecimal): BigDecimal {
        if (x.signum() == 0) return BigDecimal.ZERO
        val e = exponent(x)
        if (e >= MAX_EXPONENT) fail(CalcError.Overflow)
        if (e < -MAX_EXPONENT) return BigDecimal.ZERO
        return x
    }

    private fun isInteger(x: BigDecimal) = x.signum() == 0 || x.stripTrailingZeros().scale() <= 0

    // --- Series --------------------------------------------------------------------------------

    /** atan(x) = x − x³/3 + x⁵/5 − …, para |x| chico. */
    private fun atanSeries(x: BigDecimal, mc: MathContext): BigDecimal {
        val eps = BigDecimal.ONE.movePointLeft(mc.precision + 5)
        val x2 = x.multiply(x, mc)
        var power = x
        var sum = x
        var k = 1
        while (true) {
            power = power.multiply(x2, mc).negate()
            val term = power.divide(BigDecimal(2 * k + 1), mc)
            if (term.abs() < eps) return sum
            sum = sum.add(term, mc)
            k++
        }
    }

    /** atanh(x) = x + x³/3 + x⁵/5 + …, para |x| chico. */
    private fun atanhSeries(x: BigDecimal, mc: MathContext): BigDecimal {
        val eps = BigDecimal.ONE.movePointLeft(mc.precision + 5)
        val x2 = x.multiply(x, mc)
        var power = x
        var sum = x
        var k = 1
        while (true) {
            power = power.multiply(x2, mc)
            val term = power.divide(BigDecimal(2 * k + 1), mc)
            if (term.abs() < eps) return sum
            sum = sum.add(term, mc)
            k++
        }
    }

    // --- Exponencial y logaritmos -------------------------------------------------------------

    fun exp(x: BigDecimal): BigDecimal {
        if (x.signum() == 0) return BigDecimal.ONE
        // e^23026 ≈ 10^10000.
        if (x > BigDecimal(23_026)) fail(CalcError.Overflow)
        if (x < BigDecimal(-23_026)) return BigDecimal.ZERO
        // x = n·ln2 + r, con |r| ≤ ln2/2; e^r se calcula con r/256 y se eleva al cuadrado 8 veces.
        val n = x.divide(LN2, WORK).setScale(0, RoundingMode.HALF_EVEN)
        val r = (x - n.multiply(LN2)).divide(BigDecimal(256), WORK)
        var sum = BigDecimal.ONE
        var term = BigDecimal.ONE
        var k = 1
        while (true) {
            term = term.multiply(r, WORK).divide(BigDecimal(k), WORK)
            if (term.abs() < EPSILON) break
            sum = sum.add(term, WORK)
            k++
        }
        repeat(8) { sum = sum.multiply(sum, WORK) }
        return checked(sum.multiply(TWO.pow(n.intValueExact(), WORK), WORK).round(MC))
    }

    fun ln(x: BigDecimal): BigDecimal {
        if (x.signum() <= 0) fail(CalcError.Domain)
        if (x.compareTo(BigDecimal.ONE) == 0) return BigDecimal.ZERO
        // x = m·10^k con m en [1, 10); después m = f·2^j con f en [0,75; 1,5).
        val k = exponent(x)
        var m = x.movePointLeft(k)
        var j = 0
        while (m > BigDecimal("1.5")) { m = m.divide(TWO); j++ }
        while (m < BigDecimal("0.75")) { m = m.multiply(TWO); j-- }
        val z = (m - BigDecimal.ONE).divide(m + BigDecimal.ONE, WORK)
        val lnM = atanhSeries(z, WORK).multiply(TWO) + LN2.multiply(BigDecimal(j))
        return (lnM + LN10.multiply(BigDecimal(k))).round(MC)
    }

    fun log10(x: BigDecimal): BigDecimal {
        if (x.signum() <= 0) fail(CalcError.Domain)
        // Potencias exactas de 10 (0,001; 1; 1000) dan un entero exacto.
        val s = x.stripTrailingZeros()
        if (s.unscaledValue() == BigInteger.ONE) return BigDecimal(-s.scale())
        return ln(x).divide(LN10, MC)
    }

    // --- Raíces y potencias -------------------------------------------------------------------

    fun sqrt(x: BigDecimal): BigDecimal {
        if (x.signum() < 0) fail(CalcError.Domain)
        if (x.signum() == 0) return BigDecimal.ZERO
        // x = m·10^(2k) con m en [1, 100): √x = √m·10^k.
        val k = Math.floorDiv(exponent(x), 2)
        val m = x.movePointLeft(2 * k)
        var y = BigDecimal(Math.sqrt(m.toDouble()))
        repeat(8) { y = (y + m.divide(y, WORK)).multiply(HALF) }
        return y.round(MC).movePointRight(k)
    }

    fun cbrt(x: BigDecimal): BigDecimal {
        if (x.signum() == 0) return BigDecimal.ZERO
        if (x.signum() < 0) return cbrt(x.negate()).negate()
        val k = Math.floorDiv(exponent(x), 3)
        val m = x.movePointLeft(3 * k)
        var y = BigDecimal(Math.cbrt(m.toDouble()))
        val three = BigDecimal(3)
        repeat(8) { y = (y.multiply(TWO) + m.divide(y.multiply(y, WORK), WORK)).divide(three, WORK) }
        return y.round(MC).movePointRight(k)
    }

    fun pow(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        if (isInteger(exponent)) {
            val n = exponent.toBigIntegerExact()
            if (base.signum() == 0) {
                return when (n.signum()) {
                    0 -> BigDecimal.ONE
                    1 -> BigDecimal.ZERO
                    else -> fail(CalcError.DivisionByZero)
                }
            }
            if (n.signum() == 0) return BigDecimal.ONE
            // 1^n y (−1)^n, aunque n sea enorme.
            if (base.abs().compareTo(BigDecimal.ONE) == 0) {
                return if (base.signum() < 0 && n.testBit(0)) BigDecimal.ONE.negate() else BigDecimal.ONE
            }
            // Cifras del resultado ≈ n·log10|base|: se descarta antes de calcular algo enorme.
            val magnitude = BigDecimal(n).multiply(log10(base.abs()), MathContext(20))
            if (magnitude > BigDecimal(MAX_EXPONENT)) fail(CalcError.Overflow)
            if (magnitude < BigDecimal(-MAX_EXPONENT)) return BigDecimal.ZERO
            return checked(base.pow(n.intValueExact(), MC))
        }
        return when (base.signum()) {
            0 -> if (exponent.signum() > 0) BigDecimal.ZERO else fail(CalcError.DivisionByZero)
            -1 -> fail(CalcError.Domain)
            else -> exp(exponent.multiply(ln(base), WORK))
        }
    }

    fun factorial(x: BigDecimal): BigDecimal {
        if (!isInteger(x) || x.signum() < 0) fail(CalcError.Domain)
        // 3249! ya pasa de 10^10000.
        if (x > BigDecimal(3248)) fail(CalcError.Overflow)
        var result = BigInteger.ONE
        for (i in 2..x.intValueExact()) result = result.multiply(BigInteger.valueOf(i.toLong()))
        return BigDecimal(result)
    }

    // --- Trigonometría ------------------------------------------------------------------------

    private val TINY = BigDecimal.ONE.movePointLeft(40)

    /** Lo que es 0 salvo por el último dígito (sin π, cos π/2) se devuelve como 0 exacto. */
    private fun snap(x: BigDecimal) = if (x.abs() < TINY) BigDecimal.ZERO else x.round(MC)

    /** Lleva el ángulo a [−π, π]. */
    private fun reduce(x: BigDecimal): BigDecimal {
        // Más allá de 10^60 rad, el ángulo reducido ya no tiene cifras significativas.
        if (exponent(x) > 60) fail(CalcError.Domain)
        val twoPi = PI.multiply(TWO)
        var r = x.remainder(twoPi, WIDE)
        if (r > PI) r -= twoPi
        if (r < PI.negate()) r += twoPi
        return r
    }

    fun sin(x: BigDecimal): BigDecimal {
        val r = reduce(x)
        val r2 = r.multiply(r, WORK)
        var term = r
        var sum = r
        var k = 1
        while (true) {
            term = term.multiply(r2, WORK).divide(BigDecimal((2L * k) * (2L * k + 1)), WORK).negate()
            if (term.abs() < EPSILON) return snap(sum)
            sum = sum.add(term, WORK)
            k++
        }
    }

    fun cos(x: BigDecimal): BigDecimal {
        val r = reduce(x)
        val r2 = r.multiply(r, WORK)
        var term = BigDecimal.ONE
        var sum = BigDecimal.ONE
        var k = 1
        while (true) {
            term = term.multiply(r2, WORK).divide(BigDecimal((2L * k - 1) * (2L * k)), WORK).negate()
            if (term.abs() < EPSILON) return snap(sum)
            sum = sum.add(term, WORK)
            k++
        }
    }

    fun tan(x: BigDecimal): BigDecimal {
        val c = cos(x)
        if (c.signum() == 0) fail(CalcError.Domain)
        return sin(x).divide(c, MC)
    }

    fun atan(x: BigDecimal): BigDecimal {
        if (x.signum() == 0) return BigDecimal.ZERO
        val halfPi = PI.multiply(HALF)
        val cmp = x.abs().compareTo(BigDecimal.ONE)
        if (cmp == 0) return PI.divide(BigDecimal(x.signum() * 4), MC)
        // atan(x) = ±π/2 − atan(1/x) para |x| > 1.
        if (cmp > 0) return (halfPi.multiply(BigDecimal(x.signum())) - atan(BigDecimal.ONE.divide(x, WORK))).round(MC)
        // atan(x) = 2·atan(x / (1 + √(1 + x²))): tres veces, para que la serie converja rápido.
        var y = x
        repeat(3) { y = y.divide(BigDecimal.ONE + sqrt(BigDecimal.ONE + y.multiply(y, WORK)), WORK) }
        return atanSeries(y, WORK).multiply(BigDecimal(8)).round(MC)
    }

    fun asin(x: BigDecimal): BigDecimal {
        val cmp = x.abs().compareTo(BigDecimal.ONE)
        if (cmp > 0) fail(CalcError.Domain)
        if (cmp == 0) return PI.multiply(HALF).multiply(BigDecimal(x.signum())).round(MC)
        return atan(x.divide(sqrt(BigDecimal.ONE - x.multiply(x)), WORK))
    }

    fun acos(x: BigDecimal): BigDecimal {
        if (x.abs() > BigDecimal.ONE) fail(CalcError.Domain)
        return (PI.multiply(HALF).round(MC) - asin(x)).round(MC)
    }

    // --- Grados -------------------------------------------------------------------------------

    private val D360 = BigDecimal(360)
    private val D180 = BigDecimal(180)

    /** Ángulo en grados llevado a [0, 360) sin redondear. */
    private fun degrees(d: BigDecimal): BigDecimal {
        val r = d.remainder(D360)
        return if (r.signum() < 0) r + D360 else r
    }

    private fun toRadians(d: BigDecimal) = d.multiply(PI, WIDE).divide(D180, WIDE)

    /** Seno de los ángulos con valor exacto (0, 30, 90, 150…): sin errores de redondeo. */
    private val EXACT_SIN = mapOf(
        0 to BigDecimal.ZERO, 30 to HALF, 90 to BigDecimal.ONE, 150 to HALF,
        180 to BigDecimal.ZERO, 210 to HALF.negate(), 270 to BigDecimal.ONE.negate(), 330 to HALF.negate(),
    )

    fun sinDegrees(d: BigDecimal): BigDecimal {
        val a = degrees(d)
        if (isInteger(a)) EXACT_SIN[a.intValueExact()]?.let { return it }
        return sin(toRadians(a))
    }

    fun cosDegrees(d: BigDecimal): BigDecimal = sinDegrees(d + BigDecimal(90))

    fun tanDegrees(d: BigDecimal): BigDecimal {
        val a = d.remainder(D180).let { if (it.signum() < 0) it + D180 else it }
        if (isInteger(a)) {
            when (a.intValueExact()) {
                0 -> return BigDecimal.ZERO
                45 -> return BigDecimal.ONE
                90 -> fail(CalcError.Domain)
                135 -> return BigDecimal.ONE.negate()
            }
        }
        return tan(toRadians(a))
    }

    /** Radianes a grados; lo que queda a un pelo de un entero (asin 0,5 = 30) se redondea. */
    fun toDegrees(r: BigDecimal): BigDecimal {
        val d = r.multiply(D180, WORK).divide(PI, WORK)
        val whole = d.setScale(0, RoundingMode.HALF_EVEN)
        return if ((d - whole).abs() < BigDecimal.ONE.movePointLeft(38)) whole else d.round(MC)
    }
}
