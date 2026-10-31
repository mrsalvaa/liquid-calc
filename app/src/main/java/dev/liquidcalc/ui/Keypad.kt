// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import androidx.annotation.StringRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquidcalc.ui.glass.GlassSurface
import dev.liquidcalc.ui.glass.styled
import dev.liquidcalc.ui.glass.LocalGlass
import dev.liquidcalc.ui.glass.accentFill
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick

/** Cómo se ve una tecla: números, operadores, funciones, "=" o borrar. */
enum class KeyKind { DIGIT, OPERATOR, FUNCTION, EQUALS, CLEAR }

/**
 * Una tecla. [spoken] es lo que lee TalkBack ("dividido", "seno"); si es null, lee [label].
 * [icon], si está, reemplaza al texto (borrar). [onHold] se ejecuta si se mantiene apretada
 * [HOLD_MS] (borrar: todo); [holdLabel] es cómo lo anuncia TalkBack.
 */
class CalcKey(
    val label: String,
    @StringRes val spoken: Int?,
    val kind: KeyKind,
    val icon: ImageVector? = null,
    @StringRes val holdLabel: Int? = null,
    val onHold: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

/** Cuánto hay que mantener apretada una tecla para su acción larga. */
const val HOLD_MS = 700L

/** Teclas en filas iguales que ocupan todo el lugar disponible. */
@Composable
fun KeyGrid(rows: List<List<CalcKey>>, modifier: Modifier = Modifier) {
    Column(modifier) {
        rows.forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { key -> KeyButton(key, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

/**
 * Tecla de cristal con forma de píldora (en el estilo Sólido, un rectángulo de esquinas chicas).
 * El "=" va relleno con el color de énfasis; los operadores, con el texto en ese color.
 */
@Composable
fun KeyButton(key: CalcKey, modifier: Modifier = Modifier) {
    val p = LocalGlass.current
    val haptics = LocalHapticFeedback.current
    val spoken = key.spoken?.let { stringResource(it) } ?: key.label
    // Si se mantuvo apretada hasta la acción larga, al soltar no se hace también la corta.
    var held by remember { mutableStateOf(false) }
    val press = {
        if (held) {
            held = false
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            key.onClick()
        }
    }
    val hold = key.onHold
    val holdLabel = key.holdLabel?.let { stringResource(it) }
    val color = when (key.kind) {
        KeyKind.DIGIT, KeyKind.FUNCTION -> p.content
        KeyKind.OPERATOR, KeyKind.CLEAR -> p.accentFg
        KeyKind.EQUALS -> p.onAccent
    }
    GlassSurface(
        modifier
            .padding(4.dp)
            .then(
                if (hold == null) {
                    Modifier
                } else {
                    // Mantenerla [HOLD_MS]: la acción larga apenas se cumple el tiempo, sin esperar a soltar.
                    Modifier.pointerInput(hold) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            held = false
                            // null solo si se cumple el tiempo (soltar o cancelar terminan antes con true;
                            // waitForUpOrCancellation da null también si el botón ya usó el evento).
                            val released = withTimeoutOrNull(HOLD_MS) {
                                waitForUpOrCancellation()
                                true
                            }
                            if (released == null) {
                                held = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                hold()
                            }
                        }
                    }
                },
            )
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick { press(); true }
                if (hold != null) onLongClick(holdLabel) { hold(); true }
            },
        shape = CircleShape,
        strong = key.kind != KeyKind.DIGIT,
        tint = if (key.kind == KeyKind.EQUALS) p.accentFill() else null,
        frost = 4.dp,
        shadowRadius = 8.dp,
        onClick = press,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (key.icon != null) {
                Icon(styled(key.icon), null, Modifier.size(26.dp), tint = color)
            } else {
                Text(
                    key.label,
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
