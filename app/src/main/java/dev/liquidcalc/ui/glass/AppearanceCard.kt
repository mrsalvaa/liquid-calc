// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.stringResource
import dev.liquidcalc.R

private fun Color.argb(): Long = toArgb().toLong() and 0xFFFFFFFFL

/**
 * Tarjeta de Apariencia compartida: estilo de interfaz, tema, fondo o color de énfasis según el
 * estilo, e ícono de la app. Cada cambio llega a [onChange] con todos los ajustes compartidos.
 */
@Composable
fun AppearanceCard(s: LiquidSettings, appIcons: AppIcons, onChange: (LiquidPrefs) -> Unit) {
    val prefs = s.toPrefs()
    val context = LocalContext.current
    val p = LocalGlass.current
    GlassCard {
        Label(stringResource(R.string.liquid_ui_style))
        ChipRow(listOf(0, 1), s.uiStyle, { stringResource(listOf(R.string.liquid_ui_style_liquid, R.string.liquid_ui_style_solid)[it]) }) { v ->
            onChange(prefs.copy(uiStyle = v))
        }
        if (s.uiStyle == 1) {
            Text(
                stringResource(R.string.liquid_ui_style_solid_hint),
                Modifier.padding(top = 4.dp),
                fontSize = 13.sp,
                color = p.contentSecondary,
            )
        }

        Label(stringResource(R.string.liquid_theme))
        ChipRow(listOf(0, 1, 2), s.themeMode, { stringResource(listOf(R.string.liquid_theme_system, R.string.liquid_theme_light, R.string.liquid_theme_dark)[it]) }) { v ->
            onChange(prefs.copy(themeMode = v))
        }

        if (s.uiStyle == 1) {
            // Sólido: color de énfasis propio (no pisa el de Liquid Glass).
            Label(stringResource(R.string.liquid_background_accent))
            AccentPicker(s.solidAccent) { c -> onChange(prefs.copy(solidAccent = c)) }
        } else {
            LiquidBackgroundOptions(s, onChange)
        }

        Label(stringResource(R.string.liquid_app_icon))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            appIcons.all.forEachIndexed { i, icon ->
                IconChoice(icon, selected = s.appIcon == i) {
                    if (s.appIcon != i) {
                        onChange(prefs.copy(appIcon = i))
                        appIcons.apply(context, i)
                    }
                }
            }
        }
        Text(
            stringResource(R.string.liquid_app_icon_hint),
            fontSize = 12.sp,
            color = p.contentSecondary,
        )
    }
}

/** Opciones de fondo de Liquid Glass: liso, en movimiento (con sets de colores) o color de énfasis. */
@Composable
private fun LiquidBackgroundOptions(s: LiquidSettings, onChange: (LiquidPrefs) -> Unit) {
    val prefs = s.toPrefs()
    val p = LocalGlass.current
    Label(stringResource(R.string.liquid_background))
    // Como en Liquid Clock: 2 = liso (el predeterminado), 0 = en movimiento, 1 = color de énfasis.
    ChipRow(
        listOf(2, 0, 1),
        s.backgroundMode,
        { stringResource(listOf(R.string.liquid_background_moving, R.string.liquid_background_accent, R.string.liquid_background_plain)[it]) },
    ) { v ->
        onChange(prefs.copy(backgroundMode = v))
    }

    if (s.backgroundMode == 2) {
        Text(
            stringResource(R.string.liquid_background_plain_hint),
            Modifier.padding(top = 4.dp),
            fontSize = 13.sp,
            color = p.contentSecondary,
        )
    } else if (s.backgroundMode == 0) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            COLOR_SETS.forEachIndexed { i, set ->
                val blobs = if (p.dark) set.darkBlobs else set.lightBlobs
                Swatch(
                    brush = Brush.linearGradient(listOf(blobs[0], blobs[1])),
                    selected = s.colorSet == i,
                    label = stringResource(set.name),
                    modifier = Modifier.weight(1f),
                ) { onChange(prefs.copy(colorSet = i)) }
            }
        }
        val reduced = LocalReducedMotion.current
        SettingRow(
            stringResource(R.string.liquid_animated_background),
            subtitle = stringResource(if (reduced) R.string.liquid_animated_background_reduced else R.string.liquid_animated_background_hint),
        ) {
            GlassSwitch(s.animatedBackground, { v -> onChange(prefs.copy(animatedBackground = v)) })
        }
    } else {
        AccentPicker(s.accentColor) { c -> onChange(prefs.copy(accentColor = c)) }
    }
}

/** Los 10 colores de énfasis (dos filas de 5) y sus tres tonos. */
@Composable
private fun AccentPicker(value: Long, onPick: (Long) -> Unit) {
    val hueIndex = ACCENT_HUES.indexOfFirst { h -> h.tones.any { it.argb() == value } }.coerceAtLeast(0)
    ACCENT_HUES.chunked(5).forEachIndexed { row, hues ->
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            hues.forEachIndexed { col, hue ->
                val i = row * 5 + col
                Swatch(
                    brush = Brush.linearGradient(listOf(muted(hue.tones[0]), muted(hue.tones[2]))),
                    selected = i == hueIndex,
                    label = stringResource(hue.name),
                    modifier = Modifier.weight(1f),
                ) {
                    // Al cambiar de color se queda con el mismo tono (claro, medio u oscuro).
                    val tone = ACCENT_HUES[hueIndex].tones.indexOfFirst { it.argb() == value }.coerceAtLeast(1)
                    onPick(hue.tones[tone].argb())
                }
            }
        }
    }
    Label(stringResource(R.string.liquid_tone))
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ACCENT_HUES[hueIndex].tones.forEachIndexed { t, tone ->
            Swatch(
                brush = Brush.linearGradient(listOf(muted(tone), muted(tone))),
                selected = tone.argb() == value,
                label = stringResource(listOf(R.string.liquid_tone_light, R.string.liquid_tone_medium, R.string.liquid_tone_dark)[t]),
                modifier = Modifier.width(62.dp),
            ) { onPick(tone.argb()) }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(top = 10.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun Swatch(
    brush: Brush,
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val p = LocalGlass.current
    Column(
        modifier.selectable(selected, remember { MutableInteractionSource() }, null, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(50.dp)
                .border(2.dp, if (selected) p.content else Color.Transparent, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(brush),
        )
        Text(
            label,
            Modifier.padding(top = 6.dp),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            color = if (selected) p.content else p.contentSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Vista previa de un ícono adaptativo: fondo y primer plano recortados en forma de "squircle". */
@Composable
private fun IconChoice(icon: AppIcons.Icon, selected: Boolean, onClick: () -> Unit) {
    val p = LocalGlass.current
    val shape: Shape = RoundedCornerShape(30)
    Column(
        Modifier
            .width(66.dp)
            .selectable(selected, remember { MutableInteractionSource() }, null, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .border(2.dp, if (selected) p.content else Color.Transparent, RoundedCornerShape(32))
                .padding(4.dp)
                .clip(shape),
        ) {
            // El icono adaptativo mide 108 y se ven los 72 centrales: se amplía 1,5 veces.
            val zoom = Modifier.matchParentSize().graphicsLayer {
                scaleX = 1.5f
                scaleY = 1.5f
            }
            Image(painterResource(icon.background), null, zoom)
            Image(painterResource(icon.foreground), null, zoom)
        }
        Text(
            icon.name,
            Modifier.padding(top = 6.dp),
            fontSize = 12.sp,
            color = if (selected) p.content else p.contentSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
