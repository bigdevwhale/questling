package app.questling.ui.play

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.questling.container
import app.questling.data.Friend
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.components.Avatar
import app.questling.ui.components.Btn
import app.questling.ui.components.ChipRow
import app.questling.ui.components.FriendPicker
import app.questling.ui.components.Ghost
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Page
import app.questling.ui.components.Segments
import app.questling.ui.components.card
import app.questling.ui.components.friendColor
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val SIDES = listOf(4, 6, 8, 10, 12, 20, 100)

/** Three small things for the table: dice, «who goes first» and a turn timer. */
@Composable
fun ToolsPage(tab0: Int, d: Data, nav: Nav) {
    var tab by remember { mutableIntStateOf(tab0.coerceIn(0, 2)) }
    Page(tr("Инструменты", "Tools"), { nav.pop() }) {
        Segments(listOf(tr("Кубики", "Dice"), tr("Первый ход", "First move"), tr("Таймер хода", "Turn timer")), tab) { tab = it }
        Spacer(Modifier.height(14.dp))
        when (tab) { 0 -> Dice(d); 1 -> First(d); else -> Turn(d) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Dice(d: Data) {
    val c = LocalContext.current.container
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val n0 = d.s.dice.substringBefore('d').toIntOrNull()?.coerceIn(1, 8) ?: 1
    val s0 = d.s.dice.substringAfter('d').toIntOrNull()?.takeIf { it in SIDES } ?: 20
    var n by remember { mutableIntStateOf(n0) }
    var sides by remember { mutableIntStateOf(s0) }
    var roll by remember { mutableStateOf(emptyList<Int>()) }
    var rolling by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(emptyList<String>()) }
    ChipRow(SIDES.map { "d$it" }, SIDES.indexOf(sides)) { sides = SIDES[it]; roll = emptyList() }
    Row(Modifier.padding(top = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Сколько кубиков", "How many dice"), style = T.body, modifier = Modifier.weight(1f))
        Round(Icons.MINUS) { n = (n - 1).coerceAtLeast(1); roll = emptyList() }
        Text("$n", style = T.num, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
        Round(Icons.PLUS) { n = (n + 1).coerceAtMost(8); roll = emptyList() }
    }
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().card(22.dp).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            (if (roll.isEmpty()) List(n) { 0 } else roll).forEach { v ->
                val crit = !rolling && v == sides && sides >= 6; val fail = !rolling && v == 1 && roll.isNotEmpty() && sides >= 6
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(if (crit) C.gold else if (fail) C.card2 else C.acc), contentAlignment = Alignment.Center) {
                    Text(if (v == 0) "?" else "$v", style = T.num.copy(color = if (fail) C.mute else Color.White, fontSize = if (sides == 100) 20.sp else 26.sp)) }
            }
        }
        if (roll.size > 1 && !rolling) Text(tr("Сумма: ${roll.sum()}", "Total: ${roll.sum()}"), style = T.h3, modifier = Modifier.padding(top = 12.dp))
        if (roll.isNotEmpty() && !rolling && sides >= 6 && roll.any { it == sides }) Text(tr("Критический успех!", "Critical hit!"), style = T.smallB.copy(color = C.gold), modifier = Modifier.padding(top = 4.dp))
    }
    Btn(tr("Бросить", "Roll"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = !rolling) {
        rolling = true
        scope.launch {
            repeat(9) { roll = List(n) { Random.nextInt(1, sides + 1) }; view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); delay(55L + it * 12) }
            rolling = false
            history = (listOf("${n}d$sides: " + roll.joinToString(" + ") + if (n > 1) " = ${roll.sum()}" else "") + history).take(6)
            c.tool("dice"); c.settings.update { it.copy(dice = "${n}d$sides") }
        }
    }
    if (history.isNotEmpty()) { Label(tr("Последние броски", "Last rolls")); history.forEach { Text(it, style = T.small, modifier = Modifier.padding(vertical = 2.dp)) } }
}

@Composable
private fun Round(icon: Icons, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(C.card2).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, C.ink, 18.dp) }
}

