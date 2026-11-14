package app.questling.ui.play

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.data.Friend
import app.questling.data.Media
import app.questling.domain.Kinds
import app.questling.domain.tr
import app.questling.notify.Notes
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.art.PetView
import app.questling.ui.art.Pose
import app.questling.ui.components.Avatar
import app.questling.ui.components.Btn
import app.questling.ui.components.Cover
import app.questling.ui.components.Field
import app.questling.ui.components.FriendPicker
import app.questling.ui.components.Ghost
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Page
import app.questling.ui.components.Popup
import app.questling.ui.components.Sheet
import app.questling.ui.components.card
import app.questling.ui.home.clock
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The session running: the clock, a break, the company, the table tools and the score sheet.
 * When it has been long, the pet yawns and asks for a break – once.
 */
@Composable
fun LivePage(d: Data, nav: Nav) {
    val ctx = LocalContext.current; val c = ctx.container
    val l = d.s.live
    val g = d.liveGame
    if (!l.on) { LaunchedEffect(Unit) { nav.stack.remove(Screen.Live) }; return }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(500) } }
    var finish by remember { mutableStateOf(false) }
    var drop by remember { mutableStateOf(false) }
    var people by remember { mutableStateOf(false) }
    var newFriend by remember { mutableStateOf(false) }
    val played = l.played(now)
    val onBreak = l.pausedSince > 0
    val yawn = d.s.breakAfter > 0 && !l.yawned && !onBreak && played >= d.s.breakAfter * 60_000L
    val table = g != null && Kinds.table(g.kind)

    Page(g?.title ?: tr("Сессия", "Session"), { nav.pop() }) {
        Column(Modifier.fillMaxWidth().card(22.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(g, Modifier.size(60.dp, 80.dp), 10.dp)
                Spacer(Modifier.width(14.dp))
                d.leader?.let { PetView(it, 96.dp, if (onBreak) Pose.SLEEP else if (yawn) Pose.IDLE else Pose.WORK) }
            }
            Text(clock(played), style = T.big, modifier = Modifier.padding(top = 8.dp))
            Text(listOfNotNull(if (onBreak) tr("перерыв ${clock(now - l.pausedSince)}", "break ${clock(now - l.pausedSince)}") else null,
                l.platform.takeIf { it.isNotBlank() }?.let { Kinds.platformTitle(it) },
                l.breaks.takeIf { it > 0 }?.let { tr("перерывов: $it", "breaks: $it") }).joinToString(" · "), style = T.small, textAlign = TextAlign.Center)
            Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Ghost(if (onBreak) tr("Продолжить", "Resume") else tr("Перерыв", "Break"), Modifier.weight(1f)) { c.write { if (onBreak) resumeLive() else pauseLive() } }
                Btn(tr("Закончить", "Finish"), Modifier.weight(1f)) { finish = true }
            }
        }

        if (yawn) Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.goldSoft).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("${d.leader?.name ?: "Питомец"} зевает", "${d.leader?.name ?: "Your pet"} is yawning"), style = T.bodyB)
                Text(tr("Вы играете уже ${clock(played)}. Разомнёмся пять минут?", "You have been playing for ${clock(played)}. Five minutes to stretch?"), style = T.small.copy(color = C.ink))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(tr("Перерыв", "Take a break"), small = true, color = C.gold) { c.write { yawned(); pauseLive(heeded = true) } }
                    Ghost(tr("Позже", "Later"), small = true) { c.write { yawned() } }
                }
            }
        }

        // the company
        Label(tr("Кто играет", "Who is playing"))
        Row(Modifier.fillMaxWidth().card(18.dp).clickable { people = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(null, 34.dp, d.s.player)
            l.friends.mapNotNull { d.friendById[it] }.forEach { f -> Spacer(Modifier.width(6.dp)); Avatar(f, 34.dp) }
            Spacer(Modifier.width(10.dp))
            Text(if (l.friends.isEmpty()) tr("Играю в одиночку", "Playing solo") else l.friends.mapNotNull { d.friendById[it]?.name }.joinToString(", "), style = T.body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.EDIT, C.mute, 18.dp)
        }

        // the table tools
        Label(tr("Инструменты", "Tools"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolDoor(Icons.DICE, tr("Кубики", "Dice"), Modifier.weight(1f)) { nav.push(Screen.Tools(0)) }
            ToolDoor(Icons.CROWN, tr("Первый ход", "First move"), Modifier.weight(1f)) { nav.push(Screen.Tools(1)) }
            ToolDoor(Icons.HOURGLASS, tr("Таймер хода", "Turn timer"), Modifier.weight(1f)) { nav.push(Screen.Tools(2)) }
        }

        // the score sheet
        if (table || l.friends.isNotEmpty() || l.scores.isNotEmpty()) {
            Label(tr("Протокол очков", "Score sheet"))
            Column(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
                (listOf<Friend?>(null) + l.friends.mapNotNull { d.friendById[it] }).forEach { f ->
                    val id = f?.id ?: 0L; val v = l.scores[id] ?: 0
                    fun set(n: Int) = c.write { val first = settings.now().live.scores.isEmpty(); liveScores(settings.now().live.scores + (id to n)); if (first) tool("score") }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(f, 30.dp, d.s.player); Spacer(Modifier.width(10.dp))
                        Text(f?.name ?: d.s.player.ifBlank { tr("Я", "Me") }, style = T.body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Step("−5") { set(v - 5) }; Step("−1") { set(v - 1) }
                        Text("$v", style = T.num, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                        Step("+1") { set(v + 1) }; Step("+5") { set(v + 5) }
                    }
                }
            }
        }
        Text(tr("Отменить сессию, ничего не записывая", "Drop the session without writing it down"), style = T.small.copy(color = C.bad),
            modifier = Modifier.padding(top = 22.dp).fillMaxWidth().clickable { drop = true }.padding(8.dp), textAlign = TextAlign.Center)
    }

    if (people) Sheet({ people = false }) {
        Text(tr("Кто играет", "Who is playing"), style = T.h2, modifier = Modifier.padding(bottom = 10.dp))
        FriendPicker(d.friends, l.friends, onAdd = { newFriend = true }) { c.write { liveFriends(it) } }
        Btn(tr("Готово", "Done"), Modifier.fillMaxWidth().padding(top = 16.dp)) { people = false }
    }
    if (newFriend) NewFriend({ newFriend = false }) { id -> c.write { liveFriends(settings.now().live.friends + id) } }
    if (drop) Popup({ drop = false }) {
        Text(tr("Отменить сессию?", "Drop the session?"), style = T.h2)
        Text(tr("Время не будет записано.", "The time will not be written down."), style = T.body.copy(color = C.mute), modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Играю дальше", "Keep playing"), Modifier.weight(1f)) { drop = false }
            Btn(tr("Отменить", "Drop it"), Modifier.weight(1f), color = C.bad) { Notes.stopLive(ctx); c.write { cancelLive() }; drop = false; nav.stack.remove(Screen.Live) }
        }
    }
    if (finish) FinishSheet(d, nav, now) { finish = false }
}

@Composable
private fun Step(label: String, onClick: () -> Unit) {
    Box(Modifier.padding(horizontal = 2.dp).size(36.dp, 34.dp).clip(RoundedCornerShape(10.dp)).background(C.card2).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(label, style = T.smallB) }
}

@Composable
private fun ToolDoor(icon: Icons, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.card(16.dp).clickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(C.accSoft), contentAlignment = Alignment.Center) { Icon(icon, C.acc, 21.dp) }
        Text(label, style = T.tiny.copy(color = C.ink), modifier = Modifier.padding(top = 6.dp), maxLines = 1)
    }
}

