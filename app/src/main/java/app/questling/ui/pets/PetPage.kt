package app.questling.ui.pets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.domain.Pets
import app.questling.domain.Stage
import app.questling.domain.count
import app.questling.domain.fmtDayYear
import app.questling.domain.monthShort
import app.questling.domain.stage
import app.questling.domain.title
import app.questling.domain.toNext
import app.questling.domain.tr
import app.questling.domain.Stat
import app.questling.domain.speciality
import app.questling.domain.dur
import androidx.compose.foundation.layout.width
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.art.Creature
import app.questling.ui.art.PetView
import app.questling.ui.art.Pose
import app.questling.ui.components.Bars
import app.questling.ui.components.Btn
import app.questling.ui.components.Field
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Meter
import app.questling.ui.components.Popup
import app.questling.ui.components.Segments
import app.questling.ui.components.card
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.domain.Genres
import app.questling.domain.klass
import app.questling.domain.level
import app.questling.ui.components.genreColor
import app.questling.ui.components.tone
import app.questling.ui.theme.T
import java.time.LocalDate

/**
 * A pet's own page: a big stage in its colour where it shows its poses, its name and growth,
 * its place in the tavern, what it does, its six stats and what it was raised on, the stages it grows through.
 */
@Composable
fun PetPage(id: Long, d: Data, nav: Nav) {
    BackHandler { nav.pop() }
    val c = LocalContext.current.container
    // opened before the pets loaded (a deep link): wait, don't close
    val p = (if (id == -1L) d.pets.firstOrNull { it.slot == 1 } else d.pets.firstOrNull { it.id == id }) ?: run { if (d.pets.isNotEmpty()) nav.pop(); return }
    val sp = Pets.of(p.species)
    val col = Color(sp.color)
    val st = p.stage()
    var pose by remember { mutableStateOf(Pose.IDLE) }
    var rename by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(C.bg).clickable(enabled = false) {}) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // the stage
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(col.copy(alpha = .38f), col.copy(alpha = .1f), C.bg))).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Round(Icons.BACK) { nav.pop() }
                    Spacer(Modifier.weight(1f))
                    if (p.hatched > 0) Round(Icons.SHARE) { nav.share = ShareReq(CardKind.PET, p.id) }
                }
                Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.padding(bottom = 10.dp).size(180.dp, 26.dp).clip(RoundedCornerShape(50)).background(col.copy(alpha = .18f)))
                        if (p.hatched == 0L) PetView(p, 230.dp) else Creature(p.species, st, pose, 230.dp, shiny = p.shiny)
                    }
                    if (p.hatched > 0 && st >= Stage.ADULT) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Pose.IDLE to tr("Стоит", "Idle"), Pose.WORK to tr("В игре", "At play"), Pose.CHEER to tr("Радуется", "Cheers"), Pose.SLEEP to tr("Спит", "Sleeps")).forEach { (ps, l) ->
                            Text(l, style = T.smallB.copy(color = if (ps == pose) C.accInk else C.ink), modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(if (ps == pose) col else C.card.copy(alpha = .7f)).clickable { pose = ps }.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 18.dp)) {
                // name and growth
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (p.hatched == 0L) tr("Яйцо", "An egg") else p.name, style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p.hatched > 0) Box(Modifier.padding(start = 6.dp).size(32.dp).clip(CircleShape).clickable { rename = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.mute, 17.dp) }
                        }
                        Text(sp.title + (if (p.hatched > 0) " · ${st.title()} · ${tr("ур.", "lvl")} ${p.level()}" else "") + if (p.shiny) tr(" · редкий окрас", " · rare colours") else "", style = T.small)
                    }
                }
                val (have, need) = p.toNext()
                if (st != Stage.MASTER) {
                    Meter(have.toFloat() / need, Modifier.padding(top = 10.dp), color = col, h = 10.dp)
                    val next = Stage.entries[(st.ordinal + 1).coerceAtMost(Stage.MASTER.ordinal)]
                    Text(if (p.hatched == 0L) tr("Ещё ${need - have} – и вылупится. Яйцо греют ваши сессии.", "${need - have} more and it hatches. Your sessions keep the egg warm.")
                        else tr("До ступени «${next.title().lowercase()}» – ${need - have} опыта", "${need - have} XP to «${next.title().lowercase()}»") + if (p.belly > 0) tr(" · ночью прибавится ${p.belly}", " · +${p.belly} tonight") else "", style = T.tiny, modifier = Modifier.padding(top = 4.dp))
                } else Text(tr("Высшая ступень", "The highest stage"), style = T.tiny.copy(color = C.gold), modifier = Modifier.padding(top = 6.dp))

                // the place in the team
                if (p.hatched > 0) {
                    Label(tr("Место", "Place"))
                    val slot = when (p.slot) { 0 -> 0; 1, 2 -> 1; else -> 2 }
                    Segments(listOf(tr("Вожак", "Leader"), tr("У очага", "By the fire"), tr("Отдыхает", "Resting")), slot) { i ->
                        if (i == slot) return@Segments
                        c.write { when (i) { 0 -> setSlot(p, 0); 1 -> setSlot(p, (1..2).firstOrNull { sl -> d.pets.none { it.slot == sl } } ?: 2); else -> setSlot(p, -1) } }
                    }
                }

                // what it does
                Label(tr("Что умеет", "What it does"))
                Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
                    Text(sp.perkText, style = T.body)
                    Text(tr("В трактире: ", "In the tavern: ") + perkShort(sp), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp)
                        .clip(RoundedCornerShape(50)).background(C.accSoft).padding(horizontal = 10.dp, vertical = 5.dp))
                    if (p.hatched == 0L && sp.find.isNotEmpty()) Text(sp.find, style = T.small, modifier = Modifier.padding(top = 8.dp))
                }

                // the six stats and what it was raised on
                if (p.hatched > 0) {
                    Label(tr("Характеристики", "Stats"))
                    Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
                        val max = (p.stats.maxOrNull() ?: 1).coerceAtLeast(10)
                        Stat.entries.forEachIndexed { i, stt -> val v = p.stats[i]
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stt.title, style = T.smallB, modifier = Modifier.width(130.dp))
                                Meter(v.toFloat() / max, Modifier.weight(1f), color = tone(stt.color), h = 8.dp)
                                Text("$v", style = T.smallB, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                    val raised = p.genreMap().entries.sortedByDescending { it.value }.take(4)
                    if (raised.isNotEmpty()) {
                        Label(tr("На чём вырос", "What it was raised on"))
                        Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
                            p.klass()?.let { k -> Text(tr("Класс: ", "Class: ") + k.klass + (p.speciality()?.let { g -> tr(" · любимый жанр: ", " · favourite genre: ") + g.title } ?: ""), style = T.bodyB.copy(color = tone(k.color)), modifier = Modifier.padding(bottom = 6.dp)) }
                            val tot = raised.sumOf { it.value }.coerceAtLeast(1)
                            raised.forEach { (k, v) -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                app.questling.ui.components.Dot(genreColor(k), 12.dp); Spacer(Modifier.width(8.dp))
                                Text(Genres.of(k)?.title ?: k, style = T.small.copy(color = C.ink), modifier = Modifier.weight(1f))
                                Text("${v * 100 / tot}%", style = T.smallB) } }
                        }
                    }
                }

                // the stages
                if (p.hatched > 0) {
                    Label(tr("Ступени", "Stages"))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Stage.BABY, Stage.TEEN, Stage.ADULT, Stage.MASTER).forEach { s ->
                            val reached = st >= s
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (s == st) col.copy(alpha = .2f) else C.card)
                                .then(if (s == st) Modifier.border(1.5.dp, col, RoundedCornerShape(16.dp)) else Modifier).padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.alpha(if (reached) 1f else .25f)) { Creature(p.species, s, Pose.IDLE, 62.dp, shiny = p.shiny) }
                                Text(s.title(), style = T.tiny.copy(color = if (reached) C.ink else C.mute), maxLines = 1, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                // facts
                Label(tr("История", "Story"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fact("${p.xp}", tr("опыта", "XP"), Modifier.weight(1f))
                    Fact("${(d.today - p.found).coerceAtLeast(0)}", tr("дней в трактире", "days here"), Modifier.weight(1f))
                }
                Text(tr("Появился ", "Found ") + fmtDayYear(p.found) + (if (p.hatched > 0) tr(", вылупился ", ", hatched ") + fmtDayYear(p.hatched) else ""), style = T.tiny, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(30.dp))
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
    if (rename) { var name by remember { mutableStateOf(p.name) }
        Popup({ rename = false }) {
            Text(tr("Как зовут?", "What's its name?"), style = T.h2)
            Field(name, { name = it.take(20) }, sp.title, Modifier.padding(top = 10.dp))
            Btn(tr("Готово", "Done"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = name.isNotBlank()) { c.write { rename(p, name) }; rename = false }
        } }
}

@Composable
private fun Round(icon: Icons, onClick: () -> Unit) =
    Box(Modifier.size(42.dp).clip(CircleShape).background(C.card.copy(alpha = .75f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, C.ink, 21.dp) }

@Composable
private fun Label(text: String) = Text(text.uppercase(), style = T.sec, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))

@Composable
private fun Fact(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.card(16.dp).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.num)
        Text(label, style = T.tiny)
    }
}
