// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui.glass

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes

/**
 * Íconos de launcher elegibles, cada uno un activity-alias del manifest de la app. El orden es
 * el mismo en toda la suite, así el valor de `appIcon` significa lo mismo en cada app:
 * 0 Liquid, 1 Material, 2 Samsung, 3 iPhone, 4 Motorola ([STYLES]).
 *
 * [namespace] es el paquete de las clases (el namespace de Gradle), no el applicationId: en la
 * build debug el applicationId lleva ".debug" pero los alias siguen en el mismo paquete.
 */
class AppIcons(private val namespace: String, val all: List<Icon>) {
    data class Icon(val name: String, val alias: String, @DrawableRes val background: Int, @DrawableRes val foreground: Int)

    /** Activa el alias elegido y desactiva el resto (primero activa, para no quedarse sin ícono). */
    fun apply(context: Context, index: Int) {
        val pm = context.packageManager
        fun set(icon: Icon, enabled: Boolean) = pm.setComponentEnabledSetting(
            ComponentName(context.packageName, "$namespace.${icon.alias}"),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        val chosen = all.getOrElse(index) { all[0] }
        set(chosen, true)
        all.filter { it != chosen }.forEach { set(it, false) }
    }

    companion object {
        /** Nombre y alias de cada estilo, en el orden compartido por la suite. */
        val STYLES = listOf(
            "Liquid" to "IconLiquid",
            "Material" to "IconMaterial",
            "Samsung" to "IconSamsung",
            "iPhone" to "IconIphone",
            "Motorola" to "IconMotorola",
        )
    }
}
