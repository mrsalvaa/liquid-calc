// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.liquidcalc.R
import dev.liquidcalc.core.Format
import dev.liquidcalc.data.History
import dev.liquidcalc.data.HistoryItem
import dev.liquidcalc.data.Stores
import dev.liquidcalc.ui.glass.DialogSpec
import dev.liquidcalc.ui.glass.EmptyState
import dev.liquidcalc.ui.glass.GlassButton
import dev.liquidcalc.ui.glass.GlassIconButton
import dev.liquidcalc.ui.glass.GlassSurface
import dev.liquidcalc.ui.glass.LocalDialog
import dev.liquidcalc.ui.glass.LocalToast
import dev.liquidcalc.ui.glass.LocalGlass
import dev.liquidcalc.ui.glass.LocalReducedMotion
import java.math.BigDecimal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Historial como panel de cristal desde abajo (refracta la calculadora). Tocar una cuenta la
 * vuelve a poner en la pantalla para usarla o cambiarla.
 */
@Composable
fun BoxScope.HistoryPanel(visible: Boolean, enabled: Boolean, onClose: () -> Unit) {
    val reduced = LocalReducedMotion.current
    BackHandler(enabled = visible, onBack = onClose)

    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
        )
    }
    AnimatedVisibility(
        visible,
        Modifier.align(Alignment.BottomCenter),
        enter = if (reduced) fadeIn() else slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { it } + fadeIn(),
        exit = if (reduced) fadeOut() else slideOutVertically { it } + fadeOut(),
    ) {
        GlassSurface(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .navigationBarsPadding()
                .padding(8.dp),
            shape = RoundedCornerShape(34.dp),
            strong = true,
            frost = 16.dp,
        ) {
            HistoryContent(enabled, onClose)
        }
    }
}

@Composable
private fun HistoryContent(enabled: Boolean, onClose: () -> Unit) {
    val p = LocalGlass.current
    val items by Stores.history.state.collectAsStateWithLifecycle()
    val dialog = LocalDialog.current
    val toast = LocalToast.current
    val cleared = stringResource(R.string.history_cleared)
    val confirmTitle = stringResource(R.string.clear_history)
    val confirmText = stringResource(R.string.clear_history_confirm)
    val deleteText = stringResource(R.string.delete)
    val cancelText = stringResource(R.string.liquid_cancel)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.history),
                Modifier.weight(1f).padding(start = 8.dp).semantics { heading() },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            if (enabled && items.isNotEmpty()) {
                GlassIconButton(Icons.Rounded.DeleteOutline, confirmTitle, {
                    dialog(
                        DialogSpec(confirmTitle) { dismiss ->
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(confirmText, color = p.contentSecondary, fontSize = 15.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GlassButton(cancelText, dismiss, Modifier.weight(1f))
                                    GlassButton(deleteText, {
                                        History.clear()
                                        dismiss()
                                        toast(cleared)
                                    }, Modifier.weight(1f), danger = true)
                                }
                            }
                        },
                    )
                })
            }
        }
        when {
            !enabled -> EmptyState(stringResource(R.string.history_off), stringResource(R.string.history_off_hint))
            items.isEmpty() -> EmptyState(stringResource(R.string.history_empty), stringResource(R.string.history_empty_hint))
            else -> {
                // En su propia capa: deslizar la lista no obliga a volver a desenfocar el panel.
                LazyColumn(Modifier.graphicsLayer {}, contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)) {
                    items(items, key = { it.time.toString() + it.expression }) { item ->
                        HistoryRow(item) {
                            Session.edit { it.set(item.expression) }
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit) {
    val p = LocalGlass.current
    val separator = Session.editor.separator
    val result = pretty(Format.display(BigDecimal(item.value), separator), LocalGrouping.current)
    val spoken = "${Editor.toEngine(item.expression)} = $result"
    GlassSurface(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                role = Role.Button
            },
        shape = RoundedCornerShape(20.dp),
        frost = 8.dp,
        shadowRadius = 6.dp,
        onClick = onClick,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), horizontalAlignment = Alignment.End) {
            FitMathText(
                item.expression,
                TextStyle(color = p.contentSecondary),
                maxSize = 16.sp,
                minSize = 11.sp,
            )
            Row {
                Spacer(Modifier.width(8.dp))
                Text("= $result", color = p.content, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
