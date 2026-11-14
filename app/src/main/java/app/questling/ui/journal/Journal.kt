package app.questling.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.data.Game
import app.questling.data.Play
import app.questling.domain.Kinds
import app.questling.domain.cap
import app.questling.domain.fmtDay
import app.questling.domain.monthName
import app.questling.domain.weekStart
import app.questling.domain.weekday
import java.time.LocalDate
import app.questling.domain.count
import app.questling.domain.dur
import app.questling.domain.fmtDayYear
import app.questling.domain.hm
import app.questling.domain.minOfDay
import app.questling.domain.relDay
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.components.Avatar
import app.questling.ui.components.Chip
import app.questling.ui.components.Cover
import app.questling.ui.components.Empty
import app.questling.ui.components.Field
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Segments
import app.questling.ui.components.Tag
import app.questling.ui.components.card
import app.questling.ui.components.kindColor
import app.questling.ui.play.ShotThumb
import app.questling.ui.theme.C
import app.questling.ui.theme.T

/** The journal: every session by day, the collection of games, and the summary. */
@Composable
fun JournalScreen(d: Data, nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Журнал", "The journal"), style = T.title, modifier = Modifier.weight(1f))
            Round(Icons.USERS) { nav.push(Screen.Friends) }
            Round(Icons.IMAGE) { nav.push(Screen.Album()) }
            Round(Icons.PLUS) { if (nav.journal == 1) nav.push(Screen.GameEdit()) else nav.push(Screen.PlayEdit()) }
        }
        Segments(listOf(tr("Сессии", "Sessions"), tr("Игры", "Games"), tr("Итоги", "Summary")), nav.journal, Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) { nav.journal = it }
        when (nav.journal) { 0 -> Sessions(d, nav); 1 -> Library(d, nav); else -> StatsPart(d, nav) }
    }
}

@Composable
private fun Round(icon: Icons, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, C.ink, 22.dp) }
}

/** A session as one line of a day: the hour, the cover, the game and what there is to say, the length. */
@Composable
fun PlayRow(p: Play, d: Data, nav: Nav, showGame: Boolean = true, showDay: Boolean = false) {
    val g = d.gameById[p.gameId]
    Column(Modifier.fillMaxWidth().clickable { nav.push(Screen.PlayInfo(p.id)) }.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showGame) { Cover(g, Modifier.size(40.dp, 54.dp), 8.dp); Spacer(Modifier.width(12.dp)) }
            else { Box(Modifier.size(4.dp, 38.dp).clip(RoundedCornerShape(50)).background(resultColor(p.result) ?: C.track)); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(if (showGame) g?.title ?: "–" else if (showDay) fmtDayYear(p.day).cap() else hm(minOfDay(p.start)), style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(listOfNotNull(hm(minOfDay(p.start)).takeIf { showGame || showDay }, p.platform.takeIf { it.isNotBlank() }?.let { Kinds.platformTitle(it) }).joinToString(" · "), style = T.small, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (p.moment.isNotBlank()) Tag(Kinds.moment(p.moment), C.gold)
                }
            }
            if (p.friendIds.isNotEmpty()) Row(Modifier.padding(start = 6.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy((-6).dp)) { p.friendIds.take(3).mapNotNull { d.friendById[it] }.forEach { Avatar(it, 22.dp) } }
            // the length, and under it how it ended
            Column(horizontalAlignment = Alignment.End) {
                Text(dur(p.minutes * 60), style = T.numS, maxLines = 1)
                if (p.result != Play.NONE) Text(Kinds.result(p.result).lowercase(), style = T.tiny.copy(color = resultColor(p.result) ?: C.mute), maxLines = 1)
            }
        }
        if (p.note.isNotBlank()) Text(p.note, style = T.small.copy(color = C.ink), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = if (showGame) 52.dp else 16.dp))
        if (p.shotList.isNotEmpty()) Row(Modifier.padding(top = 8.dp, start = if (showGame) 52.dp else 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            p.shotList.take(4).forEach { ShotThumb(it, 54.dp) }
            if (p.shotList.size > 4) Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(C.card2), contentAlignment = Alignment.Center) { Text("+${p.shotList.size - 4}", style = T.smallB) }
        }
    }
}

