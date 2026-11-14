package app.questling.ui.trophies

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.domain.Family
import app.questling.domain.Reward
import app.questling.domain.Season
import app.questling.domain.TGroup
import app.questling.domain.Tier
import app.questling.domain.Trophies
import app.questling.domain.Trophy
import app.questling.domain.count
import app.questling.domain.fmtDayYear
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.art.Prop
import app.questling.ui.components.Chip
import app.questling.ui.components.Cup
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Meter
import app.questling.ui.components.Segments
import app.questling.ui.components.Sheet
import app.questling.ui.components.card
import app.questling.ui.components.tierColor
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.ui.theme.T

/** The trophy hall and the season pass. */
@Composable
fun TrophiesScreen(d: Data, nav: Nav, season0: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(if (season0) 1 else 0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Трофеи", "Trophies"), style = T.title, modifier = Modifier.weight(1f))
            Box(Modifier.size(42.dp).clip(CircleShape).clickable { nav.share = ShareReq(CardKind.TROPHIES) }, contentAlignment = Alignment.Center) { Icon(Icons.SHARE, C.ink, 20.dp) }
        }
        Segments(listOf(tr("Достижения", "Achievements"), tr("Сезон", "Season")), tab, Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) { tab = it }
        if (tab == 0) Hall(d) else SeasonPart(d)
    }
}

/** A ring that fills with [v] 0..1, with something in the middle. */
@Composable
private fun Ring(v: Float, size: Dp, color: Color, stroke: Dp = 8.dp, content: @Composable () -> Unit) {
    val track = C.track
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx(); val r = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(w / 2, w / 2), r, style = Stroke(w))
            drawArc(color, -90f, 360f * v.coerceIn(0f, 1f), false, Offset(w / 2, w / 2), r, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Small dots for the steps of a ladder: each in its metal once earned. */
@Composable
private fun Pips(f: Family, got: Map<String, Long>) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        f.steps.forEach { t -> Box(Modifier.size(width = if (f.steps.size > 6) 12.dp else 16.dp, height = 6.dp).clip(RoundedCornerShape(50)).background(if (t.id in got) tierColor(t.tier) else C.track)) }
    }
}

@Composable
private fun Hall(d: Data) {
    var group by rememberSaveable { mutableIntStateOf(-1) }
    var open by remember { mutableStateOf<Family?>(null) }
    val got = d.s.trophies
    val f = d.facts
    // the nearest ones: not earned, not secret, the furthest along
    val near = remember(f) { Trophies.families.filter { !it.hidden }.mapNotNull { fam -> fam.next(got)?.let { fam to Trophies.shown(it, f) } }.filter { it.second.first > 0 }
        .sortedByDescending { it.second.first.toFloat() / it.second.second }.take(6) }
    val groups = TGroup.entries.filter { group < 0 || it.ordinal == group }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        // the hall at a glance
        item {
            Row(Modifier.fillMaxWidth().card(22.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Ring(got.size.toFloat() / Trophies.all.size, 92.dp, C.gold) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${got.size}", style = T.num); Text(tr("из ${Trophies.all.size}", "of ${Trophies.all.size}"), style = T.tiny) }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tier.entries.reversed().forEach { t -> val all = Trophies.all.count { it.tier == t }; val n = Trophies.all.count { it.tier == t && it.id in got }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Prop(t.prop, 20.dp); Spacer(Modifier.width(6.dp))
                            Text(t.title, style = T.small.copy(color = C.ink), modifier = Modifier.width(74.dp), maxLines = 1)
                            Meter(n.toFloat() / all, Modifier.weight(1f), tierColor(t), 6.dp)
                            Text("$n", style = T.smallB, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                        } }
                }
            }
        }
        if (near.isNotEmpty()) {
            item { Text(tr("Совсем близко", "Almost there").uppercase(), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
            item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                near.forEach { (fam, pr) -> val t = fam.next(got)!!
                    Column(Modifier.width(168.dp).card(18.dp).clickable { open = fam }.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Cup(t, false, 38.dp); Spacer(Modifier.weight(1f)); Text("${pr.first}/${pr.second}", style = T.smallB.copy(color = tierColor(t.tier))) }
                        Text(t.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        Text(t.desc, style = T.tiny, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                        Meter(pr.first.toFloat() / pr.second, Modifier.padding(top = 8.dp), tierColor(t.tier), 6.dp)
                    } } } }
        }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(tr("Все", "All"), group < 0) { group = -1 }
            TGroup.entries.forEachIndexed { i, g -> Chip(g.title, group == i) { group = if (group == i) -1 else i } } } }

        groups.forEach { g ->
            val fams = Trophies.families.filter { it.group == g }
            val all = fams.sumOf { it.steps.size }; val have = fams.sumOf { it.earned(got) }
            item(key = "g" + g.name) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(g.title.uppercase(), style = T.sec)
                    Meter(have.toFloat() / all, Modifier.weight(1f).padding(horizontal = 10.dp), C.gold, 4.dp)
                    Text("$have/$all", style = T.sec)
                }
            }
            if (g == TGroup.SECRET) item(key = "secret") { SecretGrid(fams, got) { open = it } }
            else items(fams, key = { it.key }) { fam -> FamilyRow(fam, d) { open = fam } }
        }
        item { Spacer(Modifier.height(26.dp)) }
    }
    open?.let { fam -> FamilySheet(fam, d) { open = null } }
}

