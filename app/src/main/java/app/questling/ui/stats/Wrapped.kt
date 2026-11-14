package app.questling.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.questling.data.Game
import app.questling.domain.Span
import app.questling.domain.Stats
import app.questling.domain.cap
import app.questling.domain.count
import app.questling.domain.dur
import app.questling.domain.monthName
import app.questling.domain.stage
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.art.Creature
import app.questling.ui.art.Pose
import app.questling.ui.art.Prop
import app.questling.ui.components.Btn
import app.questling.ui.components.Cover
import app.questling.ui.components.Empty
import app.questling.ui.components.Page
import app.questling.ui.components.tone
import app.questling.ui.journal.Radar
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import java.time.LocalDate

/** «The year in games»: the year's story told card by card. */
@Composable
fun WrappedPage(year: Int, d: Data, nav: Nav) {
    val from = LocalDate.of(year, 1, 1).toEpochDay(); val to = LocalDate.of(year, 12, 31).toEpochDay()
    val st = Stats(d.plays, Span(from, to, "$year"), d.gameById, d.today)
    Page(tr("Год в играх · $year", "The year in games · $year"), { nav.pop() }) {
        if (st.count == 0) { Empty(tr("В этом году сессий нет", "No sessions this year")); return@Page }
        @Composable fun Leaf(bg: Color, content: @Composable () -> Unit) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(24.dp)).background(bg).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
        Leaf(C.accSoft) {
            Prop("hourglass", 90.dp)
            Text(dur(st.minutes * 60), style = T.big, modifier = Modifier.padding(top = 8.dp))
            Text(tr("за играми в этом году – ${count(st.days, "день", "дня", "дней", "", "")}, ${count(st.count, "сессия", "сессии", "сессий", "", "")}",
                "of play this year – ${count(st.days, "", "", "", "day", "days")}, ${count(st.count, "", "", "", "session", "sessions")}"), style = T.body, textAlign = TextAlign.Center)
        }
        val games = st.byGame()
        games.firstOrNull()?.let { top -> Leaf(C.goldSoft) {
            Cover(top.key, Modifier.size(120.dp, 160.dp), 14.dp)
            Text(tr("Игра года", "The game of the year").uppercase(), style = T.sec, modifier = Modifier.padding(top = 10.dp))
            Text(top.key?.title ?: "–", style = T.title, textAlign = TextAlign.Center)
            Text(tr("${dur(top.minutes * 60)} – ${(top.share * 100).toInt()}% всего времени", "${dur(top.minutes * 60)} – ${(top.share * 100).toInt()}% of all the time"), style = T.body)
        } }
        if (games.size >= 3) Leaf(C.card) {
            Text(tr("Ещё любимые", "More favourites").uppercase(), style = T.sec)
            Row(Modifier.padding(top = 10.dp)) { games.drop(1).take(4).forEach { g -> Column(Modifier.padding(horizontal = 5.dp).width(68.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Cover(g.key, Modifier.size(68.dp, 90.dp), 10.dp); Text(dur(g.minutes * 60), style = T.tiny, modifier = Modifier.padding(top = 4.dp)) } } }
            Text(count(st.gamesPlayed, "разная игра", "разные игры", "разных игр", "different game", "different games") + if (st.fresh > 0) tr(", из них новых – ${st.fresh}", ", ${st.fresh} of them new") else "", style = T.body, modifier = Modifier.padding(top = 8.dp))
        }
        st.byGenre().firstOrNull()?.let { g -> Leaf(C.card) {
            Text(tr("Жанр года", "The genre of the year").uppercase(), style = T.sec)
            Text(g.key.title, style = T.title.copy(color = tone(g.key.color)))
            Text(dur(g.minutes * 60), style = T.body)
            Radar(st.byStat(), Modifier.padding(top = 4.dp))
        } }
        val ms = (1..12).map { m -> val a = LocalDate.of(year, m, 1).toEpochDay(); val z = LocalDate.of(year, m, 1).plusMonths(1).toEpochDay() - 1
            m to st.plays.filter { it.day in a..z }.sumOf { it.minutes } }.filter { it.second > 0 }
        if (ms.size >= 2) Leaf(C.card) { val hi = ms.maxBy { it.second }
            Text(tr("Самый игровой месяц", "The most playful month").uppercase(), style = T.sec)
            Text(monthName(hi.first).cap(), style = T.title); Text(dur(hi.second * 60), style = T.body) }
        val done = d.games.filter { it.finished in from..to }
        if (done.isNotEmpty()) Leaf(C.goldSoft) {
            Prop("gold", 80.dp)
            Text(count(done.size, "игра пройдена", "игры пройдены", "игр пройдено", "game finished", "games finished"), style = T.title, textAlign = TextAlign.Center)
            Text(done.joinToString(", ") { it.title }, style = T.body, textAlign = TextAlign.Center)
            done.count { it.status == Game.HUNDRED }.takeIf { it > 0 }?.let { Text(tr("из них на 100% – $it", "$it of them 100%"), style = T.small) }
        }
        if (st.social > 0) Leaf(C.okSoft) {
            Creature("bear", app.questling.domain.Stage.ADULT, Pose.CHEER, 120.dp)
            Text(count(st.social, "сессия с друзьями", "сессии с друзьями", "сессий с друзьями", "session with friends", "sessions with friends"), style = T.title, textAlign = TextAlign.Center)
            if (st.wins + st.losses > 0) Text(tr("Побед – ${st.wins}, поражений – ${st.losses}", "${st.wins} wins, ${st.losses} losses"), style = T.body)
        }
        val trophies = d.s.trophies.count { it.value in from..to }
        if (trophies > 0) Leaf(C.card) {
            Prop("platinum", 80.dp)
            Text(count(trophies, "трофей", "трофея", "трофеев", "trophy", "trophies"), style = T.title)
            Text(tr("за год", "this year"), style = T.body)
        }
        if (st.shots > 0) Leaf(C.card) {
            Prop("camera", 80.dp)
            Text(count(st.shots, "снимок", "снимка", "снимков", "picture", "pictures"), style = T.title)
            Text(tr("в альбоме за год", "in the album this year"), style = T.body)
        }
        val born = d.pets.filter { it.hatched in from..to }
        if (born.isNotEmpty()) Leaf(C.card) {
            Text(tr("Вылупились в этом году", "Hatched this year").uppercase(), style = T.sec)
            Row(Modifier.padding(top = 8.dp)) { born.take(5).forEach { p -> Creature(p.species, p.stage(), Pose.IDLE, 58.dp, shiny = p.shiny); Spacer(Modifier.width(2.dp)) } }
            Text(count(born.size, "питомец", "питомца", "питомцев", "creature", "creatures"), style = T.title)
        }
        Btn(tr("Поделиться годом", "Share the year"), Modifier.fillMaxWidth().padding(top = 16.dp)) { nav.share = ShareReq(CardKind.YEAR, year.toLong()) }
    }
}
