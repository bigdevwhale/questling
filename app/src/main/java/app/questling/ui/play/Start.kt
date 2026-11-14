package app.questling.ui.play

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.data.Friend
import app.questling.data.Game
import app.questling.domain.Kinds
import app.questling.domain.dur
import app.questling.domain.relDay
import app.questling.domain.tr
import app.questling.notify.Notes
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.components.Btn
import app.questling.ui.components.ChipRow
import app.questling.ui.components.Cover
import app.questling.ui.components.Field
import app.questling.ui.components.FriendPicker
import app.questling.ui.components.GameRow
import app.questling.ui.components.Ghost
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Popup
import app.questling.ui.components.Sheet
import app.questling.ui.theme.C
import app.questling.ui.theme.T

/** The platforms to offer for a game: «at the table» for tabletop kinds, else the player's own, the game's first. */
fun platformsFor(g: Game?, d: Data): List<String> {
    if (g != null && Kinds.table(g.kind)) return listOf("table")
    val mine = d.s.platforms.ifEmpty { Kinds.PLATFORMS }
    val first = listOfNotNull(d.last(g?.id ?: -1)?.platform?.takeIf { it.isNotBlank() }, g?.platform?.takeIf { it.isNotBlank() })
    return (first + mine + Kinds.PLATFORMS).distinct()
}

/** A small dialog that adds a player. */
@Composable
fun NewFriend(onClose: () -> Unit, onAdded: (Long) -> Unit) {
    val c = LocalContext.current.container
    var name by remember { mutableStateOf("") }
    Popup(onClose) {
        Text(tr("Новый игрок", "New player"), style = T.h2)
        Text(tr("С кем вы играете: друг, партнёр, ребёнок, коллега.", "Somebody you play with: a friend, a partner, a child, a colleague."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        Field(name, { name = it }, tr("Имя", "Name"), Modifier.fillMaxWidth())
        Btn(tr("Добавить", "Add"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = name.isNotBlank()) {
            c.write { val id = saveFriend(Friend(name = name.trim(), created = 0)); onAdded(id) }; onClose() }
    }
}

/** «What are we playing?» – pick a game, a platform and the company, then start the clock or write a past session down. */
@Composable
fun StartSheet(d: Data, nav: Nav, onClose: () -> Unit) {
    val ctx = LocalContext.current; val c = ctx.container
    var q by remember { mutableStateOf("") }
    var game by remember { mutableStateOf(d.gameById[nav.startGame]) }
    var platform by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf(emptyList<Long>()) }
    var newFriend by remember { mutableStateOf(false) }
    Sheet({ nav.startGame = 0; onClose() }) {
        val g = game
        if (g == null) {
            Text(tr("Во что играем?", "What are we playing?"), style = T.h2)
            Field(q, { q = it }, tr("Найти игру или добавить новую", "Find a game or add a new one"), Modifier.fillMaxWidth().padding(top = 10.dp))
            val list = d.recent.filter { q.isBlank() || it.title.contains(q.trim(), ignoreCase = true) }.take(if (q.isBlank()) 6 else 12)
            Spacer(Modifier.height(6.dp))
            list.forEach { x ->
                val last = d.last(x.id)
                GameRow(x, listOfNotNull(Kinds.title(x.kind), last?.let { relDay(it.day, d.today) }, d.minutes(x.id).takeIf { it > 0 }?.let { dur(it * 60) }).joinToString(" · ")) { game = x }
            }
            if (d.games.isEmpty() && q.isBlank()) Text(tr("Здесь появятся ваши игры. Начните с названия – видеоигра, настолка, что угодно.", "Your games will show here. Start with a title – a video game, a board game, anything."),
                style = T.small, modifier = Modifier.padding(vertical = 10.dp))
            val exact = d.games.any { it.title.equals(q.trim(), ignoreCase = true) }
            if (!exact) Row(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.accSoft).clickable {
                onClose(); nav.push(Screen.GameEdit(title = q.trim(), thenPlay = true)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(C.acc), contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.accInk, 18.dp) }
                Spacer(Modifier.width(10.dp))
                Text(if (q.isBlank()) tr("Добавить новую игру", "Add a new game") else tr("Добавить «${q.trim()}»", "Add «${q.trim()}»"), style = T.bodyB.copy(color = C.acc), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            val plats = remember(g.id) { platformsFor(g, d) }
            if (platform.isBlank() || platform !in plats) platform = plats.first()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(g, Modifier.size(54.dp, 72.dp), 10.dp); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.title, style = T.h2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(tr("Другая игра", "Another game"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { game = null }.padding(vertical = 4.dp))
                }
            }
            if (plats.size > 1) {
                Text(tr("На чём", "On what").uppercase(), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                ChipRow(plats.map { Kinds.platformTitle(it) }, plats.indexOf(platform)) { platform = plats[it] }
            }
            Text(tr("С кем", "With whom").uppercase(), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            FriendPicker(d.friends, friends, onAdd = { newFriend = true }) { friends = it }
            if (friends.isEmpty()) Text(tr("Никого не отмечено – играю в одиночку.", "Nobody ticked – playing solo."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Ghost(tr("Уже сыграли", "Already played"), Modifier.weight(1f)) { nav.startGame = 0; onClose(); nav.push(Screen.PlayEdit(gameId = g.id)) }
                Btn(tr("Начать", "Start"), Modifier.weight(1f)) {
                    c.write { startLive(g.id, platform, friends); Notes.live(ctx, g.title, System.currentTimeMillis(), d.s.breakAfter) }
                    nav.startGame = 0; onClose(); nav.push(Screen.Live) }
            }
        }
    }
    if (newFriend) NewFriend({ newFriend = false }) { friends = friends + it }
}