/** A ladder in one row: the cup to go for, where it stands, the steps as pips. */
@Composable
private fun FamilyRow(fam: Family, d: Data, onClick: () -> Unit) {
    val got = d.s.trophies
    val next = fam.next(got); val top = fam.top(got); val show = next ?: top!!
    val done = next == null
    Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().card(16.dp).then(if (done) Modifier.border(1.dp, tierColor(show.tier).copy(alpha = .5f), RoundedCornerShape(16.dp)) else Modifier)
        .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Cup(top ?: show, top != null, 44.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fam.title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (fam.steps.size > 1) { Spacer(Modifier.width(8.dp)); Pips(fam, got) }
            }
            Text(if (done) tr("Всё собрано", "All collected") else show.desc, style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!done) { val (h, n) = Trophies.shown(show, d.facts)
                if (n > 1) Meter(h.toFloat() / n, Modifier.padding(top = 5.dp), tierColor(show.tier), 5.dp) }
        }
        Spacer(Modifier.width(8.dp))
        if (done) Icon(Icons.CHECK, tierColor(show.tier), 18.dp)
        else { val (h, n) = Trophies.shown(show, d.facts); if (n > 1) Text("$h/$n", style = T.tiny) }
    }
}

/** The secret trophies as a wall of cups: the earned ones shine, the rest wait as question marks. */
@Composable
private fun SecretGrid(fams: List<Family>, got: Map<String, Long>, onOpen: (Family) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().card(18.dp).padding(12.dp)) {
        val cols = 6; val cell = maxWidth / cols
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            fams.sortedBy { if (it.earned(got) > 0) 0 else 1 }.chunked(cols).forEach { row -> Row {
                row.forEach { fam -> val t = fam.steps[0]; val on = t.id in got
                    Box(Modifier.size(cell).clip(CircleShape).clickable { onOpen(fam) }, contentAlignment = Alignment.Center) {
                        if (on) Cup(t, true, cell - 8.dp) else Box(Modifier.size(cell - 8.dp).clip(CircleShape).background(C.card2), contentAlignment = Alignment.Center) { Text("?", style = T.h3.copy(color = C.mute)) }
                    } } } }
        }
    }
}

