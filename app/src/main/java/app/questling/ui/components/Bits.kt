package app.questling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.questling.data.Friend
import app.questling.data.Game
import app.questling.data.Media
import app.questling.domain.Genres
import app.questling.domain.Kinds
import app.questling.domain.Tier
import app.questling.domain.Trophy
import app.questling.domain.fmt
import app.questling.domain.hash
import app.questling.domain.tr
import app.questling.ui.art.Prop
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlin.math.roundToInt

/** A colour of the data (a genre, a kind, a stat), a touch lighter at night. */
fun tone(c: Long): Color { val col = Color(c); return if (C.dark) lerp(col, Color.White, .18f) else col }
fun kindColor(kind: String) = tone(Kinds.color(kind))
fun genreColor(id: String?) = tone(Genres.of(id)?.color ?: 0xFF8A8A8A)
fun gameColor(g: Game?): Color = if (g == null) C.mute else g.genreList.firstOrNull()?.let { genreColor(it) } ?: kindColor(g.kind)
fun kindIcon(kind: String): Icons = when (kind) { Game.VIDEO -> Icons.PAD; Game.BOARD -> Icons.MEEPLE; Game.RPG -> Icons.DICE; Game.CARD -> Icons.CARDS; Game.MOBILE -> Icons.PHONE; else -> Icons.STAR }

private val FRIEND = listOf(0xFFD0503A, 0xFF4A7AD0, 0xFF3FA36B, 0xFFE0A030, 0xFF8A5AC8, 0xFFD0609A, 0xFF3F9A9A, 0xFFB07A3A)
fun friendColor(f: Friend?): Color = tone(FRIEND[((f?.color ?: 1) - 1).coerceAtLeast(0) % FRIEND.size])

/** Gold as a little pill: the coin and the number. */
@Composable
fun GoldPill(n: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(modifier.clip(RoundedCornerShape(50)).background(C.gold.copy(alpha = .16f)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Prop("coin", 20.dp); Spacer(Modifier.width(4.dp))
        Text(fmt(n), style = T.smallB.copy(color = C.ink))
    }
}

/**
 * A game's cover: its picture, or – without one – a box in the colour of its genre with the kind's glyph
 * and the first letters of the title.
 */
@Composable
fun Cover(g: Game?, modifier: Modifier = Modifier, r: Dp = 10.dp) {
    val ctx = LocalContext.current
    val img = remember(g?.cover) { g?.cover?.let { Media.cover(ctx, it) } }
    Box(modifier.clip(RoundedCornerShape(r)).background(C.card2), contentAlignment = Alignment.Center) {
        if (img != null) Canvas(Modifier.fillMaxSize()) {
            val s = maxOf(size.width / img.width, size.height / img.height)
            val w = img.width * s; val h = img.height * s
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((size.width - w) / 2).roundToInt(), ((size.height - h) / 2).roundToInt()),
                IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Medium)
        } else {
            val col = gameColor(g)
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lerp(col, Color.White, .12f), lerp(col, Color.Black, .28f)))))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(3.dp)) {
                Icon(kindIcon(g?.kind ?: ""), Color.White.copy(alpha = .9f), 20.dp)
                val letters = g?.title.orEmpty().split(' ', '-', ':').filter { it.isNotBlank() }.take(3).joinToString("") { it.first().uppercase() }
                if (letters.isNotEmpty()) Text(letters, style = T.tiny.copy(color = Color.White, fontSize = 10.sp), maxLines = 1)
            }
        }
    }
}

/** A game's row for lists and pickers. */
@Composable
fun GameRow(g: Game, sub: String, modifier: Modifier = Modifier, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(g, Modifier.size(44.dp, 58.dp), 8.dp); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = T.small, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, style = T.numS, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Somebody's round mark: the first letter on their colour. [id] 0 is the player. */
@Composable
fun Avatar(f: Friend?, size: Dp = 36.dp, me: String = "") {
    val col = if (f == null) C.acc else friendColor(f)
    val name = f?.name ?: me.ifBlank { tr("Я", "Me") }
    Box(Modifier.size(size).clip(CircleShape).background(col), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), style = T.bodyB.copy(color = Color.White, fontSize = (size.value * .42f).sp))
    }
}