/** The end of a session: how long it really was and everything worth keeping about it. */
@Composable
private fun FinishSheet(d: Data, nav: Nav, now: Long, onClose: () -> Unit) {
    val ctx = LocalContext.current; val c = ctx.container
    val scope = rememberCoroutineScope()
    val l = d.s.live; val g = d.liveGame
    val end = remember { if (l.pausedSince > 0) l.pausedSince else now }
    val dr = remember { PlayDraft(scores = l.scores) }
    var minutes by remember { mutableStateOf(((l.played(end) + 30_000) / 60_000).coerceAtLeast(1).toString()) }
    var found by remember { mutableStateOf(emptyList<Uri>()) }
    var canRead by remember { mutableStateOf(Media.canRead(ctx)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canRead = it }
    LaunchedEffect(canRead) { if (canRead && d.s.shotsAuto) { found = withContext(Dispatchers.IO) { Media.screenshots(ctx, l.start, end) }; dr.picked = found.toSet() } }
    var saving by remember { mutableStateOf(false) }
    Sheet(onClose) {
        Text(tr("Сессия окончена", "Session over"), style = T.h2)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Сыграно, минут", "Played, minutes"), style = T.body, modifier = Modifier.weight(1f))
            Field(minutes, { v -> minutes = v.filter { it.isDigit() }.take(4) }, "0", Modifier.width(96.dp), number = true, center = true)
        }
        PlayFields(dr, g, l.friends, d, found)
        if (d.s.shotsAuto && !canRead) Text(tr("Разрешить подхватывать скриншоты, сделанные во время сессии", "Let the app pick up screenshots made during a session"), style = T.smallB.copy(color = C.acc),
            modifier = Modifier.padding(top = 8.dp).clickable { ask.launch(Media.readPermission) }.padding(vertical = 4.dp))
        Btn(tr("Записать", "Write it down"), Modifier.fillMaxWidth().padding(top = 18.dp), enabled = !saving) {
            saving = true
            scope.launch {
                Notes.stopLive(ctx)
                val shots = dr.allShots(ctx)
                val id = c.finishLive(dr.result, dr.myScore, dr.mood, dr.note, shots, dr.moment, minutes.toIntOrNull() ?: 1, dr.seats(l.friends))
                onClose(); nav.stack.remove(Screen.Live); if (id > 0) nav.push(Screen.PlayInfo(id))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