/** Every step of a ladder: what it asks, how far it is, when it was earned. */
@Composable
private fun FamilySheet(fam: Family, d: Data, onClose: () -> Unit) = Sheet(onClose) {
    val got = d.s.trophies
    val secret = fam.hidden && fam.earned(got) == 0
    Text(if (secret) tr("Секретный трофей", "A secret trophy") else fam.title, style = T.h2)
    if (secret) Text(tr("Его условие откроется, когда вы его выполните. Играйте по-разному – секреты любят любопытных.", "Its condition shows once you meet it. Play in different ways – secrets like the curious."),
        style = T.body.copy(color = C.mute), modifier = Modifier.padding(top = 6.dp))
    else if (fam.steps.size > 1) Text(tr("${fam.earned(got)} из ${fam.steps.size} ступеней", "${fam.earned(got)} of ${fam.steps.size} steps"), style = T.small, modifier = Modifier.padding(top = 2.dp))
    Spacer(Modifier.height(8.dp))
    val next = fam.next(got)
    fam.steps.forEach { t -> val day = got[t.id]; val on = day != null
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Cup(t, on, 42.dp); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (secret) "???" else t.title, style = T.bodyB.copy(color = if (on) C.ink else C.mute))
                if (!secret) Text(t.desc, style = T.small)
                if (on) Text(tr("Получен ${fmtDayYear(day!!)}", "Earned ${fmtDayYear(day!!)}"), style = T.tiny.copy(color = tierColor(t.tier)))
                else if (t == next && !secret) { val (h, n) = Trophies.shown(t, d.facts)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) { Meter(h.toFloat() / n, Modifier.weight(1f), tierColor(t.tier), 5.dp); Text("  $h/$n", style = T.tiny) } }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) { Text("+${t.tier.gold}", style = T.smallB.copy(color = C.gold)); Text(t.tier.title, style = T.tiny.copy(color = tierColor(t.tier))) }
        }
    }
    Text(tr("Каждый трофей приносит золото и опыт сезона.", "Every trophy brings gold and season xp."), style = T.tiny, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun rewardText(r: Reward) = when (r) { is Reward.Gold -> tr("${r.n} золота", "${r.n} gold"); Reward.Egg -> tr("Яйцо питомца", "A creature egg")
    is Reward.Title -> tr("Титул «${r.ru}» и ${r.gold} золота", "The title «${r.en}» and ${r.gold} gold") }
private fun rewardProp(r: Reward) = when (r) { is Reward.Gold -> "coins"; Reward.Egg -> "egg"; is Reward.Title -> "gold" }

@Composable
private fun SeasonPart(d: Data) {
    val c = LocalContext.current.container
    val s = d.s
    val id = s.season.ifBlank { Season.id(d.today) }
    val lvl = Season.level(s.seasonXp); val (have, need) = Season.within(s.seasonXp)
    var all by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Row(Modifier.fillMaxWidth().card(22.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Ring(have.toFloat() / need, 92.dp, C.gold) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$lvl", style = T.num.copy(color = C.gold)); Text(tr("уровень", "level"), style = T.tiny) } }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(Season.name(id), style = T.h2)
                    Text(if (lvl >= Season.LEVELS) tr("Сезон пройден!", "Season complete!") else tr("до следующего уровня – ${need - have} опыта", "${need - have} xp to the next level"), style = T.small)
                    Text(tr("осталось ${count(Season.left(d.today).toInt(), "день", "дня", "дней", "", "")}", "${Season.left(d.today)} days left"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
                }
            }
            // what comes next: a short track of the nearest rewards
            if (lvl < Season.LEVELS) {
                Text(tr("Ближайшие награды", "Coming up").uppercase(), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ((lvl + 1)..minOf(lvl + 8, Season.LEVELS)).forEach { l -> val r = Season.reward(l); val nextUp = l == lvl + 1
                        Column(Modifier.width(96.dp).card(16.dp, if (nextUp) C.goldSoft else null).padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tr("ур. $l", "lvl $l"), style = T.tiny.copy(color = if (nextUp) C.gold else C.mute))
                            Prop(rewardProp(r), 40.dp, Modifier.padding(vertical = 4.dp))
                            Text(when (r) { is Reward.Gold -> "${r.n}"; Reward.Egg -> tr("яйцо", "an egg"); is Reward.Title -> tr("титул", "a title") }, style = T.smallB, maxLines = 1)
                        } }
                }
            }
            // titles to wear
            val titles = buildList { add(""); listOf(10, 20, 30).forEach { l -> if (lvl >= l || s.seasons.values.any { it >= l }) (Season.reward(l) as? Reward.Title)?.let { add(tr(it.ru, it.en)) } }
                if (s.trophies.size >= 50) add(tr("Охотник за трофеями", "Trophy hunter")); if (d.facts.hours >= 500) add(tr("Старый волк", "Old hand")) }
            if (titles.size > 1) {
                Text(tr("Титул", "Title").uppercase(), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    titles.forEach { t -> Chip(t.ifEmpty { tr("Без титула", "No title") }, s.title == t, color = C.gold) { c.write { settings.update { it.copy(title = t) } } } } }
            }
            Text(tr("Опыт сезона дают сессии, новые игры, игра с друзьями, задания недели и трофеи. Купить его нельзя.", "Season xp comes from sessions, new games, playing with friends, weekly quests and trophies. It can't be bought."),
                style = T.tiny, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp).clip(RoundedCornerShape(10.dp)).clickable { all = !all }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Все 30 уровней", "All 30 levels").uppercase(), style = T.sec, modifier = Modifier.weight(1f)); Icon(if (all) Icons.UP else Icons.DOWN, C.mute, 18.dp) }
        }
        if (all) items((1..Season.LEVELS).toList(), key = { it }) { l ->
            val done = l <= lvl; val r = Season.reward(l)
            Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().card(16.dp, if (l == lvl + 1) C.goldSoft else null).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(if (done) C.gold else C.card2), contentAlignment = Alignment.Center) {
                    if (done) Icon(Icons.CHECK, Color.White, 17.dp) else Text("$l", style = T.smallB.copy(color = C.mute)) }
                Spacer(Modifier.width(12.dp)); Prop(rewardProp(r), 30.dp, alpha = if (done) 1f else .7f); Spacer(Modifier.width(10.dp))
                Text(rewardText(r), style = if (done) T.body.copy(color = C.mute) else T.bodyB, modifier = Modifier.weight(1f))
            }
        }
        if (s.seasons.isNotEmpty()) {
            item { Text(tr("Прошлые сезоны", "Past seasons").uppercase(), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)) }
            items(s.seasons.entries.sortedByDescending { it.key }, key = { "s" + it.key }) { (k, v) ->
                Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().card(16.dp).padding(12.dp)) { Text(Season.name(k), style = T.body, modifier = Modifier.weight(1f)); Text(tr("уровень $v", "level $v"), style = T.smallB.copy(color = C.gold)) } }
        }
        item { Spacer(Modifier.height(26.dp)) }
    }
}
