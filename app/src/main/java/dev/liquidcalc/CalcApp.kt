// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import dev.liquidcalc.data.Stores
import dev.liquidcalc.ui.glass.AppIcons
import dev.liquidcalc.ui.glass.SolidIcons

class CalcApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Stores.init(this)
        // Los íconos de la calculadora en su versión de líneas, para el estilo Sólido.
        SolidIcons.register(
            mapOf(
                Icons.AutoMirrored.Rounded.Backspace to Icons.AutoMirrored.Outlined.Backspace,
                Icons.Rounded.ContentCopy to Icons.Outlined.ContentCopy,
                Icons.Rounded.DeleteOutline to Icons.Outlined.DeleteOutline,
                Icons.Rounded.Functions to Icons.Outlined.Functions,
                Icons.Rounded.History to Icons.Outlined.History,
                Icons.Rounded.Settings to Icons.Outlined.Settings,
            ),
        )
    }

    companion object {
        /** Íconos de launcher: los cinco estilos de la suite, en el mismo orden (ver AppIcons). */
        val appIcons = AppIcons(
            "dev.liquidcalc",
            listOf(
                AppIcons.Icon("Liquid", "IconLiquid", R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground),
                AppIcons.Icon("Material", "IconMaterial", R.drawable.ic_launcher_material_background, R.drawable.ic_launcher_material_foreground),
                AppIcons.Icon("Samsung", "IconSamsung", R.drawable.ic_launcher_samsung_background, R.drawable.ic_launcher_samsung_foreground),
                AppIcons.Icon("iPhone", "IconIphone", R.drawable.ic_launcher_iphone_background, R.drawable.ic_launcher_iphone_foreground),
                AppIcons.Icon("Motorola", "IconMotorola", R.drawable.ic_launcher_motorola_background, R.drawable.ic_launcher_motorola_foreground),
            ),
        )
    }
}
