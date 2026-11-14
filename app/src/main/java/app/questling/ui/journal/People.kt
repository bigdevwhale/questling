package app.questling.ui.journal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.questling.container
import app.questling.data.Media
import app.questling.domain.With
import app.questling.domain.cap
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
import app.questling.ui.components.Btn
import app.questling.ui.components.Empty
import app.questling.ui.components.Field
import app.questling.ui.components.GameRow
import app.questling.ui.components.Ghost
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Page
import app.questling.ui.components.Split
import app.questling.ui.components.TileGrid
import app.questling.ui.components.card
import app.questling.ui.components.tap
import app.questling.ui.play.Label
import app.questling.ui.play.NewFriend
import app.questling.ui.play.ShotGrid
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Everybody the player plays with: hours together and the score between them. */
@Composable
fun FriendsPage(d: Data, nav: Nav) {
    var add by remember { mutableStateOf(false) }
    val rows = remember(d.friends, d.plays, d.seats) { d.friends.filter { !it.archived }.map { With(it, d.plays, d.seats, d.gameById) }.sortedByDescending { it.minutes } }
    Page(tr("Игроки", "Players"), { nav.pop() }, action = {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { add = true }, contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.ink, 22.dp) } }) {
        if (rows.isEmpty()) Empty(tr("Добавьте тех, с кем играете: друзей, партнёра, детей.\nПриложение запомнит, кто у кого выигрывает.", "Add the people you play with: friends, a partner, children.\nThe app will remember who beats whom."))
        rows.forEach { w ->
            Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.FriendInfo(w.friend.id)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(w.friend, 44.dp); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(w.friend.name, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (w.plays.isEmpty()) tr("ещё не играли вместе", "no games together yet") else
                        "${count(w.plays.size, "сессия", "сессии", "сессий", "session", "sessions")} · ${dur(w.minutes * 60)}", style = T.small)
                }
                if (w.won + w.lost > 0) Text("${w.won} : ${w.lost}", style = T.numS.copy(color = if (w.won >= w.lost) C.ok else C.bad))
            }
        }
    }
    if (add) NewFriend({ add = false }) {}
}

/** One friend: the score between you, the favourite game, the sessions together. */
@Composable
fun FriendPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val f = d.friendById[id] ?: run { if (d.friends.isNotEmpty()) LaunchedEffect(Unit) { nav.stack.remove(Screen.FriendInfo(id)) }; return }
    val w = remember(f, d.plays, d.seats) { With(f, d.plays, d.seats, d.gameById) }
    var name by remember(f.id) { mutableStateOf(f.name) }
    var edit by remember { mutableStateOf(false) }
    Page(f.name, { nav.pop() }, action = {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { edit = !edit }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.ink, 20.dp) } }) {
        Row(Modifier.fillMaxWidth().card(22.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(f, 64.dp); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = T.h2)
                Text(if (w.last > 0) tr("Последняя игра – ${relDay(w.last, d.today)}", "Last game – ${relDay(w.last, d.today)}") else tr("Ещё не играли вместе", "No games together yet"), style = T.small)
            }
        }
        if (edit) {
            Label(tr("Имя", "Name"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Field(name, { name = it }, tr("Имя", "Name"), Modifier.weight(1f))
                Btn(tr("Сохранить", "Save"), small = true, enabled = name.isNotBlank()) { c.write { saveFriend(f.copy(name = name.trim())) }; edit = false }
            }
            Ghost(tr("Убрать из списка игроков", "Remove from the players"), Modifier.padding(top = 10.dp), color = C.bad, small = true) { c.write { saveFriend(f.copy(archived = true)) }; nav.pop() }
            Text(tr("Сессии с этим игроком останутся в журнале.", "The sessions with this player stay in the journal."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        if (w.plays.isNotEmpty()) {
            Label(tr("Вместе", "Together"))
            TileGrid(listOfNotNull(
                Triple("${w.plays.size}", tr("сессий", "sessions"), null as String?), Triple(dur(w.minutes * 60), tr("наиграно", "played"), null),
                if (w.coop > 0) Triple("${w.coop}", tr("общих побед", "wins together"), null) else null,
                Triple("${w.plays.map { it.gameId }.toSet().size}", tr("разных игр", "different games"), null)))
            if (w.won + w.lost > 0) {
                Label(tr("Счёт между вами", "The score between you"))
                Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
                    Split(w.won.toDouble(), w.lost.toDouble(), d.s.player.ifBlank { tr("Я", "Me") } + " ${w.won} ·", "· ${w.lost} ${f.name}", C.ok, C.bad) }
            }
            w.top?.let { g -> Label(tr("Любимая игра на двоих", "Your game together")); GameRow(g, dur(w.plays.filter { it.gameId == g.id }.sumOf { it.minutes } * 60)) { nav.push(Screen.GameInfo(g.id)) } }
            Label(tr("Сессии", "Sessions"))
            PlayGroup(w.plays.take(20), d, nav)
        }
    }
}

/** Every picture kept, the newest first; [gameId] 0 is the whole album. */
@Composable
fun AlbumPage(gameId: Long, d: Data, nav: Nav) {
    val plays = if (gameId == 0L) d.plays else d.byGame[gameId].orEmpty()
    val groups = remember(plays) { plays.filter { it.shotList.isNotEmpty() } }
    val all = remember(groups) { groups.flatMap { it.shotList } }
    Page(if (gameId == 0L) tr("Альбом", "Album") else d.gameById[gameId]?.title ?: tr("Альбом", "Album"), { nav.pop() }) {
        if (all.isEmpty()) Empty(tr("Здесь соберутся скриншоты и фото ваших сессий.\nИх можно добавить, когда заканчиваете сессию.", "Screenshots and photos of your sessions gather here.\nAdd them when you finish a session."))
        else Text(count(all.size, "снимок", "снимка", "снимков", "picture", "pictures"), style = T.small, modifier = Modifier.padding(bottom = 4.dp))
        var at = 0
        groups.forEach { p ->
            val base = at; at += p.shotList.size
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp).clickable { nav.push(Screen.PlayInfo(p.id)) }) {
                Text((if (gameId == 0L) (d.gameById[p.gameId]?.title ?: "–") + " · " else "") + fmtDayYear(p.day), style = T.smallB, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hm(minOfDay(p.start)), style = T.small)
            }
            ShotGrid(p.shotList) { nav.push(Screen.Viewer(all, base + it)) }
        }
    }
}

