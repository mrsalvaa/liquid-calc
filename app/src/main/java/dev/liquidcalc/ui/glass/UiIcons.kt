// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui.glass

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Íconos de interfaz (Material Icons, Apache-2.0). Las apps los escriben en su versión Rounded,
 * la de Liquid Glass; en el estilo Sólido se dibuja la versión Outlined, de líneas finas.
 *
 * Cada app registra al arrancar los pares de los íconos que usa con [register]: así el mapa
 * solo contiene íconos que existen en el APK (R8 descarta el resto). Un ícono sin par se dibuja
 * igual en los dos estilos.
 */
object SolidIcons {
    private val map = mutableMapOf<ImageVector, ImageVector>(
        // Los que usan los propios componentes de ui/glass.
        Icons.AutoMirrored.Rounded.ArrowBack to Icons.AutoMirrored.Outlined.ArrowBack,
    )

    @Synchronized
    fun register(pairs: Map<ImageVector, ImageVector>) {
        map += pairs
    }

    @Synchronized
    internal fun outlined(icon: ImageVector): ImageVector = map[icon] ?: icon
}

/** El ícono que corresponde al estilo actual. */
@Composable
fun styled(icon: ImageVector): ImageVector =
    if (LocalGlass.current.solid) SolidIcons.outlined(icon) else icon