fun resultColor(r: Int) = when (r) { Play.WIN, Play.COOP_WIN -> C.ok; Play.DRAW -> C.gold; Play.LOSS, Play.COOP_LOSS -> C.bad; else -> null }

/** Sessions of one day (or any list) in one card, with thin lines between them. */
@Composable
fun PlayGroup(plays: List<Play>, d: Data, nav: Nav, showGame: Boolean = true, showDay: Boolean = false) {
    Column(Modifier.fillMaxWidth().card(18.dp)) {
        plays.forEachIndexed { i, p ->
            if (i > 0) Box(Modifier.padding(start = if (showGame) 64.dp else 28.dp).fillMaxWidth().height(1.dp).background(C.line.copy(alpha = .6f)))
            PlayRow(p, d, nav, showGame, showDay)
        }
    }
}

@Composable
private fun Glance(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = T.numS, maxLines = 1); Text(label, style = T.tiny, maxLines = 1) }
}

@Composable
private fun Sessions(d: Data, nav: Nav) {
    var kind by rememberSaveable { mutableStateOf("") }
    var q by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf(false) }
    if (d.plays.isEmpty()) { Empty(tr("Здесь будет каждая ваша сессия.\nНажмите кнопку внизу – и начнём.", "Every session of yours will be here.\nPress the button below to start."), Modifier.padding(30.dp)); return }
    val kinds = remember(d.plays, d.games) { d.plays.mapNotNull { d.gameById[it.gameId]?.kind }.distinct().sortedBy { Game.KINDS.indexOf(it) } }
    val list = remember(d.plays, kind, q) { d.plays.filter { p -> (kind.isEmpty() || d.gameById[p.gameId]?.kind == kind) &&
        (q.isBlank() || d.gameById[p.gameId]?.title?.contains(q.trim(), true) == true || p.note.contains(q.trim(), true)) } }
    val days = remember(list) { list.groupBy { it.day }.entries.sortedByDescending { it.key } }
    val monday = weekStart(d.today); val month = LocalDate.ofEpochDay(d.today).withDayOfMonth(1).toEpochDay()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        // the journal at a glance
        item { Row(Modifier.fillMaxWidth().card(18.dp)) {
            Glance(dur(d.plays.filter { it.day >= monday }.sumOf { it.minutes } * 60), tr("эта неделя", "this week"), Modifier.weight(1f))
            Glance(dur(d.plays.filter { it.day >= month }.sumOf { it.minutes } * 60), tr("этот месяц", "this month"), Modifier.weight(1f))
            Glance("${d.plays.size}", tr("сессий всего", "sessions in all"), Modifier.weight(1f))
        } }
        item { Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(tr("Все", "All"), kind.isEmpty()) { kind = "" }
                if (kinds.size > 1) kinds.forEach { k -> Chip(Kinds.plural(k), kind == k, color = kindColor(k)) { kind = if (kind == k) "" else k } }
            }
            Box(Modifier.padding(start = 6.dp).size(36.dp).clip(CircleShape).background(if (search) C.accSoft else C.card2).clickable { search = !search; if (!search) q = "" }, contentAlignment = Alignment.Center) {
                Icon(if (search) Icons.CLOSE else Icons.SEARCH, if (search) C.acc else C.ink, 17.dp) }
        } }
        if (search) item { Field(q, { q = it }, tr("Игра или слово из заметки", "A game or a word from a note"), Modifier.fillMaxWidth().padding(top = 8.dp)) }
        if (days.isEmpty()) item { Empty(tr("Ничего не нашлось.", "Nothing found.")) }
        var lastMonth = -1
        days.forEach { (day, l) ->
            val ld = LocalDate.ofEpochDay(day); val mk = ld.year * 12 + ld.monthValue
            // a month's own line, once the list leaves the current month
            if (mk != lastMonth) { lastMonth = mk
                if (day < month) { val a = ld.withDayOfMonth(1).toEpochDay(); val z = ld.withDayOfMonth(1).plusMonths(1).toEpochDay() - 1
                    val min = list.filter { it.day in a..z }.sumOf { it.minutes }
                    item(key = "m$mk") { Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${monthName(ld.monthValue).cap()} ${ld.year}", style = T.h2, modifier = Modifier.weight(1f)); Text(dur(min * 60), style = T.smallB.copy(color = C.mute)) } } } }
            item(key = "d$day") { Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (d.today - day <= 1) relDay(day, d.today).cap() else "${weekday(day).cap()}, ${fmtDay(day)}", style = T.smallB, modifier = Modifier.weight(1f))
                Text(dur(l.sumOf { it.minutes } * 60), style = T.small) } }
            item(key = "p$day") { PlayGroup(l, d, nav) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Library(d: Data, nav: Nav) {
    var status by rememberSaveable { mutableStateOf("") }
    var q by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    if (d.games.isEmpty()) { Empty(tr("Коллекция пуста. Добавьте игру плюсом вверху – или просто начните сессию.", "The collection is empty. Add a game with the plus above – or just start a session."), Modifier.padding(30.dp)); return }
    val list = d.games.filter { (status.isEmpty() || it.status == status) && (q.isBlank() || it.title.contains(q.trim(), true)) }.let { l ->
        when (sort) { 0 -> l.sortedByDescending { d.last(it.id)?.start ?: (it.added * 86_400_000L) }; 1 -> l.sortedByDescending { d.minutes(it.id) }; 2 -> l.sortedBy { it.title.lowercase() }; else -> l.sortedByDescending { it.rating } } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        if (d.games.size > 8) Field(q, { q = it }, tr("Найти в коллекции", "Search the collection"), Modifier.fillMaxWidth().padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(tr("Все", "All") + " ${d.games.size}", status.isEmpty()) { status = "" }
            Kinds.STATUSES.forEach { st -> val n = d.games.count { it.status == st }; if (n > 0) Chip("${Kinds.status(st)} $n", status == st, color = statusColor(st)) { status = if (status == st) "" else st } }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(count(list.size, "игра", "игры", "игр", "game", "games"), style = T.small, modifier = Modifier.weight(1f))
            Text(listOf(tr("недавние", "recent"), tr("по часам", "by hours"), tr("по алфавиту", "A–Z"), tr("по оценке", "by rating"))[sort], style = T.smallB.copy(color = C.acc),
                modifier = Modifier.clickable { sort = (sort + 1) % 4 }.padding(4.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val w = (maxWidth - 20.dp) / 3
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                list.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { g ->
                        Column(Modifier.width(w).clip(RoundedCornerShape(12.dp)).clickable { nav.push(Screen.GameInfo(g.id)) }) {
                            Box { Cover(g, Modifier.size(w, w * 1.34f), 12.dp)
                                if (g.status == Game.FINISHED || g.status == Game.HUNDRED) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(22.dp).clip(CircleShape).background(C.gold), contentAlignment = Alignment.Center) {
                                    Icon(if (g.status == Game.HUNDRED) Icons.STAR else Icons.CHECK, androidx.compose.ui.graphics.Color.White, 13.dp) }
                                if (g.rating > 0) Text("★ ${g.rating}", style = T.tiny.copy(color = androidx.compose.ui.graphics.Color.White), modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                                    .clip(RoundedCornerShape(50)).background(androidx.compose.ui.graphics.Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp)) }
                            Text(g.title, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                            Text(d.minutes(g.id).takeIf { it > 0 }?.let { dur(it * 60) } ?: Kinds.status(g.status), style = T.tiny, maxLines = 1)
                        }
                    } } }
            }
        }
        Spacer(Modifier.height(26.dp))
    }
}

fun statusColor(st: String) = when (st) { Game.PLAYING -> C.acc; Game.FINISHED, Game.HUNDRED -> C.gold; Game.DROPPED -> C.mute; Game.WISH -> C.ok; else -> C.mute }
