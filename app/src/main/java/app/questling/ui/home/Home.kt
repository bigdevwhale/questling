package app.questling.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.domain.Quests
import app.questling.domain.Season
import app.questling.domain.WeekFacts
import app.questling.domain.cap
import app.questling.domain.count
import app.questling.domain.dayShort
import app.questling.domain.dur
import app.questling.domain.fmtDay
import app.questling.domain.hm
import app.questling.domain.minOfDay
import app.questling.domain.tr
import app.questling.domain.weekStart
import app.questling.domain.weekday
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.art.Pose
import app.questling.ui.art.Prop
import app.questling.ui.art.Sky
import app.questling.ui.art.TavernScene
import app.questling.ui.components.Cover
import app.questling.ui.components.GoldPill
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Meter
import app.questling.ui.components.Row2
import app.questling.ui.components.Sheet
import app.questling.ui.components.card
import app.questling.ui.components.gameColor
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.delay

/**
 * The tavern on one screen: the room, the session button, what is being played, the one thing waiting,
 * the week's quests, the season, the doors and the week in bars.
 */
@Composable
fun HomeScreen(d: Data, nav: Nav) {
    val s = d.s
    var quests by remember { mutableStateOf(false) }
    var dayOpen by remember { mutableStateOf<Long?>(null) }
    val todayMin = d.plays.filter { it.day == d.today }.sumOf { it.minutes }
    val lastDay = d.plays.maxOfOrNull { it.day } ?: d.today
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Трактир", "The tavern"), style = T.title)
                val streak = d.facts.streakNow
                Text(listOfNotNull(s.title.takeIf { it.isNotBlank() }, if (todayMin > 0) "${tr("сегодня", "today")} ${dur(todayMin * 60)}" else null,
                    if (streak >= 2) count(streak, "день подряд", "дня подряд", "дней подряд", "day in a row", "days in a row") else null)
                    .joinToString(" · ").ifBlank { tr("«Последнее сохранение»", "«The Last Save»") }, style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GoldPill(s.gold) { nav.push(Screen.Shop) }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { nav.push(Screen.Settings) }, contentAlignment = Alignment.Center) { Icon(Icons.GEAR, C.ink, 22.dp) }
        }
        Box(Modifier.fillMaxWidth().card(22.dp)) {
            val pose = when {
                s.live.on -> Pose.WORK
                d.today - lastDay >= 5 -> Pose.SLEEP
                todayMin > 0 -> Pose.CHEER
                else -> Pose.IDLE
            }
            TavernScene(d.team, s.items - s.hidden, pose, sky = Sky.of(s)) { nav.push(Screen.PetInfo(it.id)) }
        }

        // the session: the main thing to do here
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(s.live.on) { while (s.live.on) { now = System.currentTimeMillis(); delay(1000) } }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (s.live.on) C.ok else C.acc)
            .clickable { if (s.live.on) nav.push(Screen.Live) else nav.start = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Prop(if (s.live.on) "hourglass" else "pad", 36.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (s.live.on) (d.liveGame?.title ?: tr("Идёт сессия", "A session is on")) else tr("Начать сессию", "Start a session"), style = T.btn.copy(color = C.accInk), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (s.live.on) Text((if (s.live.pausedSince > 0) tr("перерыв · ", "break · ") else "") + clock(s.live.played(now)), style = T.small.copy(color = C.accInk.copy(alpha = .85f)))
            }
            Icon(Icons.NEXT, C.accInk, 20.dp)
        }

        // phone sessions found by the tracker
        if (s.found.isNotEmpty()) Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.Found) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(C.okSoft), contentAlignment = Alignment.Center) { Icon(Icons.PHONE, C.ok, 22.dp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Хомяк нашёл игры в телефоне", "The hamster found games on the phone"), style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(count(s.found.size, "сессия ждёт", "сессии ждут", "сессий ждут", "session to confirm", "sessions to confirm") + tr(" подтверждения", ""), style = T.small)
            }
            Icon(Icons.NEXT, C.mute, 20.dp)
        }

        // what is being played
        if (d.playing.isNotEmpty()) {
            Text(tr("Сейчас играю", "Playing now").uppercase(), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                d.playing.take(12).forEach { g ->
                    Column(Modifier.width(76.dp).clip(RoundedCornerShape(12.dp)).clickable { nav.push(Screen.GameInfo(g.id)) }) {
                        Cover(g, Modifier.size(76.dp, 100.dp), 12.dp)
                        Text(g.title, style = T.tiny.copy(color = C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        Text(dur(d.minutes(g.id) * 60), style = T.tiny, maxLines = 1)
                    }
                }
            }
        }

        val qs = s.quests.mapNotNull { Quests.byId[it] }
        if (qs.isNotEmpty()) {
            val done = qs.count { it.id in s.questsDone }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().card(18.dp).clickable { quests = true }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Prop("scroll", 32.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Задания недели", "This week's quests"), style = T.bodyB, maxLines = 1)
                    Text(if (done == qs.size) tr("все выполнены", "all done") else tr("выполнено $done из ${qs.size}", "$done of ${qs.size} done"), style = T.small, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                    qs.forEach { q -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (q.id in s.questsDone) C.ok else C.track)) }
                }
                Icon(Icons.NEXT, C.mute, 20.dp)
            }
        }

        // the season
        val lvl = Season.level(s.seasonXp); val (have, need) = Season.within(s.seasonXp)
        Row(Modifier.padding(top = 10.dp).fillMaxWidth().card(18.dp).clickable { nav.tab = 2 }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(C.goldSoft), contentAlignment = Alignment.Center) { Text("$lvl", style = T.numS.copy(color = C.gold)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(Season.name(s.season.ifBlank { Season.id(d.today) }), style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Meter(have.toFloat() / need, Modifier.padding(top = 5.dp), C.gold, 6.dp)
            }
            Spacer(Modifier.width(8.dp)); Icon(Icons.NEXT, C.mute, 20.dp)
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Door("chest", tr("Игры", "Games"), Modifier.weight(1f)) { nav.journal = 1; nav.tab = 1 }
            Door("meeple", tr("Игроки", "Players"), Modifier.weight(1f)) { nav.push(Screen.Friends) }
            Door("d20", tr("Кубики", "Dice"), Modifier.weight(1f)) { nav.push(Screen.Tools()) }
            Door("camera", tr("Альбом", "Album"), Modifier.weight(1f), d.facts.shots.takeIf { it > 0 }?.toString()) { nav.push(Screen.Album()) }
            Door("coins", tr("Лавка", "Shop"), Modifier.weight(1f)) { nav.push(Screen.Shop) }
        }

        Text(tr("Эта неделя", "This week").uppercase() + (d.plays.filter { it.day >= weekStart(d.today) }.sumOf { it.minutes }.takeIf { it > 0 }?.let { " · " + dur(it * 60) } ?: ""), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Week(d) { dayOpen = it }
        Spacer(Modifier.height(24.dp))
    }

    // the sheets fill the screen, so they live outside the scrolling column
    dayOpen?.let { day -> DaySheet(day, d, nav) { dayOpen = null } }
    if (quests) Sheet({ quests = false }) {
        Text(tr("Задания недели", "This week's quests"), style = T.h2)
        Text(tr("Новые – каждый понедельник. Каждое задание награждается сразу, как только выполнено: золото и опыт сезона.",
            "New ones every Monday. Each quest pays as soon as it's done: gold and season xp."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        val monday = weekStart(d.today)
        val facts = WeekFacts(monday, d.plays, d.games)
        // done on the page but not paid: settle it at once (#3)
        val unpaid = s.questWeek == monday && s.quests.mapNotNull { Quests.byId[it] }.any { it.id !in s.questsDone && it.progress(facts) >= it.need }
        val qc = androidx.compose.ui.platform.LocalContext.current.let { (it.applicationContext as app.questling.QuestApp).container }
        androidx.compose.runtime.LaunchedEffect(unpaid) { if (unpaid) qc.settle() }
        s.quests.mapNotNull { Quests.byId[it] }.forEach { q ->
            val done = q.id in s.questsDone
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(if (done) C.ok else C.card2), contentAlignment = Alignment.Center) { if (done) Icon(Icons.CHECK, C.accInk, 16.dp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(q.title, style = if (done) T.body.copy(color = C.mute) else T.body)
                    if (!done && s.questWeek == monday) Text(q.shown(facts), style = T.tiny)
                }
                // what was paid shows as paid
                if (done) Text(tr("получено +${q.gold}", "got +${q.gold}"), style = T.small.copy(color = C.ok)) else Text("+${q.gold}", style = T.smallB.copy(color = C.gold))
            }
        }
    }
}

/** 1:23:45 or 23:45. */
fun clock(ms: Long): String { val t = ms / 1000; val h = t / 3600; val m = t % 3600 / 60; val sec = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec) }

