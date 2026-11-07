// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquidcalc.ui.glass.GlassSurface
import dev.liquidcalc.ui.glass.LocalGlass
import dev.liquidcalc.ui.glass.accentFill
import dev.liquidcalc.ui.glass.styled

/** Cómo se ve una tecla: números, operadores, funciones, "=" o borrar. */
enum class KeyKind { DIGIT, OPERATOR, FUNCTION, EQUALS, CLEAR }

/**
 * Una tecla. [spoken] es lo que lee TalkBack ("dividido", "seno"); si es null, lee [label].
 * [icon], si está, reemplaza al texto (borrar). [onHold] se ejecuta si se mantiene apretada
 * [HOLD_MS] (borrar: todo; en las científicas, la segunda función, como el SHIFT de una Casio);
 * [holdLabel] es cómo lo anuncia TalkBack y [shiftLabel], lo que se ve chico arriba de la tecla.
 * [weight] es el ancho relativo en su fila.
 */
class CalcKey(
    val label: String,
    @StringRes val spoken: Int?,
    val kind: KeyKind,
    val icon: ImageVector? = null,
    @StringRes val holdLabel: Int? = null,
    val shiftLabel: String? = null,
    /** [label] y [shiftLabel] van en escritura natural (fracción apilada, √ con cuadradito). */
    val math: Boolean = false,
    val weight: Float = 1f,
    val onHold: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

/** Cuánto hay que mantener apretada una tecla para su acción larga. */
const val HOLD_MS = 400L

/** Teclas en filas iguales que ocupan todo el lugar disponible. */
@Composable
fun KeyGrid(rows: List<List<CalcKey>>, modifier: Modifier = Modifier, uniform: Boolean = false) {
    Column(modifier) {
        rows.forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // uniform: todas del mismo ancho, sin el ancho propio de cada tecla.
                row.forEach { key -> KeyButton(key, Modifier.weight(if (uniform) 1f else key.weight).fillMaxHeight()) }
            }
        }
    }
}

/**
 * Tecla de cristal con forma de píldora (en el estilo Sólido, un rectángulo de esquinas chicas).
 * El "=" va relleno con el color de énfasis; los operadores, con el texto en ese color.
 *
 * El toque se toma en toda la celda (también el margen entre teclas y el borde de la píldora) y
 * no se cancela si el dedo se corre un poco: al soltar, cuenta como toque.
 */
@Composable
fun KeyButton(key: CalcKey, modifier: Modifier = Modifier) {
    val p = LocalGlass.current
    val haptics = LocalHapticFeedback.current
    val spoken = key.spoken?.let { stringResource(it) } ?: key.label
    val holdLabel = key.holdLabel?.let { stringResource(it) }
    val interaction = remember { MutableInteractionSource() }
    val current by rememberUpdatedState(key)
    val color = when (key.kind) {
        KeyKind.DIGIT, KeyKind.FUNCTION -> p.content
        KeyKind.OPERATOR, KeyKind.CLEAR -> p.accentFg
        KeyKind.EQUALS -> p.onAccent
    }
    val click = {
        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        current.onClick()
    }
    Box(
        modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    interaction.tryEmit(press)
                    val k = current
                    val start = down.uptimeMillis
                    var held = false
                    // Hasta que se levanten todos los dedos; salir de la tecla no cancela.
                    while (true) {
                        val wait = if (held || k.onHold == null) Long.MAX_VALUE else (HOLD_MS - currentEventUptime(start)).coerceAtLeast(0)
                        val event = withTimeoutOrNull(wait) { awaitPointerEvent() }
                        if (event == null) {
                            held = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            k.onHold?.invoke()
                            continue
                        }
                        event.changes.forEach { it.consume() }
                        if (event.changes.all { !it.pressed }) break
                    }
                    interaction.tryEmit(PressInteraction.Release(press))
                    if (!held) click()
                }
            }
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick { click(); true }
                key.onHold?.let { hold -> onLongClick(holdLabel) { hold(); true } }
            },
    ) {
        GlassSurface(
            Modifier.fillMaxSize().padding(4.dp),
            shape = CircleShape,
            strong = key.kind != KeyKind.DIGIT,
            tint = if (key.kind == KeyKind.EQUALS) p.accentFill() else null,
            frost = 4.dp,
            shadowRadius = 8.dp,
            interactionSource = interaction,
        ) {
            KeyLabel(key, color)
        }
    }
}

/** Tiempo desde [start] según el reloj de los eventos táctiles. */
private fun currentEventUptime(start: Long): Long = android.os.SystemClock.uptimeMillis() - start

@Composable
private fun androidx.compose.foundation.layout.BoxScope.KeyLabel(key: CalcKey, color: androidx.compose.ui.graphics.Color) {
    val p = LocalGlass.current
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
    // Tecla baja (en horizontal): la segunda función va a la izquierda, en la misma línea.
    val side = key.shiftLabel != null && maxHeight < 64.dp
    val shifted = key.shiftLabel != null && !side
    if (side) {
        androidx.compose.foundation.layout.Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            val shiftStyle = TextStyle(color = p.accentFg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            if (key.math) MathText(key.shiftLabel!!, shiftStyle, Modifier, boxes = true)
            else Text(key.shiftLabel!!, color = p.accentFg, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            if (key.math) MathText(key.label, TextStyle(color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium), Modifier, boxes = true)
            else Text(key.label, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        return@BoxWithConstraints
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (shifted) {
            // Separada del borde de arriba: la píldora es redonda y ahí se angosta.
            val top = Modifier.align(Alignment.TopCenter).padding(top = 9.dp)
            if (key.math) {
                MathText(
                    key.shiftLabel,
                    TextStyle(color = p.accentFg, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    top,
                    boxes = true,
                )
            } else {
                Text(key.shiftLabel, top, color = p.accentFg, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        val below = Modifier.padding(top = if (shifted) 16.dp else 0.dp)
        when {
            key.math -> MathText(
                key.label,
                TextStyle(color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                below,
                boxes = true,
            )
            key.icon != null -> Icon(styled(key.icon), null, Modifier.size(26.dp), tint = color)
            else -> Text(
                key.label,
                below,
                color = color,
                fontSize = when (key.kind) {
                    KeyKind.DIGIT -> 28.sp
                    KeyKind.OPERATOR, KeyKind.EQUALS -> 30.sp
                    KeyKind.FUNCTION -> 17.sp
                    KeyKind.CLEAR -> 20.sp
                },
                fontWeight = if (key.kind == KeyKind.DIGIT) FontWeight.Normal else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
    }
}
