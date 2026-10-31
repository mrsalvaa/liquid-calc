// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.util

import android.app.Activity

/** Pide la frecuencia de refresco más alta de la pantalla (las animaciones del cristal lo notan). */
fun Activity.preferHighestRefreshRate() {
    val display = display ?: return
    val current = display.mode
    val best = display.supportedModes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .maxByOrNull { it.refreshRate } ?: return
    if (best.modeId != window.attributes.preferredDisplayModeId) {
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
}
