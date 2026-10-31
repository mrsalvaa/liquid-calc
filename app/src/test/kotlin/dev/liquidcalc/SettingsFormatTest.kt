// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc

import dev.liquidcalc.data.AppSettings
import dev.liquidui.settings.LiquidDefaults
import dev.liquidui.settings.LiquidJson
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** settings.json tiene que usar los mismos campos y valores por defecto que el resto de la suite. */
class SettingsFormatTest {
    private val json = LiquidJson.parseToJsonElement(LiquidJson.encodeToString(AppSettings.serializer(), AppSettings())).jsonObject

    @Test fun sharedKeysArePresent() {
        for (key in LiquidDefaults.KEYS) assertTrue(key in json, "falta \"$key\" en settings.json")
    }

    @Test fun sharedDefaults() {
        assertEquals("0", json["themeMode"].toString())
        assertEquals("2", json["backgroundMode"].toString())
        assertEquals("0", json["colorSet"].toString())
        assertEquals(0xFF0EA5E9.toString(), json["accentColor"].toString())
        assertEquals("true", json["animatedBackground"].toString())
        assertEquals("0", json["uiStyle"].toString())
        assertEquals(0xFF0EA5E9.toString(), json["solidAccent"].toString())
        assertEquals("0", json["appIcon"].toString())
    }

    @Test fun readsSettingsFromAnotherApp() {
        // Un settings.json de Liquid Calendar (con campos que la calculadora no conoce).
        val calendar = """{"themeMode":2,"backgroundMode":1,"colorSet":3,"accentColor":4294626325,"animatedBackground":false,"uiStyle":1,"solidAccent":4279150057,"appIcon":4,"firstDayOfWeek":1}"""
        val s = LiquidJson.decodeFromString(AppSettings.serializer(), calendar)
        assertEquals(2, s.themeMode)
        assertEquals(1, s.uiStyle)
        assertEquals(4, s.appIcon)
        assertEquals(4294626325, s.accentColor)
    }
}
