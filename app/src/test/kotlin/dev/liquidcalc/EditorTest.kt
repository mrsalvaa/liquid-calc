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
        assertEquals("2", type({ it.digit('2') }, { it.close() }))
    }

    @Test fun backspaceRemovesWholeTokens() {
        assertEquals("2×", Editor("2×sin(").backspace().expression)
        assertEquals("", Editor("ANS").backspace().expression)
        assertEquals("12", Editor("123").backspace().expression)
        assertEquals("5", Editor("5^(−1)").backspace().expression)
    }

    @Test fun postfixOnlyAfterOperand() {
        assertEquals("5!", type({ it.digit('5') }, { it.postfix('!') }))
        assertEquals("5+", type({ it.digit('5') }, { it.operator('+') }, { it.postfix('%') }))
        assertEquals("5^2", type({ it.digit('5') }, { it.power("^2") }))
    }

    @Test fun normalizeKeyboardAndClipboard() {
        assertEquals("2×3÷4−1", Editor.normalize("2*3/4-1", ','))
        assertEquals("√(2)+π", Editor.normalize("sqrt(2)+pi", '.'))
        assertEquals("1,5+ANS", Editor.normalize("1.5 + ans", ','))
        assertEquals("12", Editor.normalize("1;2", '.'))
    }
}