/** One picture on the whole screen; a swipe or the arrows move to the next. */
@Composable
fun ViewerPage(scr: Screen.Viewer, nav: Nav) {
    val ctx = LocalContext.current
    var at by remember { mutableIntStateOf(scr.at.coerceIn(0, (scr.shots.size - 1).coerceAtLeast(0))) }
    var drag by remember { mutableFloatStateOf(0f) }
    androidx.activity.compose.BackHandler { nav.pop() }
    val name = scr.shots.getOrNull(at) ?: return
    val img by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, name) { value = withContext(Dispatchers.IO) { Media.shot(ctx, name, 1600) } }
    Box(Modifier.fillMaxSize().background(Color.Black).tap {}.pointerInput(scr.shots.size) {
        detectHorizontalDragGestures(onDragEnd = { if (drag < -80 && at < scr.shots.size - 1) at++ else if (drag > 80 && at > 0) at--; drag = 0f }) { _, dx -> drag += dx } }) {
        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        Row(Modifier.statusBarsPadding().fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x66000000)).clickable { nav.pop() }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, Color.White, 22.dp) }
            Text("${at + 1} / ${scr.shots.size}", style = T.smallB.copy(color = Color.White), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x66000000)).clickable {
                runCatching { val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", Media.shotFile(ctx, name))
                    ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("image/jpeg").putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION), null)) } }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, Color.White, 20.dp) }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000)).clickable(enabled = at > 0) { at-- }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, Color.White.copy(alpha = if (at > 0) 1f else .3f), 22.dp) }
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000)).clickable(enabled = at < scr.shots.size - 1) { at++ }, contentAlignment = Alignment.Center) { Icon(Icons.NEXT, Color.White.copy(alpha = if (at < scr.shots.size - 1) 1f else .3f), 22.dp) }
        }
    }
}

/** Stretches of phone games the usage tracker found: each becomes a session with one tap, or is dropped. */
@Composable
fun FoundPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    Page(tr("Найдено в телефоне", "Found on the phone"), { nav.pop() }) {
        Text(tr("Телефон сам знает, во что и сколько вы играли. Подтвердите – и сессия попадёт в журнал.", "The phone knows what you played and for how long. Confirm – and the session goes into the journal."), style = T.small, modifier = Modifier.padding(bottom = 10.dp))
        if (d.s.found.isEmpty()) Empty(tr("Пока ничего нового.", "Nothing new yet."))
        d.s.found.sortedByDescending { it.start }.forEach { f ->
            Column(Modifier.padding(bottom = 8.dp).fillMaxWidth().card(18.dp).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(C.okSoft), contentAlignment = Alignment.Center) { Icon(Icons.PHONE, C.ok, 20.dp) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.label, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${relDay(app.questling.domain.dayOf(f.start), d.today).cap()} · ${hm(minOfDay(f.start))}", style = T.small)
                    }
                    Text(dur(f.minutes * 60), style = T.numS)
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(tr("В журнал", "Into the journal"), Modifier.weight(1f), small = true) { c.write { confirmFound(f) } }
                    Ghost(tr("Не надо", "Skip"), small = true) { c.write { dropFound(f, false) } }
                    Ghost(tr("Это не игра", "Not a game"), small = true, color = C.bad) { c.write { dropFound(f, true) } }
                }
            }
        }
        if (d.s.found.size > 1) Btn(tr("Записать все", "Write all down"), Modifier.fillMaxWidth().padding(top = 8.dp)) { c.write { settings.now().found.forEach { confirmFound(it) } } }
    }
}
