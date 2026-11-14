package app.questling.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.domain.Pets
import app.questling.domain.Quests
import app.questling.domain.Reward
import app.questling.domain.Season
import app.questling.domain.Stage
import app.questling.domain.Trophies
import app.questling.domain.count
import app.questling.domain.dur
import app.questling.domain.title
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.art.Creature
import app.questling.ui.art.EggView
import app.questling.ui.art.Pose
import app.questling.ui.art.Prop
import app.questling.ui.components.Btn
import app.questling.ui.components.Centered
import app.questling.ui.components.Cover
import app.questling.ui.components.Cup
import app.questling.ui.components.Popup
import app.questling.ui.components.tierColor
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.ui.theme.T

/** News waiting in Settings.inbox, one at a time: eggs, hatchings, growth, quests, trophies, season levels, finished games. */
@Composable
fun Inbox(d: Data, nav: Nav) {
    val item = d.s.inbox.firstOrNull() ?: return
    val c = LocalContext.current.container
    fun done() = c.write { settings.update { it.copy(inbox = it.inbox.drop(1)) } }
    val parts = item.split(':')
    var title = ""; var text = ""; var pic: @Composable () -> Unit = {}; var action: Pair<String, () -> Unit>? = null
    var body: (@Composable () -> Unit)? = null
    when (parts[0]) {
        "egg" -> { val sp = Pets.of(parts[1]); val why = parts.getOrNull(2)
            title = tr("Новое яйцо!", "A new egg!")
            text = when (why) {
                "shop" -> tr("Яйцо от гоблина-торговца. Игры его согреют, и скоро оно проклюнется.", "An egg from the goblin merchant. Play will warm it, and soon it cracks.")
                "season" -> tr("Награда сезона. Кто внутри? Сыграйте пару сессий – и узнаете.", "A season reward. Who is inside? Play a couple of sessions and see.")
                else -> tr("Его принесли за то, как вы играете. Кто внутри? Сыграйте пару сессий – и узнаете.", "It was brought for the way you play. Who is inside? Play a couple of sessions and see.") }
            pic = { EggView(sp.id, 150.dp, .6f) } }
        "hatch" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            val sp = Pets.of(p.species)
            title = tr("В трактире новый постоялец!", "A new lodger in the tavern!")
            text = tr("Знакомьтесь: ${p.name}, ${sp.ru.lowercase()}. ${sp.perkRu}.", "Meet ${p.name}, the ${sp.en.lowercase()}. ${sp.perkEn}.")
            pic = { Creature(p.species, Stage.BABY, Pose.CHEER, 160.dp, shiny = p.shiny) }
            if (p.slot !in 0..2) action = tr("Поселить у очага", "Bring to the fire") to { c.write { val free = (0..2).firstOrNull { sl -> d.pets.none { it.slot == sl } } ?: 1; setSlot(p, free) } } }
        "stage" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            val st = Stage.entries[parts[2].toInt()]
            title = tr("${p.name} подрос!", "${p.name} grew up!")
            text = tr("За ночь приключения улеглись, и ${p.name} теперь «${st.title().lowercase()}».", "Overnight the adventures settled, and ${p.name} is now a «${st.title().lowercase()}».")
            pic = { Creature(p.species, st, Pose.CHEER, 160.dp, shiny = p.shiny) } }
        "quest" -> { val q = Quests.byId[parts[1]] ?: run { done(); return }
            title = tr("Задание выполнено", "Quest done"); text = q.title + "\n+${q.gold} " + tr("золота", "gold")
            pic = { Prop("scroll", 110.dp) } }
        "trophy" -> { val ts = parts[1].split(',').mapNotNull { Trophies.byId[it] }.sortedByDescending { it.tier.ordinal }
            if (ts.isEmpty()) { done(); return }
            val one = ts.size == 1
            title = if (one) tr("Трофей!", "A trophy!") else count(ts.size, "новый трофей", "новых трофея", "новых трофеев", "new trophy", "new trophies").replaceFirstChar { it.uppercase() } + "!"
            text = if (one) "${ts[0].title}\n${ts[0].desc}" else ""
            pic = { Prop(ts[0].tier.prop, 120.dp) }
            if (!one) body = { Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ts.take(6).forEach { t -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Cup(t, true, 38.dp); Spacer(Modifier.width(10.dp))
                    Column { Text(t.title, style = T.bodyB.copy(color = tierColor(t.tier))); Text(t.desc, style = T.small, maxLines = 2) } } }
                if (ts.size > 6) Text(tr("и ещё ${ts.size - 6} – во вкладке «Трофеи»", "and ${ts.size - 6} more – in the Trophies tab"), style = T.small)
            } }
            action = tr("Посмотреть трофеи", "See the trophies") to { nav.tab = 2 } }
        "level" -> { val l = parts[1].toIntOrNull() ?: run { done(); return }
            title = tr("Уровень сезона: $l", "Season level $l")
            text = when (val r = Season.reward(l)) {
                is Reward.Gold -> tr("Награда: ${r.n} золота.", "Reward: ${r.n} gold.")
                Reward.Egg -> tr("Награда: яйцо. Его уже несут в трактир.", "Reward: an egg. It is on its way to the tavern.")
                is Reward.Title -> tr("Награда: титул «${r.ru}» и ${r.gold} золота. Надеть титул можно в настройках.", "Reward: the title «${r.en}» and ${r.gold} gold. Wear it from the settings.")
            }
            pic = { Prop("gold", 110.dp) } }
        "finished", "hundred" -> { val g = d.gameById[parts[1].toLongOrNull()] ?: run { done(); return }
            title = if (parts[0] == "hundred") tr("Пройдено на 100%!", "100% done!") else tr("Титры!", "Credits!")
            text = tr("«${g.title}» – позади. Наиграно ${dur(d.minutes(g.id) * 60)}.", "«${g.title}» is behind you. ${dur(d.minutes(g.id) * 60)} played.")
            pic = { Cover(g, Modifier.size(120.dp, 160.dp), 14.dp) }
            action = tr("Поделиться", "Share") to { nav.share = ShareReq(CardKind.GAME, g.id) } }
        else -> { done(); return }
    }
    Popup(::done) {
        Centered { pic() }
        Spacer(Modifier.height(10.dp))
        Text(title, style = T.h2, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (text.isNotBlank()) Text(text, style = T.body.copy(color = C.mute), modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
        body?.invoke()
        Spacer(Modifier.height(16.dp))
        action?.let { (l, f) -> Btn(l, Modifier.fillMaxWidth()) { f(); done() }; Spacer(Modifier.height(8.dp)) }
        Btn(tr("Отлично", "Great"), Modifier.fillMaxWidth(), color = if (action != null) C.card2 else C.acc, ink = if (action != null) C.ink else C.accInk) { done() }
    }
}
