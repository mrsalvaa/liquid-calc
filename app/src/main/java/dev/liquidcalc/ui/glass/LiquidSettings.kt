// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui.glass


/**
 * Los ajustes de apariencia compartidos por la suite. Cada app guarda los suyos en
 * `files/settings.json` con estos mismos nombres y valores, al primer nivel del JSON, junto con
 * sus campos propios: su clase de ajustes implementa esta interfaz.
 *
 * Los valores por defecto están en [LiquidDefaults]; `appIcon` lo elige cada app.
 */
interface LiquidSettings {
    /** 0 = según el sistema, 1 = claro, 2 = oscuro. */
    val themeMode: Int
    /** Liquid Glass: 0 = colores en movimiento (COLOR_SETS), 1 = color de énfasis, 2 = liso. */
    val backgroundMode: Int
    /** Índice en COLOR_SETS, para backgroundMode = 0. */
    val colorSet: Int
    /** Color de énfasis de Liquid Glass en ARGB, para backgroundMode = 1. */
    val accentColor: Long
    /** Fondo en movimiento animado (backgroundMode = 0). */
    val animatedBackground: Boolean
    /** Estilo de interfaz: 0 = Liquid Glass, 1 = Sólido. */
    val uiStyle: Int
    /** Color de énfasis del estilo Sólido en ARGB (aparte del de Liquid Glass). */
    val solidAccent: Long
    /** Ícono de launcher: 0 Liquid, 1 Material, 2 Samsung, 3 iPhone, 4 Motorola. */
    val appIcon: Int

    /** Lo que decide el aspecto del cristal. */
    val glassStyle: GlassStyle get() = GlassStyle(themeMode, backgroundMode, colorSet, accentColor, uiStyle, solidAccent)
}

/** Valores por defecto de la suite (los de Liquid Calendar). */
object LiquidDefaults {
    const val THEME_MODE = 0
    const val BACKGROUND_MODE = 2
    const val COLOR_SET = 0
    const val ACCENT_COLOR = 0xFF0EA5E9
    const val ANIMATED_BACKGROUND = true
    const val UI_STYLE = 0
    const val SOLID_ACCENT = 0xFF0EA5E9

    /** Nombres de los campos en el JSON, para comprobar que una app los respete. */
    val KEYS = listOf(
        "themeMode", "backgroundMode", "colorSet", "accentColor", "animatedBackground", "uiStyle", "solidAccent", "appIcon",
    )
}

/** Una copia de los ajustes compartidos, para editarlos desde [AppearanceCard]. */
data class LiquidPrefs(
    override val themeMode: Int,
    override val backgroundMode: Int,
    override val colorSet: Int,
    override val accentColor: Long,
    override val animatedBackground: Boolean,
    override val uiStyle: Int,
    override val solidAccent: Long,
    override val appIcon: Int,
) : LiquidSettings

fun LiquidSettings.toPrefs() = LiquidPrefs(
    themeMode, backgroundMode, colorSet, accentColor, animatedBackground, uiStyle, solidAccent, appIcon,
)
