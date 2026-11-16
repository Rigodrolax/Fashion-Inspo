@file:OptIn(ExperimentalLayoutApi::class)

package com.zeytekx.fashioninspo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeytekx.fashioninspo.R
import com.zeytekx.fashioninspo.data.Store
import com.zeytekx.fashioninspo.data.normTag
import java.text.NumberFormat
import java.util.Locale

/** Palette : fenêtres de menu bleues à bord argenté, nuit profonde et reflets de cristal, or pour l'action. */
object FF {
    val Night = Color(0xFF05070D)
    val Night2 = Color(0xFF0A1322)
    val WinTop = Color(0xFF2B44AE)
    val WinBot = Color(0xFF0B1450)
    val Border = Color(0xFFD9DFEE)
    val Ink = Color(0xFFF3F5FB)
    val Mute = Color(0xFF9DA9CB)
    val Gold = Color(0xFFEBC86B)
    val Crystal = Color(0xFF7FD4FF)
    val Red = Color(0xFFE2606E)
    val Green = Color(0xFF72DB7E)
    val Purple = Color(0xFFB487F0)
    val Yellow = Color(0xFFF2DA5C)
}

val Cinzel = FontFamily(Font(R.font.cinzel, FontWeight.Normal))
val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Normal),
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

private val textShadow = Shadow(Color(0xCC000000), Offset(1.5f, 1.5f), 0f)
val Body = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = FF.Ink, shadow = textShadow)
val TitleStyle = TextStyle(fontFamily = Cinzel, fontSize = 20.sp, letterSpacing = 2.sp, color = FF.Ink, shadow = textShadow)

fun fmt(n: Int): String = NumberFormat.getIntegerInstance(Locale.FRANCE).format(n)

@Composable
fun FFTheme(content: @Composable () -> Unit) {
    val typo = Typography(
        bodyLarge = Body,
        bodyMedium = Body.copy(fontSize = 15.sp),
        bodySmall = Body.copy(fontSize = 13.sp),
        labelLarge = Body.copy(fontWeight = FontWeight.Bold),
        titleLarge = TitleStyle,
    )
    MaterialTheme(
        colorScheme = darkColorScheme(primary = FF.Gold, background = FF.Night, surface = FF.WinBot, onSurface = FF.Ink, onBackground = FF.Ink),
        typography = typo,
    ) {
        CompositionLocalProvider(LocalContentColor provides FF.Ink, LocalTextStyle provides Body, content = content)
    }
}

/** Fond nocturne avec un halo de cristal et de fines lignes diagonales. */
fun Modifier.ffBackground(): Modifier = this.drawBehind {
    drawRect(Brush.verticalGradient(listOf(FF.Night2, FF.Night)))
    val c = Offset(size.width * 0.85f, 0f)
    drawCircle(Brush.radialGradient(listOf(FF.Crystal.copy(alpha = 0.14f), Color.Transparent), center = c, radius = size.width), radius = size.width, center = c)
    val step = 46.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(FF.Crystal.copy(alpha = 0.035f), Offset(x, size.height), Offset(x + size.height, 0f), 1f)
        x += step
    }
}

private val winBrush = Brush.verticalGradient(listOf(FF.WinTop, FF.WinBot))

/** Fenêtre de menu : dégradé bleu, double bord argenté. */
@Composable
fun Win(modifier: Modifier = Modifier, gold: Boolean = false, pad: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .clip(shape)
            .background(winBrush)
            .border(2.dp, if (gold) FF.Gold else FF.Border, shape)
            .padding(2.dp)
            .border(1.dp, Color(0x66000000), RoundedCornerShape(6.dp))
            .padding(pad),
        content = content,
    )
}

/** Curseur triangulaire doré. */
@Composable
fun Cursor(s: Dp = 12.dp, color: Color = FF.Gold) {
    Canvas(Modifier.size(s)) {
        val p = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(p, color)
    }
}

/** Orbe de couleur pour les familles de tags. */
@Composable
fun Orb(color: Color, s: Dp = 14.dp) {
    Canvas(Modifier.size(s)) {
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, color, color.copy(red = color.red * 0.4f, green = color.green * 0.4f, blue = color.blue * 0.4f)),
                center = Offset(size.width * 0.35f, size.height * 0.35f),
                radius = size.width * 0.75f,
            )
        )
    }
}

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, primary: Boolean = false, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(winBrush)
            .border(1.5.dp, if (primary) FF.Gold else if (danger) FF.Red else FF.Border.copy(alpha = 0.8f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (primary) {
            Cursor(9.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (primary) FF.Gold else if (danger) FF.Red else FF.Ink,
        )
    }
}

@Composable
fun FChip(text: String, on: Boolean = false, count: Int? = null, removable: Boolean = false, dot: Color? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (on) FF.Gold.copy(alpha = 0.2f) else Color(0x40000000))
            .border(1.dp, if (on) FF.Gold else FF.Border.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Orb(dot, 10.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (on) FF.Gold else FF.Ink, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(5.dp))
            Text(fmt(count), fontSize = 12.sp, color = FF.Mute)
        }
        if (removable) {
            Spacer(Modifier.width(6.dp))
            Text("×", fontSize = 17.sp, color = FF.Mute)
        }
    }
}