/** Friends as chips to tick; the last chip adds a new one. */
@Composable
fun FriendPicker(friends: List<Friend>, sel: List<Long>, modifier: Modifier = Modifier, onAdd: (() -> Unit)? = null, onSel: (List<Long>) -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        friends.filter { !it.archived || it.id in sel }.forEach { f ->
            val on = f.id in sel; val col = friendColor(f)
            Row(Modifier.clip(RoundedCornerShape(50)).background(if (on) col else C.card2).clickable { onSel(if (on) sel - f.id else sel + f.id) }.padding(start = 5.dp, end = 13.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(if (on) Color.White.copy(alpha = .25f) else col), contentAlignment = Alignment.Center) {
                    Text(f.name.take(1).uppercase(), style = T.tiny.copy(color = Color.White)) }
                Spacer(Modifier.width(6.dp))
                Text(f.name, style = T.smallB.copy(color = if (on) Color.White else C.ink), maxLines = 1)
            }
        }
        if (onAdd != null) Chip(tr("+ игрок", "+ player"), false, onClick = onAdd)
    }
}

/** Genres as wrapping chips to tick. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenrePicker(kind: String, sel: List<String>, modifier: Modifier = Modifier, max: Int = 3, onSel: (List<String>) -> Unit) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Genres.forKind(kind).forEach { g ->
            val on = g.id in sel
            Chip(g.title, on, color = genreColor(g.id)) { onSel(if (on) sel - g.id else if (sel.size >= max) sel.drop(1) + g.id else sel + g.id) }
        }
    }
}

/** A small label in a colour: a genre, a status, a kind. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = T.tiny.copy(color = color), maxLines = 1, modifier = modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .14f)).padding(horizontal = 8.dp, vertical = 3.dp))
}

fun tierColor(t: Tier) = tone(t.color)

/** A trophy's cup in its metal; grey and faint until earned. */
@Composable
fun Cup(t: Trophy, earned: Boolean, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(if (earned) tierColor(t.tier).copy(alpha = .16f) else C.card2), contentAlignment = Alignment.Center) {
        Prop(t.tier.prop, size * .72f, alpha = if (earned) 1f else .28f)
    }
}

/** Five little hearts for the mood after a session. */
@Composable
fun MoodRow(mood: Int, modifier: Modifier = Modifier, onSet: (Int) -> Unit) {
    val words = listOf(tr("Скучно", "Dull"), tr("Так себе", "Meh"), tr("Нормально", "Fine"), tr("Здорово", "Great"), tr("Восторг", "Thrilled"))
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        words.forEachIndexed { i, w ->
            val on = mood == i + 1; val col = moodColor(i + 1)
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) col else col.copy(alpha = .12f)).clickable { onSet(if (on) 0 else i + 1) }.padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(w, style = T.tiny.copy(color = if (on) Color.White else col, fontSize = 10.sp), maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}
fun moodColor(m: Int) = when (m) { 1 -> C.mute; 2 -> C.gold; 3 -> C.ok; 4 -> C.acc; else -> tone(0xFFD0609A) }

/** A row of choices where one is on, each in its own colour. */
@Composable
fun Choice(items: List<Pair<String, Color>>, sel: Int, modifier: Modifier = Modifier, onSel: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, (l, col) -> Chip(l, i == sel, color = col) { onSel(i) } }
    }
}

/** A tile with a picture, a title and a line about it. */
@Composable
fun StoreTile(prop: String, title: String, line: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.card(18.dp).clickable(onClick = onClick).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Prop(prop, 36.dp); Spacer(Modifier.width(8.dp))
            Text(title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Text(line, style = T.small, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun Dim(on: Boolean, content: @Composable () -> Unit) = Box(Modifier.alpha(if (on) .45f else 1f)) { content() }

/** A stable soft colour out of any text. */
fun colorOf(s: String): Color = tone(FRIEND[(hash(s) and 0x7fffffff) % FRIEND.size])

/** A thin gilded frame for things to be proud of. */
fun Modifier.gilded(r: Dp = 18.dp) = this.border(1.5.dp, C.gold.copy(alpha = .6f), RoundedCornerShape(r))

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))
