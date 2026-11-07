// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import dev.liquidcalc.core.AngleUnit

/**
 * Teclado físico (Bluetooth, USB o el de una tablet). Se lee el carácter que produce la tecla,
 * así funciona con cualquier distribución: Mayús+8 da "*" en una y "(" en otra.
 *
 * Números y + − * / ^ % ! ( ) como siempre; Enter o = calcula; Retroceso borra; Esc o Supr limpia.
 * Letras: s sin, c cos, t tan, l ln, g log, r √, p π, e, a ANS.
 * Ctrl+C copia el resultado, Ctrl+V pega, Ctrl+H abre el historial, Ctrl+D cambia grados/radianes.
 */
fun handleHardwareKey(
    event: KeyEvent,
    angle: AngleUnit,
    copy: () -> Unit,
    paste: () -> Unit,
    history: () -> Unit,
    toggleAngle: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.isCtrlPressed) {
        when (event.key) {
            Key.C -> copy()
            Key.V -> paste()
            Key.H -> history()
            Key.D -> toggleAngle()
            else -> return false
        }
        return true
    }
    when (event.key) {
        Key.Enter, Key.NumPadEnter -> { Session.equals(angle); return true }
        Key.Backspace -> { Session.backspace(); return true }
        Key.DirectionLeft -> { Session.edit { it.left() }; return true }
        Key.DirectionRight -> { Session.edit { it.right() }; return true }
        Key.Escape, Key.Delete -> { Session.edit { it.clear() }; return true }
        else -> Unit
    }
    val c = event.utf16CodePoint.takeIf { it > 0 }?.toChar() ?: return false
    val change: ((Editor) -> Editor)? = when (c) {
        in '0'..'9' -> { e -> e.digit(c) }
        '.', ',' -> { e -> e.decimal() }
        '+' -> { e -> e.operator('+') }
        '-', '−' -> { e -> e.operator(Editor.MINUS) }
        '*', 'x', '×' -> { e -> e.operator('×') }
        '/', '÷' -> { e -> e.operator('÷') }
        '^' -> { e -> e.power() }
        '%', '!' -> { e -> e.postfix(c) }
        '(' -> { e -> e.open() }
        ')' -> { e -> e.close() }
        's' -> { e -> e.operand("sin(") }
        'c' -> { e -> e.operand("cos(") }
        't' -> { e -> e.operand("tan(") }
        'l' -> { e -> e.operand("ln(") }
        'g' -> { e -> e.operand("log(") }
        'r' -> { e -> e.sqrt() }
        'p' -> { e -> e.operand("π") }
        'e' -> { e -> e.operand("e") }
        'a' -> { e -> e.operand(Editor.ANS) }
        '=' -> null.also { Session.equals(angle) }
        else -> return false
    }
    if (change != null) Session.edit(change)
    return true
}
