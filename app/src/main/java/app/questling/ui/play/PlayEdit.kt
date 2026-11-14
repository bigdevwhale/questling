package app.questling.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.data.Friend
import app.questling.data.Media
import app.questling.data.Play
import app.questling.domain.Kinds
import app.questling.domain.cap
import app.questling.domain.dur
import app.questling.domain.fmtDayYear
import app.questling.domain.hm
import app.questling.domain.minOfDay
import app.questling.domain.tr
import app.questling.domain.weekday
import app.questling.domain.zone
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.components.Avatar
import app.questling.ui.components.Btn
import app.questling.ui.components.Chip
import app.questling.ui.components.ChipRow
import app.questling.ui.components.Cover
import app.questling.ui.components.DayPicker
import app.questling.ui.components.Field
import app.questling.ui.components.FriendPicker
import app.questling.ui.components.GameRow
import app.questling.ui.components.Ghost
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Page
import app.questling.ui.components.Popup
import app.questling.ui.components.Sheet
import app.questling.ui.components.Tag
import app.questling.ui.components.TimeChip
import app.questling.ui.components.card
import app.questling.ui.components.friendColor
import app.questling.ui.components.moodColor
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A session written by hand, or an old one being edited. */
@Composable
fun PlayEditPage(scr: Screen.PlayEdit, d: Data, nav: Nav) {
    val ctx = LocalContext.current; val c = ctx.container
    val scope = rememberCoroutineScope()
    val old = remember { d.plays.firstOrNull { it.id == scr.id } }
    var gameId by remember { mutableLongStateOf(old?.gameId ?: scr.gameId) }
    var day by remember { mutableLongStateOf(old?.day ?: d.today) }
    var startMin by remember { mutableIntStateOf(old?.let { minOfDay(it.start) } ?: (minOfDay(System.currentTimeMillis()) - 60).coerceAtLeast(0)) }
    var minutes by remember { mutableStateOf((old?.minutes ?: 60).toString()) }
    var platform by remember { mutableStateOf(old?.platform ?: "") }
    var friends by remember { mutableStateOf(old?.friendIds ?: emptyList()) }
    val dr = remember { PlayDraft(old, d.seatsByPlay[scr.id].orEmpty()) }
    var pick by remember { mutableStateOf(false) }
    var newFriend by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val g = d.gameById[gameId]
    val plats = remember(gameId) { platformsFor(g, d) }
    LaunchedEffect(gameId) { if (platform.isBlank() || platform !in plats) platform = plats.first() }

    Page(if (old == null) tr("Записать сессию", "Write a session down") else tr("Сессия", "Session"), { nav.pop() },
        action = { if (old != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable { del = true }, contentAlignment = Alignment.Center) { Icon(Icons.TRASH, C.bad, 20.dp) } }) {
        Row(Modifier.fillMaxWidth().card(18.dp).clickable { pick = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(g, Modifier.size(44.dp, 58.dp), 8.dp); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g?.title ?: tr("Выберите игру", "Pick a game"), style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(g?.let { Kinds.title(it.kind) } ?: tr("Во что играли?", "What did you play?"), style = T.small)
            }
            Icon(Icons.NEXT, C.mute, 20.dp)
        }
        Label(tr("Когда", "When"))
        DayPicker(day, d.today) { day = it }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Начало", "Start"), style = T.body); TimeChip(startMin) { startMin = it }
        }
        Label(tr("Сколько играли, минут", "How long, minutes"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Field(minutes, { v -> minutes = v.filter { it.isDigit() }.take(4) }, "60", Modifier.width(84.dp), number = true, center = true)
            listOf(15, 30, 60, 90, 120, 180).forEach { m -> Chip(if (m < 60) "$m" else dur(m * 60), minutes == m.toString()) { minutes = m.toString() } }
        }
        if (plats.size > 1) { Label(tr("На чём", "On what")); ChipRow(plats.map { Kinds.platformTitle(it) }, plats.indexOf(platform)) { platform = plats[it] } }
        Label(tr("С кем", "With whom"))
        FriendPicker(d.friends, friends, onAdd = { newFriend = true }) { friends = it }
        PlayFields(dr, g, friends, d)
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 20.dp), enabled = g != null && (minutes.toIntOrNull() ?: 0) > 0 && !saving) {
            saving = true
            scope.launch {
                val start = LocalDate.ofEpochDay(day).atStartOfDay(zone).toInstant().toEpochMilli() + startMin * 60_000L
                val shots = dr.allShots(ctx)
                val seats = dr.seats(friends)
                val p = (old ?: Play(gameId = gameId, day = day, start = start, minutes = 1)).copy(gameId = gameId, day = day, start = start, minutes = minutes.toInt(), platform = platform,
                    result = dr.result, score = dr.myScore, friends = friends.joinToString(","), mood = dr.mood, note = dr.note.trim(), shots = shots.joinToString("\n"), moment = dr.moment,
                    place = seats.firstOrNull { it.friendId == 0L }?.place ?: 0)
                if (old == null) { val id = c.addPlay(p, seats); nav.replace(Screen.PlayInfo(id)) }
                else { c.updatePlay(p, seats); Media.delete(ctx, old.shotList - shots.toSet()); nav.pop() }
            }
        }
    }
    if (pick) Sheet({ pick = false }) {
        var q by remember { mutableStateOf("") }
        Text(tr("Во что играли?", "What did you play?"), style = T.h2)
        Field(q, { q = it }, tr("Найти игру", "Find a game"), Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp))
        d.recent.filter { q.isBlank() || it.title.contains(q.trim(), true) }.take(12).forEach { x -> GameRow(x, Kinds.title(x.kind)) { gameId = x.id; pick = false } }
        Ghost(tr("Добавить новую игру", "Add a new game"), Modifier.fillMaxWidth().padding(top = 8.dp)) { pick = false; nav.push(Screen.GameEdit(title = q.trim())) }
    }
    if (newFriend) NewFriend({ newFriend = false }) { friends = friends + it }
    if (del && old != null) Popup({ del = false }) {
        Text(tr("Удалить сессию?", "Delete the session?"), style = T.h2)
        Text(tr("Вместе с ней удалятся её снимки.", "Its pictures go with it."), style = T.body.copy(color = C.mute), modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Оставить", "Keep"), Modifier.weight(1f)) { del = false }
            Btn(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { c.write { deletePlay(old.id) }; Media.delete(ctx, old.shotList)
                del = false; nav.stack.removeAll { it == scr || it == Screen.PlayInfo(old.id) } }
        }
    }
}