/** A door: a picture over a word and a number. */
@Composable
private fun Door(prop: String, label: String, modifier: Modifier = Modifier, value: String? = null, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(C.card).clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Prop(prop, 34.dp)
        Text(label, style = T.tiny.copy(color = C.ink), maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        if (value != null) Text(value, style = T.tiny.copy(color = C.acc), maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Monday to Sunday: minutes played, coloured by the day's main game. */
@Composable
private fun Week(d: Data, open: (Long) -> Unit) {
    val monday = weekStart(d.today)
    val byDay = d.plays.filter { it.day in monday..(monday + 6) }.groupBy { it.day }
    val max = (byDay.values.maxOfOrNull { l -> l.sumOf { it.minutes } } ?: 60).coerceAtLeast(60)
    Row(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 10.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 7) {
            val day = monday + i; val l = byDay[day].orEmpty(); val v = l.sumOf { it.minutes }
            val main = l.groupBy { it.gameId }.maxByOrNull { e -> e.value.sumOf { it.minutes } }?.key
            Column(Modifier.weight(1f).alpha(if (day > d.today) .35f else 1f).clip(RoundedCornerShape(10.dp)).clickable(enabled = day <= d.today) { open(day) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(52.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.width(14.dp).height((4 + 48f * v / max).dp).clip(RoundedCornerShape(50)).background(if (v == 0) C.track else gameColor(d.gameById[main])))
                }
                Text(dayShort(i), style = T.tiny.copy(color = if (day == d.today) C.acc else C.mute), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** One day of the week: what was played; each session opens. */
@Composable
private fun DaySheet(day: Long, d: Data, nav: Nav, close: () -> Unit) = Sheet(close) {
    val l = d.plays.filter { it.day == day }.sortedBy { it.start }
    Text("${weekday(day).cap()}, ${fmtDay(day)}", style = T.h2)
    Text(if (l.isEmpty()) tr("В этот день не играли.", "No play on this day.")
        else "${count(l.size, "сессия", "сессии", "сессий", "session", "sessions")} · ${dur(l.sumOf { it.minutes } * 60)}",
        style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
    l.forEach { p ->
        val g = d.gameById[p.gameId]
        Row2(g?.title ?: "–", listOfNotNull(hm(minOfDay(p.start)), p.platform.takeIf { it.isNotBlank() }, p.note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }).joinToString(" · "),
            dur(p.minutes * 60), lead = { Cover(g, Modifier.size(34.dp, 44.dp), 6.dp) }) { close(); nav.push(Screen.PlayInfo(p.id)) }
    }
}
