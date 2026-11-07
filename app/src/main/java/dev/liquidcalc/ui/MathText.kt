// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquidcalc.ui.Editor.Companion.ABS
import dev.liquidcalc.ui.Editor.Companion.END
import dev.liquidcalc.ui.Editor.Companion.FRAC
import dev.liquidcalc.ui.Editor.Companion.OPENERS
import dev.liquidcalc.ui.Editor.Companion.POW
import dev.liquidcalc.ui.Editor.Companion.ROOT
import dev.liquidcalc.ui.Editor.Companion.SEP
import dev.liquidcalc.ui.Editor.Companion.SQRT
import kotlin.math.max
import kotlin.math.roundToInt

/** Separador de miles del idioma (punto en español, coma en inglés). */
val LocalGrouping = staticCompositionLocalOf { '.' }

// --- Árbol --------------------------------------------------------------------------------------

/** Lo que se dibuja: texto, una estructura con casillas o una fila. [start]/[end] son índices de la expresión. */
private sealed interface MathNode

private class Glyph(val text: String, val start: Int, val end: Int, val italic: Boolean = false) : MathNode

private class Structure(val kind: Char, val open: Int, val slots: List<MathRow>, val end: Int) : MathNode

private class MathRow(val items: List<MathNode>, val start: Int, val end: Int)

/** Elementos de varios caracteres que se ven distinto: sin⁻¹( en vez de asin(. */
private val WORDS = listOf(
    "asin(" to "sin⁻¹(", "acos(" to "cos⁻¹(", "atan(" to "tan⁻¹(",
    "sin(" to "sin(", "cos(" to "cos(", "tan(" to "tan(", "ln(" to "ln(", "log(" to "log(",
    "exp(" to "exp(", "abs(" to "Abs(", Editor.ANS to "Ans",
)

private class MathParser(val text: String, val decimal: Char, val grouping: Char) {
    var i = 0

    /** Una fila hasta [SEP] o [END] (que no consume). */
    fun row(): MathRow {
        val start = i
        val items = mutableListOf<MathNode>()
        while (i < text.length) {
            val c = text[i]
            when {
                c == SEP || c == END -> break
                c in OPENERS -> items += structure()
                c.isDigit() || c == '.' || c == ',' -> number(items)
                else -> {
                    val word = WORDS.firstOrNull { text.startsWith(it.first, i) }
                    if (word != null) {
                        items += Glyph(word.second, i, i + word.first.length)
                        i += word.first.length
                    } else {
                        val shown = when (c) {
                            '^' -> "^"
                            else -> c.toString()
                        }
                        items += Glyph(shown, i, i + 1, italic = c == 'e')
                        i++
                    }
                }
            }
        }
        return MathRow(items, start, i)
    }

    /** Un número, un glifo por cifra; antes de cada grupo de tres cifras, el separador de miles. */
    private fun number(items: MutableList<MathNode>) {
        val start = i
        while (i < text.length && (text[i].isDigit() || text[i] == '.' || text[i] == ',')) i++
        val literal = text.substring(start, i)
        val integerDigits = literal.indexOfFirst { !it.isDigit() }.let { if (it < 0) literal.length else it }
        literal.forEachIndexed { k, c ->
            val group = k in 1 until integerDigits && (integerDigits - k) % 3 == 0
            items += Glyph(if (group) "$grouping$c" else c.toString(), start + k, start + k + 1)
        }
    }

    private fun structure(): Structure {
        val kind = text[i]
        val open = i
        i++
        val slots = mutableListOf(row())
        while (i < text.length && text[i] == SEP) {
            i++
            slots += row()
        }
        val end = i
        if (i < text.length) i++ // END
        return Structure(kind, open, slots, end)
    }
}

// --- Dibujo -------------------------------------------------------------------------------------

/** Lo que comparte todo el dibujo de una expresión. */
private class MathContext(
    val style: TextStyle,
    val cursor: Int?,
    val onTap: ((Int) -> Unit)?,
    /** Etiqueta de tecla: las casillas vacías son cuadraditos lisos y más chicos. */
    val boxes: Boolean = false,
)


/**
 * Una expresión en escritura natural: potencias arriba y más chicas, fracciones apiladas, raíces
 * con la barra encima. Con [cursor], lo dibuja titilando; con [onTap], tocar un carácter mueve el
 * cursor a su izquierda o su derecha (según la mitad que se toque).
 */
@Composable
fun MathText(
    expression: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    cursor: Int? = null,
    onTap: ((Int) -> Unit)? = null,
    boxes: Boolean = false,
) {
    val grouping = LocalGrouping.current
    val decimal = if (grouping == ',') '.' else ','
    val root = remember(expression, grouping) { MathParser(expression, decimal, grouping).row() }
    val ctx = MathContext(style, cursor, onTap, boxes)
    BaselineRow(modifier) { RowContent(root, ctx, style.fontSize, placeholder = false) }
}

