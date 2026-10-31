// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.data

import android.content.Context
import dev.liquidui.settings.JsonStore
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/** Todo lo que se guarda, en la carpeta privada de la app (sin backup en la nube). */
object Stores {
    lateinit var settings: JsonStore<AppSettings>
    lateinit var history: JsonStore<List<HistoryItem>>

    @Synchronized
    fun init(context: Context) {
        if (::settings.isInitialized) return
        val dir = context.filesDir
        settings = JsonStore(File(dir, "settings.json"), AppSettings.serializer(), AppSettings())
        history = JsonStore(File(dir, "history.json"), ListSerializer(HistoryItem.serializer()), emptyList())
    }
}
