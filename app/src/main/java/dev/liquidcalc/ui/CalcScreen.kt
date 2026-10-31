// SPDX-License-Identifier: GPL-3.0-or-later
package dev.liquidcalc.ui

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquidcalc.R
import dev.liquidcalc.core.AngleUnit
import dev.liquidcalc.core.CalcError
import dev.liquidcalc.core.Format
import dev.liquidcalc.data.AppSettings
import dev.liquidcalc.data.Stores
import dev.liquidui.components.DialogHost
import dev.liquidui.components.DialogSpec
import dev.liquidui.components.GlassIconButton
import dev.liquidui.components.GlassSurface
import dev.liquidui.components.LocalDialog
import dev.liquidui.components.LocalToast
import dev.liquidui.components.ToastHost
import dev.liquidui.theme.GlassRoot
import dev.liquidui.theme.LocalGlass
import dev.liquidui.theme.LocalReducedMotion
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.liquidui.icons.styled
import kotlinx.coroutines.delay

/** Raíz de la app: la calculadora, o los ajustes encima; el historial y los avisos flotan. */
@Composable
fun CalcScreen(settings: AppSettings) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var toast by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var dialog by remember { mutableStateOf<DialogSpec?>(null) }
    val reduced = LocalReducedMotion.current

    // Separador decimal del idioma del teléfono (coma en español, punto en inglés).
    val separator = DecimalFormatSymbols.getInstance(LocalConfiguration.current.locales[0]).decimalSeparator
    LaunchedEffect(separator) {
        val e = Session.editor
        if (e.separator != separator) {
            Session.editor = e.copy(expression = e.expression.replace(e.separator, separator), separator = separator)
        }
    }

    BackHandler(enabled = showSettings && dialog == null) { showSettings = false }

    CompositionLocalProvider(
        LocalToast provides { toast = it to System.nanoTime() },
        LocalDialog provides { dialog = it },
    ) {
        GlassRoot(
            animated = settings.animatedBackground,
            floating = {
                HistoryPanel(
                    visible = showHistory && !showSettings,
                    enabled = settings.historyEnabled,
                    onClose = { showHistory = false },
                )
                ToastHost(toast) { toast = null }
                DialogHost(dialog) { dialog = null }
            },
        ) {
            AnimatedContent(
                showSettings,
                transitionSpec = { fadeIn(tween(if (reduced) 0 else 200)) togetherWith fadeOut(tween(if (reduced) 0 else 120)) },
                label = "screen",
            ) { settingsOpen ->
                if (settingsOpen) {
                    SettingsScreen(settings, onBack = { showSettings = false })
                } else {
                    Calculator(
                        settings,
                        separator,
                        onHistory = { showHistory = !showHistory },
                        onSettings = { showSettings = true; showHistory = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun Calculator(settings: AppSettings, separator: Char, onHistory: () -> Unit, onSettings: () -> Unit) {
    val toast = LocalToast.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val angle = settings.angle
    val copiedText = stringResource(R.string.copied)
    val nothingText = stringResource(R.string.nothing_to_save)

    val copy: () -> Unit = {
        Session.currentValue(angle)?.let { v ->
            val text = Format.display(v, separator).replace("-", "−")
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("result", text))) }
            toast(copiedText)
        }
    }
    val paste: () -> Unit = {
        scope.launch {
            val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) Session.edit { it.operand(Editor.normalize(text, separator)) }
        }
    }
    val toggleAngle: () -> Unit = { Stores.settings.update { it.copy(angleUnit = if (it.angleUnit == 1) 0 else 1) } }
    val withValue: ((BigDecimal) -> Unit) -> Unit = { action ->
        val v = Session.currentValue(angle)
        if (v != null) action(v) else toast(nothingText)
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { handleHardwareKey(it, angle, copy, paste, onHistory, toggleAngle) },
    ) {
        val landscape = maxWidth > maxHeight
        val basic = basicKeys(angle, separator)
        val scientific = scientificKeys(angle)
        val memory = memoryKeys(withValue)

        val topBar = @Composable {
            TopBar(settings, onAngle = toggleAngle, onHistory = onHistory, onSettings = onSettings)
        }
        val display = @Composable { m: Modifier -> CalcDisplay(angle, separator, onCopy = copy, modifier = m) }

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.9f).fillMaxHeight()) {
                    topBar()
                    display(Modifier.weight(1f).padding(4.dp))
                    KeyGrid(listOf(memory), Modifier.height(56.dp))
                }
                if (settings.scientific) KeyGrid(scientific, Modifier.weight(1.1f).fillMaxHeight())
                KeyGrid(basic, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                topBar()
                display(Modifier.weight(if (settings.scientific) 0.8f else 1f).padding(4.dp))
                KeyGrid(listOf(memory), Modifier.height(56.dp))
                if (settings.scientific) KeyGrid(scientific, Modifier.weight(1.1f))
                KeyGrid(basic, Modifier.weight(if (settings.scientific) 1.5f else 1.9f))
            }
        }
    }
}