/** Fila que apoya todo en la misma línea base (el exponente arriba, la fracción centrada). */
@Composable
private fun BaselineRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, _ ->
        val placeables = measurables.map { it.measure(Constraints()) }
        fun baseline(p: androidx.compose.ui.layout.Placeable) =
            p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: (p.height * 0.8f).roundToInt()
        val above = placeables.maxOfOrNull { baseline(it) } ?: 0
        val below = placeables.maxOfOrNull { it.height - baseline(it) } ?: 0
        var x = 0
        layout(placeables.sumOf { it.width }, above + below, mapOf(FirstBaseline to above)) {
            placeables.forEach {
                it.place(x, above - baseline(it))
                x += it.width
            }
        }
    }
}

/**
 * [MathText] que se achica para entrar en el ancho (de [maxSize] hasta [minSize]); si aun así
 * no entra, se desliza de costado y muestra el final.
 */
@Composable
fun FitMathText(
    expression: String,
    style: TextStyle,
    maxSize: TextUnit,
    minSize: TextUnit,
    modifier: Modifier = Modifier,
    cursor: Int? = null,
    onTap: ((Int) -> Unit)? = null,
    scroll: ScrollState = rememberScrollState(),
    maxHeightFraction: Float = 1f,
) {
    LaunchedEffect(expression, cursor) {
        if (cursor == null || cursor >= expression.length) scroll.scrollTo(scroll.maxValue)
    }
    SubcomposeLayout(modifier) { constraints ->
        val probe = subcompose("probe") {
            MathText(expression, style.copy(fontSize = maxSize))
        }.map { it.measure(Constraints()) }
        val natural = probe.maxOfOrNull { it.width } ?: 0
        val naturalHeight = probe.maxOfOrNull { it.height } ?: 0
        val available = constraints.maxWidth
        val byWidth = if (natural <= available || natural == 0) 1f else available.toFloat() / natural
        // Alto: que deje lugar abajo (el resultado parcial), sin pasar de [maxHeightFraction].
        val roomHeight = if (constraints.hasBoundedHeight) constraints.maxHeight * maxHeightFraction else Float.MAX_VALUE
        val byHeight = if (naturalHeight <= roomHeight || naturalHeight == 0) 1f else roomHeight / naturalHeight
        val ratio = minOf(byWidth, byHeight)
        val size = (maxSize.value * ratio).coerceAtLeast(minSize.value).sp
        val real = subcompose("real") {
            Row(Modifier.horizontalScroll(scroll)) {
                MathText(expression, style.copy(fontSize = size), cursor = cursor, onTap = onTap)
            }
        }.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val w = real.maxOfOrNull { it.width } ?: 0
        val h = real.maxOfOrNull { it.height } ?: 0
        layout(w, h) { real.forEach { it.place(0, 0) } }
    }
}

@Composable
private fun RowContent(row: MathRow, ctx: MathContext, size: TextUnit, placeholder: Boolean) {
    if (row.items.isEmpty()) {
        // Casilla vacía: un cuadradito punteado que se puede tocar.
        if (placeholder) Placeholder(row.start, ctx, size)
        if (ctx.cursor == row.start) Cursor(ctx, size)
        return
    }
    row.items.forEachIndexed { k, item ->
        val start = when (item) {
            is Glyph -> item.start
            is Structure -> item.open
        }
        if (ctx.cursor == start) Cursor(ctx, size)
        when (item) {
            is Glyph -> GlyphText(item, ctx, size)
            is Structure -> StructureView(item, ctx, size)
        }
        if (k == row.items.lastIndex && ctx.cursor == row.end) Cursor(ctx, size)
    }
}

@Composable
private fun GlyphText(g: Glyph, ctx: MathContext, size: TextUnit) {
    BasicText(
        g.text,
        Modifier.then(
            if (ctx.onTap == null) {
                Modifier
            } else {
                Modifier.pointerInput(g.start, g.end) {
                    detectTapGestures { o -> ctx.onTap.invoke(if (o.x < this.size.width / 2) g.start else g.end) }
                }
            },
        ),
        style = ctx.style.copy(fontSize = size, fontStyle = if (g.italic) FontStyle.Italic else FontStyle.Normal),
        softWrap = false,
        maxLines = 1,
    )
}

