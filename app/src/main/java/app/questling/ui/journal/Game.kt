package app.questling.ui.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import app.questling.ui.components.Avatar
import app.questling.ui.components.tap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.data.Catalog
import app.questling.data.Draft
import app.questling.data.Game
import app.questling.data.Media
import app.questling.data.Play
import app.questling.domain.Genres
import app.questling.domain.Kinds
import app.questling.domain.Lang
import app.questling.domain.count
import app.questling.domain.dur
import app.questling.domain.fmtDayYear
import app.questling.domain.monthShort
import app.questling.domain.relDay
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.components.Bars
import app.questling.ui.components.Btn
import app.questling.ui.components.Chip
import app.questling.ui.components.ChipRow
import app.questling.ui.components.Cover
import app.questling.ui.components.Field
import app.questling.ui.components.GenrePicker
import app.questling.ui.components.Ghost
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Page
import app.questling.ui.components.Popup
import app.questling.ui.components.Tag
import app.questling.ui.components.card
import app.questling.ui.components.gameColor
import app.questling.ui.components.genreColor
import app.questling.ui.components.kindColor
import app.questling.ui.play.Label
import app.questling.ui.play.ShotThumb
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A game's own page: a stage in the game's colour with the cover and the hours, the two buttons, where it stands
 * and the rating, where the player stopped, the milestones, the company, the months in bars, the album and every session.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamePage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val g = d.gameById[id]
    if (g == null) { if (d.games.isNotEmpty()) LaunchedEffect(Unit) { nav.stack.remove(Screen.GameInfo(id)) }; return }
    BackHandler { nav.pop() }
    val plays = d.byGame[id].orEmpty()
    val min = plays.sumOf { it.minutes }
    val col = gameColor(g)
    var note by remember(g.id) { mutableStateOf(g.note) }
    var editNote by remember { mutableStateOf(false) }
    var all by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(C.bg).tap {}) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // the stage
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(col.copy(alpha = .42f), col.copy(alpha = .12f), C.bg))).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Round(Icons.BACK) { nav.pop() }
                    Spacer(Modifier.weight(1f))
                    Round(Icons.SHARE) { nav.share = ShareReq(CardKind.GAME, g.id) }
                    Spacer(Modifier.width(6.dp))
                    Round(Icons.EDIT) { nav.push(Screen.GameEdit(g.id)) }
                }
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 60.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Cover(g, Modifier.shadow(10.dp, RoundedCornerShape(16.dp)).size(126.dp, 168.dp), 16.dp); Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.title, style = T.h2, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        listOfNotNull(g.maker.takeIf { it.isNotBlank() }, g.year.takeIf { it > 0 }?.toString()).joinToString(" · ").takeIf { it.isNotBlank() }?.let { Text(it, style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Tag(Kinds.title(g.kind), kindColor(g.kind))
                            g.genreList.mapNotNull { Genres.of(it) }.forEach { Tag(it.title, genreColor(it.id)) }
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 18.dp)) {
                // the hours, and the two things to do
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(if (min > 0) dur(min * 60) else tr("Ещё не играли", "Not played yet"), style = if (min > 0) T.big else T.h2)
                        if (plays.isNotEmpty()) Text("${count(plays.size, "сессия", "сессии", "сессий", "session", "sessions")} · ${tr("последняя", "the last one")} ${relDay(plays.first().day, d.today)}", style = T.small)
                    }
                    if (g.finished > 0) Column(horizontalAlignment = Alignment.End) {
                        Icon(if (g.status == Game.HUNDRED) Icons.STAR else Icons.FLAG, C.gold, 22.dp)
                        Text(if (g.status == Game.HUNDRED) "100%" else tr("пройдена", "finished"), style = T.tiny.copy(color = C.gold)); Text(fmtDayYear(g.finished), style = T.tiny)
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(if (d.s.live.on && d.s.live.gameId == g.id) tr("Идёт сессия", "Session running") else tr("Играть", "Play"), Modifier.weight(1.4f), color = col, ink = Color.White, enabled = !d.s.live.on || d.s.live.gameId == g.id) {
                        if (d.s.live.on) nav.push(Screen.Live) else { nav.startGame = g.id; nav.start = true } }
                    Ghost(tr("Уже сыграли", "Already played"), Modifier.weight(1f)) { nav.push(Screen.PlayEdit(gameId = g.id)) }
                }

                // where it stands and what it is worth, in one card
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().card(18.dp).padding(14.dp)) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Kinds.STATUSES.forEach { st -> Chip(Kinds.status(st), g.status == st, color = statusColor(st)) { c.write { setStatus(g, st) } } }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (g.rating > 0) tr("Моя оценка", "My rating") else tr("Оценить", "Rate it"), style = T.small, modifier = Modifier.weight(1f))
                        if (g.rating > 0) Text("${g.rating} / 10", style = T.numS.copy(color = C.gold))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        (1..10).forEach { r -> Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(8.dp)).clickable { c.write { saveGame(g.copy(rating = if (g.rating == r) 0 else r)) } }, contentAlignment = Alignment.Center) {
                            Icon(Icons.STAR, if (r <= g.rating) C.gold else C.track, 22.dp) } }
                    }
                }

                // where the player stopped: the game's note, and the last thing written after a session
                val last = plays.firstOrNull { it.note.isNotBlank() }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("На чём остановились", "Where you stopped").uppercase(), style = T.sec, modifier = Modifier.weight(1f))
                    Text(if (editNote) tr("Готово", "Done") else if (g.note.isBlank()) tr("Записать", "Write") else tr("Изменить", "Edit"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable {
                        if (editNote && note.trim() != g.note) c.write { saveGame(g.copy(note = note.trim())) }; editNote = !editNote }.padding(4.dp))
                }
                if (editNote) Field(note, { note = it }, tr("Билд, глава, правила, которые вечно забываются", "The build, the chapter, the rules that always slip the mind"), Modifier.fillMaxWidth(), single = false)
                else if (g.note.isNotBlank()) Text(g.note, style = T.body, modifier = Modifier.fillMaxWidth().card(18.dp).padding(14.dp))
                if (last != null && !editNote) Column(Modifier.padding(top = if (g.note.isNotBlank()) 8.dp else 0.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.PlayInfo(last.id)) }.padding(14.dp)) {
                    Text(tr("Из сессии ${relDay(last.day, d.today)}", "From the session ${relDay(last.day, d.today)}"), style = T.tiny)
                    Text(last.note, style = T.body, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
                if (g.note.isBlank() && last == null && !editNote) Text(tr("Пара слов о том, где вы сейчас, – чтобы через месяц не вспоминать заново.", "A few words on where you are – so that a month from now you needn't work it out again."), style = T.small)

                if (plays.isNotEmpty()) {
                    // three numbers, the rest in a line
                    val wins = plays.count { it.result == Play.WIN }; val losses = plays.count { it.result == Play.LOSS }; val coop = plays.count { it.result == Play.COOP_WIN }
                    Row(Modifier.padding(top = 16.dp).fillMaxWidth().card(18.dp)) {
                        Num(dur(min / plays.size * 60), tr("средняя сессия", "average session"), Modifier.weight(1f))
                        Num("${plays.map { it.day }.toSet().size}", tr("игровых дней", "play days"), Modifier.weight(1f))
                        if (wins + losses > 0) Num("$wins : $losses", tr("победы : поражения", "wins : losses"), Modifier.weight(1f))
                        else if (g.best > 0) Num("${g.best}", tr("рекорд", "best score"), Modifier.weight(1f))
                        else Num(dur((plays.maxOf { it.minutes }) * 60), tr("самая долгая", "the longest"), Modifier.weight(1f))
                    }
                    listOfNotNull(if (g.best > 0 && wins + losses > 0) tr("Рекорд – ${g.best}", "Best score – ${g.best}") else null, if (coop > 0) tr("побед вместе – $coop", "wins together – $coop") else null,
                        tr("первая сессия – ${fmtDayYear(plays.last().day)}", "first session – ${fmtDayYear(plays.last().day)}")).joinToString(" · ").let { Text(it, style = T.tiny, modifier = Modifier.padding(top = 6.dp)) }

                    // milestones
                    val marks = plays.filter { it.moment.isNotBlank() }
                    if (marks.isNotEmpty()) {
                        Label(tr("Вехи", "Milestones"))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            marks.take(12).forEach { m -> val big = m.moment == Play.M_FINISH || m.moment == Play.M_HUNDRED
                                Column(Modifier.card(14.dp, if (big) C.goldSoft else null).clickable { nav.push(Screen.PlayInfo(m.id)) }.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                    Text(Kinds.moment(m.moment), style = T.smallB.copy(color = if (big) C.gold else C.ink)); Text(fmtDayYear(m.day), style = T.tiny) } }
                        }
                    }

                    // the company
                    val with = plays.flatMap { it.friendIds }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.mapNotNull { e -> d.friendById[e.key]?.let { it to e.value } }
                    if (with.isNotEmpty()) {
                        Label(tr("С кем", "With whom"))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            with.forEach { (f, n) -> Row(Modifier.card(50.dp).clickable { nav.push(Screen.FriendInfo(f.id)) }.padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(f, 28.dp); Spacer(Modifier.width(8.dp)); Text(f.name, style = T.smallB); Text("  $n", style = T.small) } }
                        }
                    }

                    // the last twelve months
                    val first = LocalDate.ofEpochDay(d.today).withDayOfMonth(1).minusMonths(11)
                    val bars = (0 until 12).map { i -> val m = first.plusMonths(i.toLong()); val a = m.toEpochDay(); val b = m.plusMonths(1).toEpochDay() - 1
                        monthShort(m.monthValue) to plays.filter { it.day in a..b }.sumOf { it.minutes } / 60.0 }
                    if (bars.count { it.second > 0 } > 1) { Label(tr("Часы по месяцам", "Hours by month")); Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) { Bars(bars, color = col, h = 96.dp) } }
                }

                val shots = plays.flatMap { it.shotList }
                if (shots.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Альбом", "Album").uppercase() + " · ${shots.size}", style = T.sec, modifier = Modifier.weight(1f))
                        Text(tr("Все", "All"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { nav.push(Screen.Album(g.id)) }.padding(4.dp))
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        shots.take(10).forEachIndexed { i, n -> ShotThumb(n, 96.dp, Modifier.clickable { nav.push(Screen.Viewer(shots, i)) }) } }
                }

                if (plays.isNotEmpty()) {
                    Label(tr("Сессии", "Sessions"))
                    PlayGroup(if (all) plays else plays.take(6), d, nav, showGame = false, showDay = true)
                    if (!all && plays.size > 6) Ghost(tr("Показать все ${plays.size}", "Show all ${plays.size}"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { all = true }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun Round(icon: Icons, onClick: () -> Unit) =
    Box(Modifier.size(42.dp).clip(CircleShape).background(C.card.copy(alpha = .75f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, C.ink, 21.dp) }

@Composable
private fun Num(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.numS, maxLines = 1); Text(label, style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/** A new game, or an old one's facts. The catalogue fills them in from a title. */
@Composable
fun GameEditPage(scr: Screen.GameEdit, d: Data, nav: Nav) {
    val ctx = LocalContext.current; val c = ctx.container
    val scope = rememberCoroutineScope()
    val old = remember { d.gameById[scr.id] }
    var title by remember { mutableStateOf(old?.title ?: scr.title) }
    var kind by remember { mutableStateOf(old?.kind ?: d.s.kinds.firstOrNull { it in Game.KINDS } ?: Game.VIDEO) }
    var platform by remember { mutableStateOf(old?.platform ?: "") }
    var genres by remember { mutableStateOf(old?.genreList ?: emptyList()) }
    var year by remember { mutableStateOf(old?.year?.takeIf { it > 0 }?.toString() ?: "") }
    var maker by remember { mutableStateOf(old?.maker ?: "") }
    var status by remember { mutableStateOf(old?.status ?: Game.PLAYING) }
    var cover by remember { mutableStateOf(old?.cover ?: "") }
    var found by remember { mutableStateOf<List<Draft>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) scope.launch { Media.saveCover(ctx, uri)?.let { cover = it } } }

    fun take(dr: Draft) {
        title = dr.title; kind = dr.kind; if (dr.genres.isNotEmpty()) genres = dr.genres; if (dr.year > 0) year = dr.year.toString(); maker = dr.maker
        if (dr.platform.isNotBlank() && !Kinds.table(dr.kind)) platform = dr.platform
        found = null
        dr.coverUrl?.let { url -> scope.launch { busy = true; Catalog.download(url)?.let { b -> Media.saveCover(ctx, b)?.let { cover = it } }; busy = false } }
    }

    Page(if (old == null) tr("Новая игра", "New game") else tr("Игра", "Game"), { nav.pop() },
        action = { if (old != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable { del = true }, contentAlignment = Alignment.Center) { Icon(Icons.TRASH, C.bad, 20.dp) } }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Cover(Game(title = title, kind = kind, genres = genres.joinToString(","), cover = cover, added = 0), Modifier.size(78.dp, 104.dp), 12.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Field(title, { title = it; found = null }, tr("Название", "Title"), Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(if (busy) tr("Ищу…", "Searching…") else tr("Найти в каталоге", "Look it up"), small = true, enabled = title.trim().length >= 2 && !busy) {
                        busy = true; failed = false
                        scope.launch { val r = Catalog.search(title, Lang.ru); busy = false; if (r == null) failed = true else found = r }
                    }
                    if (cover.isNotBlank()) Ghost(tr("Без обложки", "No cover"), small = true) { cover = "" }
                }
            }
        }
        if (failed) Text(tr("Каталог не отвечает. Заполните вручную – это быстро.", "The catalogue does not answer. Fill it in by hand – it's quick."), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 8.dp))
        found?.let { list ->
            Column(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
                if (list.isEmpty()) Text(tr("Ничего не нашлось. Не беда – заполните вручную.", "Nothing found. Never mind – fill it in by hand."), style = T.small, modifier = Modifier.padding(vertical = 8.dp))
                list.forEach { dr ->
                    Row(Modifier.fillMaxWidth().clickable { take(dr) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(kindColor(dr.kind).copy(alpha = .15f)), contentAlignment = Alignment.Center) { Icon(app.questling.ui.components.kindIcon(dr.kind), kindColor(dr.kind), 18.dp) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(dr.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(Kinds.title(dr.kind), dr.year.takeIf { it > 0 }?.toString(), dr.maker.takeIf { it.isNotBlank() }).joinToString(" · "), style = T.small, maxLines = 1)
                        }
                    }
                }
            }
        }
        Label(tr("Что это", "What it is"))
        ChipRow(Game.KINDS.map { Kinds.title(it) }, Game.KINDS.indexOf(kind)) { kind = Game.KINDS[it]; genres = genres.filter { g -> Genres.forKind(kind).take(20).any { x -> x.id == g } } }
        if (!Kinds.table(kind)) {
            Label(tr("Платформа", "Platform"))
            val plats = (d.s.platforms + Kinds.PLATFORMS).distinct()
            ChipRow(plats.map { Kinds.platformTitle(it) }, plats.indexOf(platform)) { platform = if (platform == plats[it]) "" else plats[it] }
        }
        Label(tr("Жанры – до трёх", "Genres – up to three"))
        GenrePicker(kind, genres) { genres = it }
        Label(tr("Статус", "Status"))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Kinds.STATUSES.forEach { st -> Chip(Kinds.status(st), status == st, color = statusColor(st)) { status = st } }
        }
        Label(tr("Год и автор", "Year and maker"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(year, { v -> year = v.filter { it.isDigit() }.take(4) }, tr("Год", "Year"), Modifier.width(96.dp), number = true)
            Field(maker, { maker = it }, tr("Студия или издатель", "Studio or publisher"), Modifier.weight(1f))
        }
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 22.dp), enabled = title.isNotBlank()) {
            scope.launch {
                val g = (old ?: Game(title = "", added = 0)).copy(title = title.trim(), kind = kind, platform = if (Kinds.table(kind)) "" else platform, genres = genres.joinToString(","), cover = cover,
                    year = year.toIntOrNull() ?: 0, maker = maker.trim())
                val id = c.saveGame(if (old == null) g.copy(status = status) else g)
                if (old != null) { if (status != old.status) c.setStatus(g.copy(id = id, status = old.status), status); if (old.cover != cover) Media.deleteCover(ctx, old.cover) }
                nav.pop()
                if (scr.thenPlay) { nav.startGame = id; nav.start = true }
            }
        }
        if (old == null) Text(tr("Каталог – это Wikidata и Википедия: в сеть уходит только название игры.", "The catalogue is Wikidata and Wikipedia: only the game's title goes online."), style = T.tiny, modifier = Modifier.padding(top = 10.dp))
    }
    if (del && old != null) Popup({ del = false }) {
        val n = d.byGame[old.id]?.size ?: 0
        Text(tr("Удалить игру?", "Delete the game?"), style = T.h2)
        Text(if (n > 0) tr("Вместе с ней удалятся ${count(n, "сессия", "сессии", "сессий", "", "")} и их снимки. Питомцы и трофеи останутся.", "Its $n sessions and their pictures go with it. Creatures and trophies stay.")
            else tr("В неё ещё не играли.", "It has not been played yet."), style = T.body.copy(color = C.mute), modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Оставить", "Keep"), Modifier.weight(1f)) { del = false }
            Btn(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) {
                Media.delete(ctx, d.byGame[old.id].orEmpty().flatMap { it.shotList }); Media.deleteCover(ctx, old.cover)
                c.write { deleteGame(old.id) }; del = false; nav.stack.removeAll { it == scr || it == Screen.GameInfo(old.id) } }
        }
    }
}