/** A session as it was: the game, the time, how it ended, who played, the note and the pictures. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayPage(id: Long, d: Data, nav: Nav) {
    val p = d.plays.firstOrNull { it.id == id }
    if (p == null) { if (d.plays.isNotEmpty()) LaunchedEffect(Unit) { nav.stack.remove(Screen.PlayInfo(id)) }; return }
    val g = d.gameById[p.gameId]
    val seats = d.seatsByPlay[p.id].orEmpty()
    Page(tr("Сессия", "Session"), { nav.pop() }, action = {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.SESSION, p.id) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 20.dp) }
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.push(Screen.PlayEdit(p.id)) }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 20.dp) }
    }) {
        Row(Modifier.fillMaxWidth().card(22.dp).clickable(enabled = g != null) { g?.let { nav.push(Screen.GameInfo(it.id)) } }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(g, Modifier.size(72.dp, 96.dp), 12.dp); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(g?.title ?: "–", style = T.h2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${weekday(p.day).cap()}, ${fmtDayYear(p.day)} · ${hm(minOfDay(p.start))}", style = T.small)
                Text(dur(p.minutes * 60), style = T.num, modifier = Modifier.padding(top = 4.dp))
            }
        }
        FlowRow(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (p.platform.isNotBlank()) Tag(Kinds.platformTitle(p.platform), C.mute)
            if (p.result != Play.NONE) Tag(Kinds.result(p.result), when (p.result) { Play.WIN, Play.COOP_WIN -> C.ok; Play.DRAW -> C.gold; else -> C.bad })
            if (p.moment.isNotBlank()) Tag(Kinds.moment(p.moment), C.gold)
            if (p.mood > 0) Tag(listOf(tr("Скучно", "Dull"), tr("Так себе", "Meh"), tr("Нормально", "Fine"), tr("Здорово", "Great"), tr("Восторг", "Thrilled"))[p.mood - 1], moodColor(p.mood))
            if (p.breaks > 0) Tag(tr("перерывов: ${p.breaks}", "breaks: ${p.breaks}"), C.ok)
            if (p.auto) Tag(tr("найдена в телефоне", "found on the phone"), C.mute)
            if (p.hasScore && seats.isEmpty()) Tag(tr("очки: ${p.score}", "score: ${p.score}"), C.acc)
        }
        if (p.friendIds.isNotEmpty()) {
            Label(tr("Кто играл", "Who played"))
            Column(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
                val rows: List<Friend?> = listOf<Friend?>(null) + p.friendIds.mapNotNull { d.friendById[it] }
                rows.sortedBy { f -> seats.firstOrNull { it.friendId == (f?.id ?: 0L) }?.place?.takeIf { it > 0 } ?: 99 }.forEach { f ->
                    val st = seats.firstOrNull { it.friendId == (f?.id ?: 0L) }
                    Row(Modifier.fillMaxWidth().clickable(enabled = f != null) { f?.let { nav.push(Screen.FriendInfo(it.id)) } }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(f, 32.dp, d.s.player); Spacer(Modifier.width(10.dp))
                        Text(f?.name ?: d.s.player.ifBlank { tr("Я", "Me") }, style = T.body, modifier = Modifier.weight(1f), maxLines = 1)
                        if (st?.won == true && p.result != Play.COOP_WIN) { Icon(Icons.CROWN, C.gold, 18.dp); Spacer(Modifier.width(8.dp)) }
                        if (st != null && st.score != Int.MIN_VALUE) Text("${st.score}", style = T.numS)
                    }
                }
            }
        }
        if (p.note.isNotBlank()) { Label(tr("Заметка", "Note")); Text(p.note, style = T.body, modifier = Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) }
        if (p.shotList.isNotEmpty()) {
            Label(tr("Снимки", "Pictures"))
            ShotGrid(p.shotList) { nav.push(Screen.Viewer(p.shotList, it)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/** Pictures in three columns. */
@Composable
fun ShotGrid(shots: List<String>, onOpen: (Int) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = (maxWidth - 12.dp) / 3
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shots.withIndex().chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (i, n) -> ShotThumb(n, cell, Modifier.clickable { onOpen(i) }) } } }
        }
    }
}