@Composable
private fun TopBar(settings: AppSettings, onAngle: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (settings.scientific) AngleToggle(settings.angle, onAngle)
        Spacer(Modifier.weight(1f))
        GlassIconButton(
            Icons.Rounded.Functions,
            stringResource(R.string.scientific),
            { Stores.settings.update { it.copy(scientific = !it.scientific) } },
            prominent = settings.scientific,
        )
        Spacer(Modifier.width(10.dp))
        GlassIconButton(Icons.Rounded.History, stringResource(R.string.history), onHistory)
        Spacer(Modifier.width(10.dp))
        GlassIconButton(Icons.Rounded.Settings, stringResource(R.string.settings), onSettings)
    }
}

/** DEG / RAD: dice en qué unidad están los ángulos y la cambia al tocarlo. */
@Composable
private fun AngleToggle(angle: AngleUnit, onClick: () -> Unit) {
    val p = LocalGlass.current
    val name = stringResource(if (angle == AngleUnit.DEGREES) R.string.degrees else R.string.radians)
    val description = stringResource(R.string.angle_toggle, name)
    GlassSurface(
        Modifier
            .height(48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            },
        shape = CircleShape,
        strong = true,
        frost = 4.dp,
        shadowRadius = 6.dp,
        onClick = onClick,
    ) {
        Text(
            stringResource(if (angle == AngleUnit.DEGREES) R.string.deg else R.string.rad),
            Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
            color = p.accentFg,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * La pantalla: mientras se escribe, la expresión grande y el resultado parcial abajo; después
 * de "=", la cuenta chica arriba y el resultado grande. Manteniendo apretado el resultado aparece
 * "Copiar".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalcDisplay(angle: AngleUnit, separator: Char, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalGlass.current
    val editor = Session.editor
    val error = Session.error
    val value = Session.currentValue(angle)
    val result = value?.let { pretty(Format.display(it, separator)) }
    val expression = editor.expression.ifEmpty { "0" }
    val resultSpoken = result?.let { stringResource(R.string.result_cd, it) }
    val expressionSpoken = stringResource(R.string.expression_cd, editor.expression)

    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), frost = 14.dp) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (Session.memory.isSet) {
                    val mem = pretty(Format.display(Session.memory.recall(), separator))
                    val spoken = stringResource(R.string.memory_value, mem)
                    Row(Modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) {
                        Text("M", color = p.accentFg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(" $mem", color = p.contentSecondary, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (editor.evaluated) {
                Text(
                    "${editor.expression} =",
                    Modifier.semantics { contentDescription = expressionSpoken },
                    color = p.contentSecondary,
                    fontSize = 20.sp,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                )
                ResultText(result ?: "", resultSpoken, big = true, onCopy)
            } else {
                BasicText(
                    expression,
                    Modifier.semantics { contentDescription = expressionSpoken },
                    style = TextStyle(color = p.content, fontWeight = FontWeight.Light, textAlign = TextAlign.End),
                    maxLines = 3,
                    autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 56.sp),
                )
                when {
                    error != null -> Text(
                        errorText(error),
                        Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        color = p.danger,
                        fontSize = 17.sp,
                        textAlign = TextAlign.End,
                    )
                    result != null -> ResultText(result, resultSpoken, big = false, onCopy)
                    else -> Text(" ", fontSize = 26.sp)
                }
            }
        }
    }
}

/**
 * El resultado. Mantenerlo apretado muestra la burbuja "Copiar" encima; se va sola a los pocos
 * segundos o cuando cambia el número.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultText(text: String, spoken: String?, big: Boolean, onCopy: () -> Unit) {
    val p = LocalGlass.current
    val haptics = LocalHapticFeedback.current
    var menu by remember(text) { mutableStateOf(false) }
    LaunchedEffect(menu) {
        if (menu) {
            delay(4000)
            menu = false
        }
    }
    val copyLabel = stringResource(R.string.copy)
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(menu, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            GlassSurface(
                Modifier.padding(bottom = 6.dp).height(44.dp),
                shape = CircleShape,
                strong = true,
                frost = 4.dp,
                shadowRadius = 8.dp,
                onClick = {
                    onCopy()
                    menu = false
                },
            ) {
                Row(Modifier.align(Alignment.Center).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(styled(Icons.Rounded.ContentCopy), null, Modifier.size(18.dp), tint = p.accentFg)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_short), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        BasicText(
            text,
            Modifier
                .combinedClickable(
                    remember { MutableInteractionSource() },
                    null,
                    onLongClickLabel = copyLabel,
                    onClick = { menu = false },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    },
                )
                .semantics {
                    if (spoken != null) contentDescription = spoken
                    liveRegion = LiveRegionMode.Polite
                },
            style = TextStyle(
                color = if (big) p.content else p.contentSecondary,
                fontWeight = if (big) FontWeight.Light else FontWeight.Normal,
                textAlign = TextAlign.End,
            ),
            maxLines = 1,
            autoSize = if (big) {
                TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 64.sp)
            } else {
                TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 26.sp)
            },
        )
    }
}

@Composable
private fun errorText(error: CalcError): String = stringResource(
    when (error) {
        is CalcError.Syntax -> R.string.error_syntax
        CalcError.DivisionByZero -> R.string.error_division
        CalcError.Domain -> R.string.error_domain
        CalcError.Overflow -> R.string.error_overflow
    },
)

private val SUPERSCRIPT = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '-' to '⁻',
)

/** "1,5×10^-7" → "1,5×10⁻⁷" y el menos de verdad (−) en vez del guion. */
fun pretty(text: String): String {
    val i = text.indexOf("×10^")
    val base = if (i < 0) text else text.substring(0, i) + "×10" + text.substring(i + 4).map { SUPERSCRIPT[it] ?: it }.joinToString("")
    return if (base.startsWith("-")) "−" + base.substring(1) else base
}

// --- Teclas ---------------------------------------------------------------------------------

private fun basicKeys(angle: AngleUnit, separator: Char): List<List<CalcKey>> {
    fun digit(d: Char) = CalcKey(d.toString(), null, KeyKind.DIGIT) { Session.edit { it.digit(d) } }
    fun op(label: Char, spoken: Int) = CalcKey(label.toString(), spoken, KeyKind.OPERATOR) { Session.edit { it.operator(label) } }
    return listOf(
        listOf(
            CalcKey("AC", R.string.key_clear, KeyKind.CLEAR) { Session.edit { it.clear() } },
            CalcKey("( )", R.string.key_parens, KeyKind.OPERATOR) { Session.edit { it.parenthesis() } },
            CalcKey("%", R.string.key_percent, KeyKind.OPERATOR) { Session.edit { it.postfix('%') } },
            op('÷', R.string.key_divide),
        ),
        listOf(digit('7'), digit('8'), digit('9'), op('×', R.string.key_times)),
        listOf(digit('4'), digit('5'), digit('6'), op('−', R.string.key_minus)),
        listOf(digit('1'), digit('2'), digit('3'), op('+', R.string.key_plus)),
        listOf(
            digit('0'),
            CalcKey(separator.toString(), R.string.key_decimal, KeyKind.DIGIT) { Session.edit { it.decimal() } },
            CalcKey("", R.string.key_backspace, KeyKind.CLEAR, Icons.AutoMirrored.Rounded.Backspace) { Session.edit { it.backspace() } },
            CalcKey("=", R.string.key_equals, KeyKind.EQUALS) { Session.equals(angle) },
        ),
    )
}

private fun scientificKeys(angle: AngleUnit): List<List<CalcKey>> {
    fun fn(label: String, text: String, spoken: Int) = CalcKey(label, spoken, KeyKind.FUNCTION) { Session.edit { it.operand(text) } }
    fun pow(label: String, suffix: String, spoken: Int) = CalcKey(label, spoken, KeyKind.FUNCTION) { Session.edit { it.power(suffix) } }
    return listOf(
        listOf(fn("sin", "sin(", R.string.key_sin), fn("cos", "cos(", R.string.key_cos), fn("tan", "tan(", R.string.key_tan), fn("π", "π", R.string.key_pi), fn("e", "e", R.string.key_e)),
        listOf(
            fn("sin⁻¹", "asin(", R.string.key_asin), fn("cos⁻¹", "acos(", R.string.key_acos), fn("tan⁻¹", "atan(", R.string.key_atan),
            CalcKey("(", R.string.key_open, KeyKind.FUNCTION) { Session.edit { it.open() } },
            CalcKey(")", R.string.key_close, KeyKind.FUNCTION) { Session.edit { it.close() } },
        ),
        listOf(
            fn("ln", "ln(", R.string.key_ln), fn("log", "log(", R.string.key_log), fn("√", "√", R.string.key_sqrt), fn("∛", "∛", R.string.key_cbrt),
            CalcKey("xʸ", R.string.key_power, KeyKind.FUNCTION) { Session.edit { it.operator('^') } },
        ),
        listOf(
            pow("x²", "^2", R.string.key_square),
            CalcKey("x!", R.string.key_factorial, KeyKind.FUNCTION) { Session.edit { it.postfix('!') } },
            fn("eˣ", "exp(", R.string.key_exp), fn("|x|", "abs(", R.string.key_abs), pow("1/x", "^(−1)", R.string.key_inverse),
        ),
    )
}

private fun memoryKeys(withValue: ((BigDecimal) -> Unit) -> Unit): List<CalcKey> = listOf(
    CalcKey("MC", R.string.key_mc, KeyKind.FUNCTION) { Session.memoryClear() },
    CalcKey("MR", R.string.key_mr, KeyKind.FUNCTION) { Session.memoryRecall() },
    CalcKey("M+", R.string.key_m_plus, KeyKind.FUNCTION) { withValue { Session.memoryPlus(it) } },
    CalcKey("M−", R.string.key_m_minus, KeyKind.FUNCTION) { withValue { Session.memoryMinus(it) } },
    CalcKey("ANS", R.string.key_ans, KeyKind.FUNCTION) { Session.edit { it.operand(Editor.ANS) } },
)
