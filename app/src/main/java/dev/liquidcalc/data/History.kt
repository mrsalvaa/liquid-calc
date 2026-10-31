// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.data

import kotlinx.serialization.Serializable

/**
 * Una cuenta del historial, en `files/history.json`. [value] es el resultado completo (34
 * cifras, sin redondear para mostrar), así reutilizarlo no pierde precisión.
 */
@Serializable
data class HistoryItem(
    val expression: String,
    val value: String,
    /** Milisegundos desde 1970. */
    val time: Long,
)

object History {
    /** Las más viejas se descartan: no tiene sentido guardar miles de cuentas. */
    const val MAX_ITEMS = 200

    fun add(item: HistoryItem) {
        if (!Stores.settings.value.historyEnabled) return
        Stores.history.update { (listOf(item) + it).take(MAX_ITEMS) }
    }

    fun clear() {
        Stores.history.update { emptyList() }
    }
}
