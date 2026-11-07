// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc

import dev.liquidcalc.ui.Editor
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorTest {
    private fun type(vararg steps: (Editor) -> Editor, start: Editor = Editor(separator = ',')) =
        steps.fold(start) { e, step -> step(e) }.expression

    @Test fun digitsAndDecimal() {
        assertEquals("0,5", type({ it.decimal() }, { it.digit('5') }))
        assertEquals("1,25", type({ it.digit('1') }, { it.decimal() }, { it.digit('2') }, { it.decimal() }, { it.digit('5') }))
        assertEquals("3+0,", type({ it.digit('3') }, { it.operator('+') }, { it.decimal() }))
    }

    @Test fun operatorsReplaceEachOther() {
        assertEquals("5+", type({ it.digit('5') }, { it.operator('×') }, { it.operator('+') }))
        assertEquals("5×−", type({ it.digit('5') }, { it.operator('×') }, { it.operator('−') }))
        assertEquals("5+", type({ it.digit('5') }, { it.operator('×') }, { it.operator('−') }, { it.operator('+') }))
        assertEquals("−", type({ it.operator('−') }))
        assertEquals("", type({ it.operator('×') }))
        assertEquals("(−", type({ it.open() }, { it.operator('−') }))
    }

    @Test fun afterEqualsANumberStartsOverAndAnOperatorContinues() {
        val done = Editor("2+2", evaluated = true)
        assertEquals("7", done.digit('7').expression)
        assertEquals("ANS×", done.operator('×').expression)
        assertEquals("ANS%", done.postfix('%').expression)
        assertEquals("sin(", done.operand("sin(").expression)
    }

    @Test fun smartParenthesis() {
        assertEquals("(", type({ it.parenthesis() }))
        assertEquals("(2", type({ it.parenthesis() }, { it.digit('2') }))
        assertEquals("(2)", type({ it.parenthesis() }, { it.digit('2') }, { it.parenthesis() }))
        assertEquals("(2+(", type({ it.parenthesis() }, { it.digit('2') }, { it.operator('+') }, { it.parenthesis() }))
        assertEquals("2)", type({ it.digit('2') }, { it.close() }))
        assertEquals("()", type({ it.open() }, { it.close() }))
    }

    @Test fun backspaceRemovesWholeTokens() {
        assertEquals("2×", Editor("2×sin(").backspace().expression)
        assertEquals("", Editor("ANS").backspace().expression)
        assertEquals("12", Editor("123").backspace().expression)
        assertEquals("5", Editor("5^(−1)").backspace().expression)
        // x²: el primer borrar saca el 2, el segundo la potencia vacía.
        val squared = Editor("5").power("2")
        assertEquals("5${P}$E", squared.backspace().expression)
        assertEquals("5", squared.backspace().backspace().expression)
    }

    @Test fun postfixOnlyAfterOperand() {
        assertEquals("5!", type({ it.digit('5') }, { it.postfix('!') }))
        assertEquals("5+", type({ it.digit('5') }, { it.operator('+') }, { it.postfix('%') }))
        assertEquals("5${P}2$E", type({ it.digit('5') }, { it.power("2") }))
        assertEquals("", type({ it.power("2") }))
    }

    @Test fun normalizeKeyboardAndClipboard() {
        assertEquals("2×3÷4−1", Editor.normalize("2*3/4-1", ','))
        assertEquals("√(2)+π", Editor.normalize("sqrt(2)+pi", '.'))
        assertEquals("1,5+ANS", Editor.normalize("1.5 + ans", ','))
        assertEquals("12", Editor.normalize("1;2", '.'))
        assertEquals("27000,3", Editor.normalize("27.000,3", ','))
        assertEquals("27000,3", Editor.normalize("27,000.3", ','))
        assertEquals("1000000", Editor.normalize("1.000.000", ','))
        assertEquals("2,5", Editor.normalize("2.5", ','))
    }

    @Test fun cursorInsertsInTheMiddle() {
        val e = Editor("12+3", separator = ',').moveTo(2)
        assertEquals("125+3", e.digit('5').expression)
        assertEquals(3, e.digit('5').cursor)
        assertEquals("1+3", Editor("12+3").moveTo(2).backspace().expression)
        assertEquals("sin(", Editor("sin(").left().right().expression.substring(0, Editor("sin(").left().right().cursor))
        assertEquals(0, Editor("sin(").left().cursor)
    }

    @Test fun structures() {
        // 3 y fracción: el 3 pasa a ser el numerador y el cursor va al denominador.
        val f = Editor("3").fraction().digit('4')
        assertEquals("${F}3${S}4$E", f.expression)
        assertEquals("((3)÷(4))", f.engineText())
        assertEquals("√(2)", Editor().sqrt().digit('2').engineText())
        assertEquals("root(5;32)", Editor().root().digit('5').right().digit('3').digit('2').engineText())
        assertEquals("10^(3)", Editor().base("10").digit('3').engineText())
        assertEquals("e^(2)", Editor().base("e").digit('2').engineText())
        assertEquals("2^(3)+1", Editor("2").power().digit('3').right().operator('+').digit('1').engineText())
        assertEquals("abs(−2)", Editor().abs().operator('−').digit('2').engineText())
        // Una estructura vacía se va con un solo borrar.
        assertEquals("", Editor().fraction().backspace().expression)
    }

    @Test fun engineSeparatesWords() {
        assertEquals("e ANS", Editor.toEngine("eANS"))
        assertEquals("ANS sin(30)", Editor.toEngine("ANSsin(30)"))
        assertEquals("exp(1)", Editor.toEngine("exp(1)"))
    }

    private companion object {
        const val P = Editor.POW
        const val F = Editor.FRAC
        const val S = Editor.SEP
        const val E = Editor.END
    }
}
