package app.questling.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.domain.Kinds
import app.questling.domain.Span
import app.questling.domain.SpanKind
import app.questling.domain.Spans
import app.questling.domain.Stat
import app.questling.domain.Stats
import app.questling.domain.dayShort
import app.questling.domain.dur
import app.questling.domain.fmt1
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.components.Bars
import app.questling.ui.components.ChipRow
import app.questling.ui.components.Cover
import app.questling.ui.components.Empty
import app.questling.ui.components.Heat
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Meter
import app.questling.ui.components.Split
import app.questling.ui.components.TileGrid
import app.questling.ui.components.card
import app.questling.ui.components.gameColor
import app.questling.ui.components.genreColor
import app.questling.ui.components.kindColor
import app.questling.ui.components.colorOf
import app.questling.ui.components.tone
import app.questling.ui.play.Label
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The summary: a span at the top, then the hours, the calendar, the games, the genres and the habits. */
@Composable
fun StatsPart(d: Data, nav: Nav) {
    var kind by rememberSaveable { mutableIntStateOf(1) }
    var back by rememberSaveable { mutableIntStateOf(0) }
    val sk = SpanKind.entries[kind]
    val span = remember(sk, back, d.first, d.today) { Spans.of(sk, back, d.first, d.today) }
    val st = remember(d.plays, span, d.games) { Stats(d.plays, span, d.gameById, d.today) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        ChipRow(listOf(tr("Неделя", "Week"), tr("Месяц", "Month"), tr("Сезон", "Season"), tr("Год", "Year"), tr("Всё время", "All time")), kind) { kind = it; back = 0 }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sk != SpanKind.ALL) Arrow(Icons.BACK, span.from > d.first) { back++ }
            Text(span.label, style = T.h3, modifier = Modifier.weight(1f), textAlign = if (sk == SpanKind.ALL) TextAlign.Start else TextAlign.Center)
            if (sk != SpanKind.ALL) Arrow(Icons.NEXT, back > 0) { back-- }
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { nav.share = ShareReq(when (sk) { SpanKind.WEEK -> CardKind.WEEK; SpanKind.YEAR, SpanKind.ALL -> CardKind.YEAR; else -> CardKind.MONTH }) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 20.dp) }
        }
        if (st.count == 0) { Empty(tr("За это время сессий нет.", "No sessions in this span.")); Spacer(Modifier.height(20.dp))
            YearDoor(d, nav); return@Column }

        val delta = if (st.minutesBefore > 0 && sk != SpanKind.ALL) (st.minutes - st.minutesBefore).let { (if (it >= 0) "+" else "−") + dur(kotlin.math.abs(it) * 60) + tr(" к прошлому", " vs before") } else null
        TileGrid(listOf(
            Triple(dur(st.minutes * 60), tr("наиграно", "played"), delta), Triple("${st.count}", tr("сессий", "sessions"), null),
            Triple("${st.days}", tr("игровых дней", "play days"), null), Triple(dur(st.avg * 60), tr("средняя сессия", "average session"), null),
            Triple("${st.gamesPlayed}", tr("разных игр", "different games"), if (st.fresh > 0) tr("новых: ${st.fresh}", "new: ${st.fresh}") else null),
            Triple("${st.finished}", tr("пройдено", "finished"), null)),
            icons = listOf(Icons.CLOCK to C.acc, Icons.PAD to C.acc, Icons.CAL to C.ok, Icons.TIMER to C.ok, Icons.STAR to C.gold, Icons.FLAG to C.gold))

        Label(tr("Часы", "Hours"))
        Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { Bars(st.series(), fmtv = { fmt1(it) }) }

        if (span.days > 31) { Label(tr("Календарь", "Calendar"))
            val from = if (sk == SpanKind.ALL) maxOf(span.from, d.today - 364) else span.from
            Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { Heat(st.byDay(), from, minOf(span.to, d.today)) } }

        Label(tr("Игры", "Games"))
        Column(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
            st.byGame().take(8).forEach { s ->
                Row(Modifier.fillMaxWidth().clickable(enabled = s.key != null) { s.key?.let { nav.push(Screen.GameInfo(it.id)) } }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(s.key, Modifier.size(34.dp, 44.dp), 6.dp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.key?.title ?: "–", style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Meter(s.share / (st.byGame().firstOrNull()?.share ?: 1f).coerceAtLeast(.001f), Modifier.padding(top = 4.dp), gameColor(s.key), 6.dp)
                    }
                    Text(dur(s.minutes * 60), style = T.numS, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }

        val genres = st.byGenre()
        if (genres.isNotEmpty()) { Label(tr("Жанры", "Genres"))
            Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { Shares(genres.take(8).map { Triple(it.key.title, genreColor(it.key.id), it.minutes) }) } }

        Label(tr("Профиль игрока", "The player's shape"))
        Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Radar(st.byStat())
            Text(tr("Шесть характеристик питомцев – это и ваш портрет: чем больше играете в жанр, тем дальше его луч.", "The creatures' six stats are your portrait too: the more you play a genre, the longer its ray."), style = T.tiny, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }

        val kinds = st.byKind(); val plats = st.byPlatform()
        if (kinds.size > 1) { Label(tr("Во что", "What")); Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { Shares(kinds.map { Triple(Kinds.plural(it.key), kindColor(it.key), it.minutes) }) } }
        if (plats.size > 1) { Label(tr("На чём", "On what")); Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { Shares(plats.map { Triple(Kinds.platformTitle(it.key), colorOf(it.key), it.minutes) }) } }

        if (st.wins + st.losses > 0) { Label(tr("Победы и поражения", "Wins and losses"))
            Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { Split(st.wins.toDouble(), st.losses.toDouble(), tr("Победы", "Wins") + " ${st.wins} ·", "· ${st.losses} " + tr("поражения", "losses"), C.ok, C.bad)
                if (st.coop > 0) Text(tr("И ещё ${st.coop} побед вместе.", "And ${st.coop} more wins together."), style = T.small, modifier = Modifier.padding(top = 8.dp)) } }

        Label(tr("По дням недели", "By weekday"))
        Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { val wd = st.byWeekday(); Bars(wd.mapIndexed { i, v -> dayShort(i) to v / 60.0 }, hi = wd.indexOf(wd.max()), h = 90.dp, color = C.ok) }
        Label(tr("По времени суток", "By time of day"))
        Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { val hr = st.byHour(); Bars(hr.mapIndexed { i, v -> (if (i % 6 == 0) "$i" else "") to v / 60.0 }, hi = hr.indexOf(hr.max()), h = 80.dp, color = C.gold) }

        Label(tr("Ещё", "More"))
        TileGrid(listOfNotNull(
            st.longest?.let { Triple(dur(it.minutes * 60), tr("самая долгая сессия", "longest session"), null as String?) },
            Triple("${st.social}", tr("сессий с друзьями", "sessions with friends"), null),
            Triple("${st.shots}", tr("снимков", "pictures"), null),
            if (st.mood > 0) Triple(fmt1(st.mood) + " / 5", tr("настроение", "mood"), null) else null))
        Spacer(Modifier.height(16.dp))
        YearDoor(d, nav)
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun YearDoor(d: Data, nav: Nav) {
    val y = LocalDate.ofEpochDay(d.today).year
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.goldSoft).clickable { nav.push(Screen.Wrapped(y)) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.SCROLL, C.gold, 26.dp); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(tr("Год в играх: $y", "The year in games: $y"), style = T.bodyB); Text(tr("Главные игры, жанры и рекорды года", "The year's games, genres and records"), style = T.small.copy(color = C.ink)) }
        Icon(Icons.NEXT, C.gold, 20.dp)
    }
}