@Composable
private fun Placeholder(index: Int, ctx: MathContext, size: TextUnit) {
    if (ctx.boxes) {
        KeyBox(ctx, size)
        return
    }
    val color = ctx.style.color.copy(alpha = 0.45f)
    Layout(
        modifier = Modifier
            .drawBehind {
                val inset = this.size.width * 0.12f
                drawRect(
                    color,
                    topLeft = Offset(inset, this.size.height * 0.2f),
                    size = androidx.compose.ui.geometry.Size(this.size.width - 2 * inset, this.size.height * 0.62f),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .then(
                if (ctx.onTap == null) Modifier else Modifier.pointerInput(index) { detectTapGestures { ctx.onTap.invoke(index) } },
            ),
    ) { _, _ ->
        val h = (size.toPx() * 1.15f).roundToInt()
        val w = (size.toPx() * 0.62f).roundToInt()
        layout(w, h, mapOf(FirstBaseline to (h * 0.82f).roundToInt())) {}
    }
}

/** Cuadradito de las etiquetas de tecla (□/□, √□): liso, de la mitad del alto de la letra. */
@Composable
private fun KeyBox(ctx: MathContext, size: TextUnit) {
    val color = ctx.style.color
    Layout(
        modifier = Modifier.drawBehind {
            val side = this.size.height * 0.5f
            val stroke = 1.3.dp.toPx()
            drawRect(
                color,
                topLeft = Offset((this.size.width - side) / 2, this.size.height * 0.3f),
                size = androidx.compose.ui.geometry.Size(side, side),
                style = Stroke(width = stroke),
            )
        },
    ) { _, _ ->
        val h = (size.toPx() * 0.95f).roundToInt()
        val w = (size.toPx() * 0.62f).roundToInt()
        layout(w, h, mapOf(FirstBaseline to (h * 0.8f).roundToInt())) {}
    }
}

/** Cursor de ancho cero: no mueve nada al cambiar de lugar. */
@Composable
private fun Cursor(ctx: MathContext, size: TextUnit) {
    val color = ctx.style.color
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        1f,
        0f,
        infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse),
        label = "blink",
    )
    Layout(
        modifier = Modifier.drawBehind {
            drawLine(
                color.copy(alpha = color.alpha * (if (blink > 0.5f) 1f else 0f)),
                Offset(0f, this.size.height * 0.1f),
                Offset(0f, this.size.height * 0.92f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        },
    ) { _, _ ->
        val h = (size.toPx() * 1.15f).roundToInt()
        layout(0, h, mapOf(FirstBaseline to (h * 0.82f).roundToInt())) {}
    }
}

@Composable
private fun StructureView(s: Structure, ctx: MathContext, size: TextUnit) {
    val tapStart = if (ctx.onTap == null) {
        Modifier
    } else {
        Modifier.pointerInput(s.open, s.end) {
            detectTapGestures { o -> ctx.onTap.invoke(if (o.x < this.size.width / 2) s.open else s.end + 1) }
        }
    }
    when (s.kind) {
        POW -> Power(s, ctx, size, tapStart)
        FRAC -> Fraction(s, ctx, size, tapStart)
        SQRT -> Radical(null, s.slots.getOrNull(0), ctx, size, tapStart)
        ROOT -> Radical(s.slots.getOrNull(0), s.slots.getOrNull(1), ctx, size, tapStart)
        ABS -> Absolute(s, ctx, size, tapStart)
    }
}

@Composable
private fun SlotRow(row: MathRow?, ctx: MathContext, size: TextUnit, modifier: Modifier = Modifier) {
    if (row == null) return
    BaselineRow(modifier) { RowContent(row, ctx, size, placeholder = true) }
}

private fun smaller(size: TextUnit, factor: Float) = (size.value * factor).coerceAtLeast(9f).sp

/** Exponente: más chico y con su base apoyada a media altura. */
@Composable
private fun Power(s: Structure, ctx: MathContext, size: TextUnit, modifier: Modifier) {
    Layout(content = { SlotRow(s.slots.firstOrNull(), ctx, smaller(size, 0.62f)) }, modifier = modifier) { m, _ ->
        val p = m.first().measure(Constraints())
        val raise = (size.toPx() * 0.42f).roundToInt()
        val base = p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: p.height
        val baseline = base + raise
        layout(p.width, max(p.height, baseline + (size.toPx() * 0.25f).roundToInt()), mapOf(FirstBaseline to baseline)) {
            p.place(0, 0)
        }
    }
}

/** Numerador sobre denominador, con la raya a la altura del signo menos. */
@Composable
private fun Fraction(s: Structure, ctx: MathContext, size: TextUnit, modifier: Modifier) {
    val color = ctx.style.color
    val inner = smaller(size, 0.82f)
    // Lo que mide el layout y usa el dibujo de la raya: y, grosor, margen de cada lado.
    val bar = remember { FloatArray(3) }
    Layout(
        content = {
            SlotRow(s.slots.getOrNull(0), ctx, inner)
            SlotRow(s.slots.getOrNull(1), ctx, inner)
        },
        modifier = modifier.drawBehind {
            drawLine(color, Offset(bar[2], bar[0]), Offset(this.size.width - bar[2], bar[0]), bar[1], StrokeCap.Round)
        },
    ) { m, _ ->
        val num = m[0].measure(Constraints())
        val den = m.getOrNull(1)?.measure(Constraints())
        // Margen afuera de la raya (que no toque al + de al lado) y un poco de raya de más.
        val margin = (size.toPx() * 0.1f).roundToInt()
        val pad = margin + (size.toPx() * 0.08f).roundToInt()
        val gap = (size.toPx() * 0.1f).roundToInt()
        // En las teclas, más gruesa: a 15 sp sería de un píxel.
        val thickness = if (ctx.boxes) 1.4.dp.toPx() else max(1f, size.toPx() * 0.045f)
        val width = max(num.width, den?.width ?: 0) + 2 * pad
        val barY = num.height + gap
        val denY = barY + thickness.roundToInt() + gap
        val height = denY + (den?.height ?: 0)
        bar[0] = barY + thickness / 2
        bar[1] = thickness
        bar[2] = margin.toFloat()
        val axis = (size.toPx() * 0.3f).roundToInt()
        layout(width, height, mapOf(FirstBaseline to barY + axis)) {
            num.place((width - num.width) / 2, 0)
            den?.place((width - den.width) / 2, denY)
        }
    }
}

/** √ con la barra encima del radicando; con [index], el número chico en el gancho. */
@Composable
private fun Radical(index: MathRow?, radicand: MathRow?, ctx: MathContext, size: TextUnit, modifier: Modifier) {
    val color = ctx.style.color
    // Lo que mide el layout y usa el dibujo: x del signo, y de arriba, ancho del signo, alto.
    val geo = remember { FloatArray(4) }
    Layout(
        content = {
            SlotRow(radicand, ctx, size)
            if (index != null) SlotRow(index, ctx, smaller(size, 0.5f))
        },
        modifier = modifier.drawBehind {
            val (x, top, sign, h) = geo
            val stroke = max(1.5f, size.toPx() * 0.05f)
            val path = Path().apply {
                moveTo(x, top + h * 0.58f)
                lineTo(x + sign * 0.25f, top + h * 0.5f)
                lineTo(x + sign * 0.55f, top + h * 0.96f)
                lineTo(x + sign, top + stroke / 2)
                lineTo(this@drawBehind.size.width, top + stroke / 2)
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        },
    ) { m, _ ->
        val body = m[0].measure(Constraints())
        val idx = m.getOrNull(1)?.measure(Constraints())
        val sign = (size.toPx() * 0.55f).roundToInt()
        val gap = (size.toPx() * 0.14f).roundToInt()
        val pad = (size.toPx() * 0.08f).roundToInt()
        val bodyH = body.height + gap
        // El índice va arriba a la izquierda, apoyado sobre el gancho del √.
        val hook = (bodyH * 0.45f).roundToInt()
        val signX = idx?.let { max(0, it.width - (sign * 0.4f).roundToInt()) } ?: 0
        val shift = idx?.let { max(0, it.height - hook) } ?: 0
        val width = signX + sign + body.width + pad
        val height = shift + bodyH
        val bodyBaseline = body[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: body.height
        geo[0] = signX.toFloat(); geo[1] = shift.toFloat(); geo[2] = sign.toFloat(); geo[3] = bodyH.toFloat()
        layout(width, height, mapOf(FirstBaseline to shift + gap + bodyBaseline)) {
            body.place(signX + sign, shift + gap)
            idx?.place(0, shift + hook - idx.height)
        }
    }
}

/** Contenido entre barras verticales. */
@Composable
private fun Absolute(s: Structure, ctx: MathContext, size: TextUnit, modifier: Modifier) {
    val color = ctx.style.color
    SlotRow(
        s.slots.firstOrNull(),
        ctx,
        size,
        modifier
            .drawBehind {
                val w = 1.6.dp.toPx()
                val inset = w
                drawLine(color, Offset(inset, this.size.height * 0.08f), Offset(inset, this.size.height * 0.92f), w, StrokeCap.Round)
                drawLine(color, Offset(this.size.width - inset, this.size.height * 0.08f), Offset(this.size.width - inset, this.size.height * 0.92f), w, StrokeCap.Round)
            }
            .padding(horizontal = (size.value * 0.22f).dp),
    )
}