@Composable
private fun First(d: Data) {
    val c = LocalContext.current.container
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var who by remember { mutableStateOf(if (d.s.live.on) d.s.live.friends else emptyList()) }
    var at by remember { mutableIntStateOf(-1) }
    var spinning by remember { mutableStateOf(false) }
    var newFriend by remember { mutableStateOf(false) }
    val seats: List<Friend?> = listOf<Friend?>(null) + who.mapNotNull { d.friendById[it] }
    Text(tr("Кто за столом", "Who is at the table"), style = T.sec, modifier = Modifier.padding(bottom = 8.dp))
    FriendPicker(d.friends, who, onAdd = { newFriend = true }) { who = it; at = -1 }
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().card(22.dp).padding(14.dp)) {
        seats.forEachIndexed { i, f ->
            val on = i == at
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(if (on) C.goldSoft else Color.Transparent)
                .border(if (on && !spinning) 2.dp else 0.dp, if (on && !spinning) C.gold else Color.Transparent, RoundedCornerShape(14.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(f, 36.dp, d.s.player); Spacer(Modifier.width(12.dp))
                Text(f?.name ?: d.s.player.ifBlank { tr("Я", "Me") }, style = T.bodyB, modifier = Modifier.weight(1f))
                if (on && !spinning) Icon(Icons.CROWN, C.gold, 22.dp)
            }
        }
    }
    if (at >= 0 && !spinning) Text(tr("Первым ходит: ${seats.getOrNull(at)?.name ?: d.s.player.ifBlank { "вы" }}", "First to move: ${seats.getOrNull(at)?.name ?: d.s.player.ifBlank { "you" }}"),
        style = T.h3, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), textAlign = TextAlign.Center)
    Btn(tr("Кто ходит первым?", "Who goes first?"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = !spinning && seats.size >= 2) {
        spinning = true
        scope.launch {
            val winner = Random.nextInt(seats.size); val steps = seats.size * 3 + winner
            for (k in 0..steps) { at = k % seats.size; view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); delay(60L + k * 9) }
            spinning = false; c.tool("first")
        }
    }
    if (seats.size < 2) Text(tr("Отметьте хотя бы одного игрока.", "Tick at least one player."), style = T.small, modifier = Modifier.padding(top = 8.dp))
    if (newFriend) NewFriend({ newFriend = false }) { who = who + it }
}

private val TURNS = listOf(30, 45, 60, 90, 120, 180, 300)

@Composable
private fun Turn(d: Data) {
    val c = LocalContext.current.container
    val view = LocalView.current
    var sec by remember { mutableIntStateOf(d.s.turn.takeIf { it in TURNS } ?: 60) }
    var end by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var turns by remember { mutableIntStateOf(0) }
    var buzzed by remember { mutableStateOf(false) }
    LaunchedEffect(end) { while (end > 0) { now = System.currentTimeMillis(); delay(100) } }
    val left = if (end > 0) ((end - now) / 1000f).coerceAtLeast(0f) else sec.toFloat()
    val over = end > 0 && left <= 0f
    if (over && !buzzed) { buzzed = true; view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
    ChipRow(TURNS.map { if (it < 60) tr("$it с", "$it s") else tr("${it / 60} мин" + if (it % 60 > 0) " ${it % 60} с" else "", "${it / 60} min" + if (it % 60 > 0) " ${it % 60} s" else "") }, TURNS.indexOf(sec)) {
        sec = TURNS[it]; end = 0; c.write { settings.update { s -> s.copy(turn = sec) } } }
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().card(22.dp).background(if (over) C.bad.copy(alpha = .14f) else Color.Transparent).padding(vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val t = kotlin.math.ceil(left.toDouble()).toInt()
        Text("%d:%02d".format(t / 60, t % 60), style = T.big.copy(fontSize = 64.sp, lineHeight = 70.sp, color = if (over) C.bad else if (left <= 10 && end > 0) C.gold else C.ink))
        Text(if (over) tr("Время вышло!", "Time's up!") else if (turns > 0) tr("Ход $turns", "Turn $turns") else tr("Нажмите «Старт»", "Press Start"), style = T.small)
        Box(Modifier.padding(top = 14.dp, start = 26.dp, end = 26.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(C.track)) {
            Box(Modifier.fillMaxWidth((left / sec).coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(50)).background(if (left <= 10 && end > 0) C.gold else C.acc)) }
    }
    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (end > 0) Ghost(tr("Стоп", "Stop"), Modifier.weight(1f)) { end = 0; turns = 0 }
        Btn(if (end > 0) tr("Следующий ход", "Next turn") else tr("Старт", "Start"), Modifier.weight(1f)) {
            if (end == 0L) c.write { tool("timer") }
            end = System.currentTimeMillis() + sec * 1000L; now = System.currentTimeMillis(); turns++; buzzed = false }
    }
}
