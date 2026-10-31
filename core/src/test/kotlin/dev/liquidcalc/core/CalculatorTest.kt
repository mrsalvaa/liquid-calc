// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.core

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CalculatorTest {
    /** Resultado como se muestra en pantalla. */
    private fun show(expr: String, angle: AngleUnit = AngleUnit.DEGREES, ans: String = "0"): String =
        when (val r = Calculator.evaluate(expr, angle, BigDecimal(ans))) {
            is CalcResult.Value -> Format.display(r.value)
            is CalcResult.Error -> "error: ${r.error}"
        }

    private fun error(expr: String, angle: AngleUnit = AngleUnit.DEGREES): CalcError {
        val r = Calculator.evaluate(expr, angle)
        assertIs<CalcResult.Error>(r, "se esperaba un error para \"$expr\"")
        return r.error
    }

    // --- Precisión decimal --------------------------------------------------------------------

    @Test fun decimalSumIsExact() {
        assertEquals("0.3", show("0.1 + 0.2"))
        assertEquals("0.3", show("0,1+0,2"))
        assertEquals(BigDecimal("0.3"), (Calculator.evaluate("0.1+0.2") as CalcResult.Value).value)
    }

    @Test fun decimalSubtractionAndMultiplication() {
        assertEquals("0.1", show("0.3 - 0.2"))
        assertEquals("0.07", show("0.1 × 0.7"))
        assertEquals("1.1", show("3.3 ÷ 3"))
        assertEquals("4.35", show("1.45 * 3"))
        assertEquals("0", show("0.1 + 0.2 - 0.3"))
    }

    @Test fun thirdsRoundTrip() {
        assertEquals("1", show("1 ÷ 3 × 3"))
        assertEquals("0.333333333333333", show("1/3"))
        assertEquals("0.666666666666667", show("2/3"))
    }

    @Test fun bigAndSmallNumbers() {
        assertEquals("1.26765060022823e30", show("2^100"))
        assertEquals("1e-7", show("0.0000001"))
        assertEquals("0.000001", show("0.000001"))
        assertEquals("123456789012345", show("123456789012345"))
        assertEquals("1.23456789012346e15", show("1234567890123456"))
    }

    // --- Precedencia y anidamiento ------------------------------------------------------------

    @Test fun precedence() {
        assertEquals("14", show("2 + 3 × 4"))
        assertEquals("20", show("(2 + 3) × 4"))
        assertEquals("-4", show("-2^2"))
        assertEquals("4", show("(-2)^2"))
        assertEquals("512", show("2^3^2"))
        assertEquals("0.5", show("2^-1"))
        assertEquals("5", show("8 - 3"))
        assertEquals("1", show("8 - 4 - 3"))
        assertEquals("1", show("8 ÷ 4 ÷ 2"))
        assertEquals("6", show("--6"))
    }

    @Test fun nestedExpressions() {
        assertEquals("21", show("((1 + 2) × (3 + 4))"))
        assertEquals("2", show("(((((2)))))"))
        assertEquals("-9", show("-(1 + (2 × (3 + 1)))"))
        assertEquals("3", show("√(4 + (2 + 3))"))
        assertEquals("1", show("sin(90 × (1 + 0))"))
    }

    @Test fun missingClosingParenthesesAreClosed() {
        assertEquals("20", show("(2 + 3) × (4"))
        assertEquals("0.5", show("sin(30"))
        assertEquals("21", show("((1 + 2) × (3 + 4"))
    }

    @Test fun implicitMultiplication() {
        assertEquals("6", show("2(3)"))
        assertEquals("6.28318530717959", show("2π"))
        assertEquals("12", show("(2)(6)"))
        assertEquals("6", show("2√9"))
        assertEquals("1", show("2sin(30)"))
    }

    // --- Porcentaje ---------------------------------------------------------------------------

    @Test fun percentAlone() {
        assertEquals("0.1", show("10%"))
        assertEquals("0.005", show("0.5%"))
    }

    @Test fun percentAfterAdditionIsShareOfLeft() {
        assertEquals("220", show("200 + 10%"))
        assertEquals("180", show("200 - 10%"))
        assertEquals("110", show("100 + 10%"))
        assertEquals("121", show("100 + 10% + 10%"))
        assertEquals("230", show("200 + (15%)"))
    }

    @Test fun percentAfterMultiplicationIsFraction() {
        assertEquals("5", show("50 × 10%"))
        assertEquals("500", show("50 ÷ 10%"))
        assertEquals("200.2", show("200 + 10% × 2"))
    }

    // --- Funciones científicas ----------------------------------------------------------------

    @Test fun trigonometryInDegreesIsExactAtNotableAngles() {
        assertEquals("0.5", show("sin(30)"))
        assertEquals("1", show("sin(90)"))
        assertEquals("0", show("sin(180)"))
        assertEquals("0", show("cos(90)"))
        assertEquals("-1", show("cos(180)"))
        assertEquals("0.5", show("cos(60)"))
        assertEquals("1", show("tan(45)"))
        assertEquals("0", show("sin(-360)"))
        assertEquals("-0.5", show("sin(-30)"))
        assertEquals("0.707106781186548", show("sin(45)"))
    }

    @Test fun trigonometryInRadians() {
        assertEquals("0", show("sin(π)", AngleUnit.RADIANS))
        assertEquals("-1", show("cos(π)", AngleUnit.RADIANS))
        assertEquals("1", show("sin(π/2)", AngleUnit.RADIANS))
        assertEquals("0.841470984807897", show("sin(1)", AngleUnit.RADIANS))
        assertEquals("1.5574077246549", show("tan(1)", AngleUnit.RADIANS))
    }

    @Test fun inverseTrigonometry() {
        assertEquals("30", show("asin(0.5)"))
        assertEquals("60", show("acos(0.5)"))
        assertEquals("45", show("atan(1)"))
        assertEquals("90", show("asin(1)"))
        assertEquals("0", show("acos(1)"))
        assertEquals("-45", show("atan(-1)"))
        assertEquals("1.5707963267949", show("asin(1)", AngleUnit.RADIANS))
        assertEquals("1.47112767430373", show("atan(10)", AngleUnit.RADIANS))
    }

    @Test fun logarithmsAndExponential() {
        assertEquals("2", show("log(100)"))
        assertEquals("-3", show("log(0.001)"))
        assertEquals("1", show("ln(e)"))
        assertEquals("0", show("ln(1)"))
        assertEquals("2.30258509299405", show("ln(10)"))
        assertEquals("0.301029995663981", show("log(2)"))
        assertEquals("2.71828182845905", show("e"))
        assertEquals("7.38905609893065", show("exp(2)"))
        assertEquals("1", show("exp(ln(1))"))
        assertEquals("5", show("exp(ln(5))"))
    }

    @Test fun constantsHaveManyCorrectDigits() {
        val pi = (Calculator.evaluate("π") as CalcResult.Value).value
        assertEquals("3.141592653589793238462643383279503", pi.toPlainString())
        val e = (Calculator.evaluate("e") as CalcResult.Value).value
        assertEquals("2.718281828459045235360287471352662", e.toPlainString())
    }

    @Test fun powersAndRoots() {
        assertEquals("1024", show("2^10"))
        assertEquals("2", show("4^0.5"))
        assertEquals("3", show("27^(1/3)"))
        assertEquals("1.4142135623731", show("√2"))
        assertEquals("12", show("√144"))
        assertEquals("1.5", show("√2.25"))
        assertEquals("-3", show("∛-27"))
        assertEquals("1", show("0^0"))
        assertEquals("0", show("0^5"))
        assertEquals("-1", show("(-1)^1000001"))
        assertEquals("1", show("1^99999999999999"))
        assertEquals("0.001", show("10^-3"))
    }

    @Test fun factorial() {
        assertEquals("1", show("0!"))
        assertEquals("120", show("5!"))
        assertEquals("3628800", show("10!"))
        assertEquals("9.33262154439442e157", show("100!"))
        assertEquals("720", show("3!!"))
    }

    // --- Errores ------------------------------------------------------------------------------

    @Test fun divisionByZero() {
        assertEquals(CalcError.DivisionByZero, error("1 ÷ 0"))
        assertEquals(CalcError.DivisionByZero, error("5 / (2 - 2)"))
        assertEquals(CalcError.DivisionByZero, error("0^-1"))
        assertEquals(CalcError.DivisionByZero, error("1 ÷ 0%"))
    }

    @Test fun invalidDomain() {
        assertEquals(CalcError.Domain, error("√-4"))
        assertEquals(CalcError.Domain, error("ln(0)"))
        assertEquals(CalcError.Domain, error("log(-1)"))
        assertEquals(CalcError.Domain, error("asin(2)"))
        assertEquals(CalcError.Domain, error("acos(-1.5)"))
        assertEquals(CalcError.Domain, error("tan(90)"))
        assertEquals(CalcError.Domain, error("tan(-270)"))
        assertEquals(CalcError.Domain, error("2.5!"))
        assertEquals(CalcError.Domain, error("(-3)!"))
        assertEquals(CalcError.Domain, error("(-8)^0.5"))
    }

    @Test fun overflow() {
        assertEquals(CalcError.Overflow, error("10^10000"))
        assertEquals(CalcError.Overflow, error("5000!"))
        assertEquals(CalcError.Overflow, error("exp(1000000)"))
        assertEquals(CalcError.Overflow, error("(10^9999) × 10"))
        assertEquals("0", show("10^-20000"))
    }

    @Test fun syntaxErrors() {
        assertIs<CalcError.Syntax>(error(""))
        assertIs<CalcError.Syntax>(error("2 +"))
        assertIs<CalcError.Syntax>(error("× 2"))
        assertIs<CalcError.Syntax>(error("()"))
        assertIs<CalcError.Syntax>(error("2 3"))
        assertIs<CalcError.Syntax>(error("1.2.3"))
        assertIs<CalcError.Syntax>(error("sin 30"))
        assertIs<CalcError.Syntax>(error("2 )"))
        assertIs<CalcError.Syntax>(error("hola"))
        assertIs<CalcError.Syntax>(error("2 # 3"))
        assertEquals(CalcError.Syntax(2), error("2 #"))
    }

    @Test fun noCodeIsExecuted() {
        // El parser solo entiende números, operadores y funciones conocidas.
        assertIs<CalcError.Syntax>(error("System.exit(0)"))
        assertIs<CalcError.Syntax>(error("java.lang.Runtime"))
    }

    // --- ANS, ángulos y memoria ---------------------------------------------------------------

    @Test fun ans() {
        assertEquals("15", show("ans + 5", ans = "10"))
        assertEquals("20", show("2ans", ans = "10"))
        assertEquals("0", show("ans"))
    }

    @Test fun memory() {
        var m = Memory()
        m = m.plus(BigDecimal("0.1")).plus(BigDecimal("0.2"))
        assertEquals(BigDecimal("0.3"), m.recall())
        m = m.minus(BigDecimal("1"))
        assertEquals(BigDecimal("-0.7"), m.recall())
        assertEquals(true, m.isSet)
        m = m.clear()
        assertEquals(BigDecimal.ZERO, m.recall())
        assertEquals(false, m.isSet)
    }

    @Test fun keyboardAliases() {
        assertEquals("6", show("2*3"))
        assertEquals("2", show("6/3"))
        assertEquals("1", show("3−2"))
        assertEquals("3", show("sqrt(9)"))
        assertEquals("3.14159265358979", show("pi"))
        assertEquals("1", show("SIN(90)"))
    }

    @Test fun displayFormat() {
        assertEquals("0,3", Format.display(BigDecimal("0.3"), ','))
        assertEquals("1,5e20", Format.display(BigDecimal("1.5E+20"), ','))
        assertEquals("100", Format.display(BigDecimal("1E+2")))
        assertEquals("0", Format.display(BigDecimal("0E-34")))
        assertEquals("-2.5", Format.display(BigDecimal("-2.50")))
    }
}