@Composable
fun FLabel(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = FF.Crystal)
}

@Composable
fun FTitle(text: String, modifier: Modifier = Modifier, big: Boolean = false) {
    Text(text.uppercase(), modifier, style = TitleStyle.copy(fontSize = if (big) 24.sp else 19.sp))
}

/** Jauge façon barre d'action. */
@Composable
fun Gauge(progress: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(shape)
            .background(Color(0xFF050A20))
            .border(1.dp, FF.Border.copy(alpha = 0.5f), shape)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Brush.horizontalGradient(listOf(FF.Crystal, FF.Gold)))
        )
    }
}

@Composable
fun FField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    kb: KeyboardType = KeyboardType.Text,
    cap: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x55000000))
            .border(1.dp, FF.Border.copy(alpha = 0.6f), shape)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, color = FF.Mute)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = Body.copy(color = FF.Ink),
            singleLine = singleLine,
            minLines = minLines,
            cursorBrush = SolidColor(FF.Gold),
            keyboardOptions = KeyboardOptions(capitalization = cap, keyboardType = kb),
        )
    }
}

/** Saisie de tag avec suggestions tirées des tags existants. */
@Composable
fun TagInput(
    placeholder: String = "Ajouter un tag…",
    create: Boolean = true,
    exclude: List<String> = emptyList(),
    onPick: (String) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val nq = normTag(q)
    val sugg = remember(nq, Store.tagMap, Store.famOf, exclude) {
        if (nq.isEmpty()) emptyList() else Store.allTags()
            .filter { nq in it && it !in exclude }
            .sortedWith(compareByDescending<String> { it.startsWith(nq) }.thenByDescending { Store.count(it) })
            .take(10)
    }
    val pick: (String) -> Unit = { t -> q = ""; onPick(t) }
    val shape = RoundedCornerShape(6.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0x55000000))
                .border(1.dp, FF.Crystal.copy(alpha = 0.6f), shape)
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            if (q.isEmpty()) Text(placeholder, color = FF.Mute)
            BasicTextField(
                value = q,
                onValueChange = { q = it },
                singleLine = true,
                textStyle = Body.copy(color = FF.Ink),
                cursorBrush = SolidColor(FF.Gold),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (nq.isNotEmpty()) {
                        val m = Store.allTags().firstOrNull { it == nq } ?: (if (create) nq else sugg.firstOrNull())
                        if (m != null && m !in exclude) pick(m)
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (nq.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sugg.forEach { t -> FChip(t, count = Store.count(t)) { pick(t) } }
                if (create && nq !in sugg && nq !in exclude && Store.count(nq) == 0) FChip("+ Nouveau « $nq »", on = true) { pick(nq) }
            }
        }
    }
}

@Composable
fun Seg(options: List<Pair<String, String>>, value: String, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x44000000))
            .border(1.dp, FF.Border.copy(alpha = 0.6f), shape)
    ) {
        options.forEach { (k, l) ->
            val on = k == value
            Row(
                Modifier
                    .weight(1f)
                    .background(if (on) FF.Gold.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onPick(k) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (on) {
                    Cursor(8.dp)
                    Spacer(Modifier.width(5.dp))
                }
                Text(l, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) FF.Gold else FF.Mute, maxLines = 1)
            }
        }
    }
}

/** Feuille qui monte du bas de l'écran, dans une fenêtre de menu. */
@Composable
fun SheetFrame(
    title: String,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Win(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(8.dp)
                .heightIn(max = 680.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            pad = 14.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FTitle(title, Modifier.weight(1f))
                Text("✕", Modifier.clickable(onClick = onClose).padding(8.dp), color = FF.Mute, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                content = actions,
            )
        }
    }
}

@Composable
fun FEmpty(text: String, button: String? = null, onClick: () -> Unit = {}) {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Win(pad = 18.dp) {
            Text(text, color = FF.Ink)
            if (button != null) {
                Spacer(Modifier.height(14.dp))
                Btn(button, primary = true, onClick = onClick)
            }
        }
    }
}

/** Glisser à gauche = suivant (+1), à droite = précédent (-1). */
fun Modifier.swipe(onSwipe: (Int) -> Unit): Modifier = composed {
    val cb by rememberUpdatedState(onSwipe)
    pointerInput(Unit) {
        var acc = 0f
        detectHorizontalDragGestures(
            onDragStart = { acc = 0f },
            onDragEnd = { if (acc < -110f) cb(1) else if (acc > 110f) cb(-1) },
            onHorizontalDrag = { change, d -> acc += d; change.consume() },
        )
    }
}

fun famColor(f: String?): Color {
    if (f == null) return FF.Mute
    val i = Store.fams.indexOf(f)
    if (i < 0) return FF.Mute
    return listOf(FF.Green, FF.Crystal, FF.Red, FF.Yellow, FF.Purple)[i % 5]
}
