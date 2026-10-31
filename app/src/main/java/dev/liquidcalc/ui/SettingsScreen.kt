// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquidcalc.CalcApp
import dev.liquidcalc.R
import dev.liquidcalc.data.AppSettings
import dev.liquidcalc.data.History
import dev.liquidcalc.data.Stores
import dev.liquidcalc.ui.glass.BackTitle
import dev.liquidcalc.ui.glass.ChipRow
import dev.liquidcalc.ui.glass.GlassCard
import dev.liquidcalc.ui.glass.GlassSwitch
import dev.liquidcalc.ui.glass.SectionLabel
import dev.liquidcalc.ui.glass.SettingRow
import dev.liquidcalc.ui.glass.AppearanceCard
import dev.liquidcalc.ui.glass.LocalGlass

@Composable
fun SettingsScreen(s: AppSettings, onBack: () -> Unit) {
    val p = LocalGlass.current
    val context = LocalContext.current
    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        BackTitle(stringResource(R.string.settings), onBack)

        SectionLabel(stringResource(R.string.calculator_section))
        GlassCard {
            Text(stringResource(R.string.angle_unit), Modifier.padding(top = 4.dp), fontSize = 16.sp)
            ChipRow(listOf(0, 1), s.angleUnit, { stringResource(if (it == 0) R.string.degrees else R.string.radians) }) { v ->
                Stores.settings.update { it.copy(angleUnit = v) }
            }
            SettingRow(
                stringResource(R.string.scientific),
                trailing = { GlassSwitch(s.scientific, { v -> Stores.settings.update { it.copy(scientific = v) } }) },
            )
            SettingRow(
                stringResource(R.string.save_history),
                subtitle = stringResource(R.string.save_history_hint),
                trailing = {
                    GlassSwitch(s.historyEnabled, { v ->
                        // Desactivarlo también borra lo guardado: nada queda en el celu sin querer.
                        if (!v) History.clear()
                        Stores.settings.update { it.copy(historyEnabled = v) }
                    })
                },
            )
        }

        SectionLabel(stringResource(R.string.appearance))
        AppearanceCard(s, CalcApp.appIcons) { prefs -> Stores.settings.update { it.with(prefs) } }

        SectionLabel(stringResource(R.string.keyboard))
        GlassCard {
            Text(stringResource(R.string.keyboard_hint), fontSize = 14.sp, color = p.contentSecondary)
        }

        SectionLabel(stringResource(R.string.about))
        GlassCard {
            Text(stringResource(R.string.about_text, version), fontSize = 14.sp, color = p.contentSecondary)
        }
    }
}
