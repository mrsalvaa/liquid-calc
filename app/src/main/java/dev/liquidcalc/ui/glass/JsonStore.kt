// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui.glass

import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Formato de los JSON de la suite: los campos que una app no conoce se ignoran y los valores por
 * defecto también se escriben (así el archivo siempre tiene todos los campos).
 */
val LiquidJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Estado observable persistido como JSON en un archivo (en la carpeta privada de la app). */
class JsonStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    default: T,
) {
    private val _state = MutableStateFlow(load(default))
    val state: StateFlow<T> = _state.asStateFlow()
    val value: T get() = _state.value

    private fun load(default: T): T = try {
        if (file.exists()) LiquidJson.decodeFromString(serializer, file.readText()) else default
    } catch (e: Exception) {
        default
    }

    @Synchronized
    fun update(transform: (T) -> T): T {
        val new = transform(_state.value)
        _state.value = new
        val atomic = AtomicFile(file)
        val out = atomic.startWrite()
        try {
            out.write(LiquidJson.encodeToString(serializer, new).toByteArray())
            atomic.finishWrite(out)
        } catch (e: Exception) {
            atomic.failWrite(out)
        }
        return new
    }
}
