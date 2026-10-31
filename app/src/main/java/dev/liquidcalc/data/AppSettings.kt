// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.data

import dev.liquidcalc.core.AngleUnit
import dev.liquidcalc.ui.glass.LiquidDefaults
import dev.liquidcalc.ui.glass.LiquidPrefs
import dev.liquidcalc.ui.glass.LiquidSettings
import kotlinx.serialization.Serializable

/**
 * Ajustes de la calculadora, en `files/settings.json`. Los ocho primeros son los compartidos por
 * la suite (mismos nombres, valores y valores por defecto que en las otras apps, ver
 * [LiquidSettings]); el resto son propios de la calculadora.
 */
@Serializable
data class AppSettings(
    override val themeMode: Int = LiquidDefaults.THEME_MODE,
    override val backgroundMode: Int = LiquidDefaults.BACKGROUND_MODE,
    override val colorSet: Int = LiquidDefaults.COLOR_SET,
    override val accentColor: Long = LiquidDefaults.ACCENT_COLOR,
    override val animatedBackground: Boolean = LiquidDefaults.ANIMATED_BACKGROUND,
    override val uiStyle: Int = LiquidDefaults.UI_STYLE,
    override val solidAccent: Long = LiquidDefaults.SOLID_ACCENT,
    /** 0 = Liquid, el ícono propio de la calculadora. */
    override val appIcon: Int = 0,
    /** 0 = grados, 1 = radianes. */
    val angleUnit: Int = 0,
    /** Teclas científicas a la vista. */
    val scientific: Boolean = false,
    /** Guardar las cuentas en el historial (solo en el celu). */
    val historyEnabled: Boolean = true,
) : LiquidSettings {
    val angle: AngleUnit get() = if (angleUnit == 1) AngleUnit.RADIANS else AngleUnit.DEGREES

    /** Estos ajustes con los compartidos de [p] (lo que cambia la tarjeta de Apariencia). */
    fun with(p: LiquidPrefs) = copy(
        themeMode = p.themeMode,
        backgroundMode = p.backgroundMode,
        colorSet = p.colorSet,
        accentColor = p.accentColor,
        animatedBackground = p.animatedBackground,
        uiStyle = p.uiStyle,
        solidAccent = p.solidAccent,
        appIcon = p.appIcon,
    )
}