@Composable
private fun Arrow(icon: Icons, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(enabled = on, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, if (on) C.ink else C.line, 20.dp) }
}

/** Rows of name, a bar in the thing's colour and its hours. */
@Composable
fun Shares(items: List<Triple<String, Color, Int>>) {
    val max = (items.maxOfOrNull { it.third } ?: 1).coerceAtLeast(1)
    items.forEach { (name, col, min) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = T.small.copy(color = C.ink), modifier = Modifier.width(112.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Meter(min.toFloat() / max, Modifier.weight(1f), col, 8.dp)
            Text(dur(min * 60), style = T.tiny.copy(color = C.ink), modifier = Modifier.width(74.dp).padding(start = 8.dp), textAlign = TextAlign.End, maxLines = 1)
        }
    }
}

/** A six-rayed star of the stats. */
@Composable
fun Radar(values: List<Pair<Stat, Int>>, modifier: Modifier = Modifier) {
    val max = (values.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val line = C.line; val acc = C.acc
    Box(modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val cx = size.width / 2; val cy = size.height / 2; val r = size.width / 2
            fun pt(i: Int, k: Float) = Offset(cx + r * k * cos(-PI / 2 + i * PI / 3).toFloat(), cy + r * k * sin(-PI / 2 + i * PI / 3).toFloat())
            for (ring in 1..3) drawPath(Path().apply { for (i in 0..5) { val p = pt(i, ring / 3f); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }, line, style = Stroke(1.dp.toPx()))
            for (i in 0..5) drawLine(line, Offset(cx, cy), pt(i, 1f), 1.dp.toPx())
            val shape = Path().apply { values.forEachIndexed { i, (_, v) -> val p = pt(i, (.12f + .88f * v / max)); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
            drawPath(shape, acc.copy(alpha = .28f)); drawPath(shape, acc, style = Stroke(2.dp.toPx()))
            values.forEachIndexed { i, (s, v) -> drawCircle(tone(s.color), 4.dp.toPx(), pt(i, (.12f + .88f * v / max))) }
        }
        values.forEachIndexed { i, (s, _) ->
            val a = -PI / 2 + i * PI / 3
            Text(s.title, style = T.tiny.copy(color = tone(s.color)), modifier = Modifier.align(Alignment.Center).padding(
                start = (if (cos(a) > .1) 196 * cos(a) else 0.0).dp, end = (if (cos(a) < -.1) -196 * cos(a) else 0.0).dp,
                top = (if (sin(a) > .1) 196 * sin(a) else 0.0).dp, bottom = (if (sin(a) < -.1) -196 * sin(a) else 0.0).dp))
        }
    }
}
