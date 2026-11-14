package app.questling.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.data.Pet
import app.questling.domain.Pets
import app.questling.domain.Stage
import app.questling.domain.Species
import app.questling.domain.fmtDayYear
import app.questling.domain.stage
import app.questling.domain.title
import app.questling.domain.toNext
import app.questling.domain.tr
import app.questling.domain.Perk
import app.questling.domain.Genres
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.Screen
import app.questling.ui.art.Creature
import app.questling.ui.art.Mini
import app.questling.ui.art.PetView
import app.questling.ui.art.Pose
import app.questling.ui.components.Btn
import app.questling.ui.components.Card
import app.questling.ui.components.Field
import app.questling.ui.components.Ghost
import app.questling.ui.components.Hint
import app.questling.ui.components.Icons
import app.questling.ui.components.Meter
import app.questling.ui.components.Page
import app.questling.ui.components.Popup
import app.questling.ui.components.Row2
import app.questling.ui.components.Section
import app.questling.ui.components.card
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.C
import app.questling.ui.theme.T

@Composable
fun PetsScreen(d: Data, nav: Nav) {
    var ask by remember { mutableStateOf<Species?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Text(tr("Питомцы", "Creatures"), style = T.title, modifier = Modifier.weight(1f))
            Text("${d.pets.count { it.hatched > 0 }} / ${Pets.all.size}", style = T.h3.copy(color = C.mute))
        }
        Section(tr("Живут в трактире", "In the tavern"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..2).forEach { slot ->
                val p = d.pets.firstOrNull { it.slot == slot }
                Column(Modifier.weight(1f).card(18.dp).clickable(enabled = p != null) { p?.let { nav.push(Screen.PetInfo(it.id)) } }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (slot == 0) tr("Вожак", "Leader") else tr("У очага", "By the fire"), style = T.tiny)
                    if (p != null) { PetView(p, 84.dp, Pose.IDLE); Text(p.name, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(perkShort(Pets.of(p.species)), style = T.tiny, maxLines = 2, textAlign = TextAlign.Center, minLines = 2) }
                    else { Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) { Text("?", style = T.title.copy(color = C.line)) }; Text(tr("свободно", "free"), style = T.small) }
                }
            }
        }
        listOf(tr("Из игровых миров", "From game worlds") to Pets.all.filter { it.world }, tr("Обычные звери", "Ordinary animals") to Pets.all.filter { !it.world }).forEach { (name, list) ->
        Section(name + " · ${list.count { sp -> d.pets.any { it.species == sp.id && it.hatched > 0 } }}/${list.size}")
        list.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sp ->
                    val p = d.pets.firstOrNull { it.species == sp.id }
                    Column(Modifier.weight(1f).card(16.dp, if (sp.rare && p == null) C.goldSoft else null).clickable { if (p != null) nav.push(Screen.PetInfo(p.id)) else ask = sp }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            p == null -> { Mini(sp.id, 58.dp, dim = true); Text(sp.title, style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            p.hatched == 0L -> { PetView(p, 58.dp); Meter(p.growth.toFloat() / Pets.HATCH, Modifier.padding(top = 4.dp, bottom = 3.dp), h = 5.dp) }
                            else -> { PetView(p, 58.dp); Text(p.name, style = T.tiny.copy(color = C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        }
        Spacer(Modifier.height(30.dp))
    }
    ask?.let { sp -> Popup({ ask = null }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Mini(sp.id, 120.dp, dim = true) }
        Text(sp.title, style = T.h2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        Text(sp.perkText, style = T.body.copy(color = C.mute), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
        Text(tr("Как найти", "How to find"), style = T.sec, modifier = Modifier.padding(top = 14.dp))
        Text(sp.find, style = T.bodyB, modifier = Modifier.padding(top = 4.dp))
        if (!sp.rare) Text(tr("А ещё яйцо можно купить у гоблина в лавке или получить за уровень сезона.", "An egg can also be bought from the goblin in the shop or earned with a season level."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth().padding(top = 14.dp)) { ask = null }
    } }
}

fun perkShort(sp: Species): String = when (sp.perk) {
    Perk.DAYS -> tr("×2 за первую сессию дня", "×2 for the day's first session")
    Perk.GENRE -> tr("×2 за ", "×2 for ") + sp.likes.mapNotNull { Genres.of(it)?.title?.lowercase() }.joinToString(", ")
    Perk.FINISH -> tr("×2 за пройденные игры", "×2 for finished games"); Perk.QUEST -> tr("×2 за задания недели", "×2 for weekly quests")
    Perk.BOARD -> tr("×2 за настолки", "×2 for board games"); Perk.NIGHT -> tr("×2 ночью", "×2 at night")
    Perk.SHOTS -> tr("×2 за сессии со снимками", "×2 for sessions with pictures"); Perk.NEW -> tr("×3 за новую игру", "×3 for a new game")
    Perk.MOBILE -> tr("×2 за игры на телефоне", "×2 for phone games"); Perk.WIN -> tr("×2 за победы", "×2 for wins")
    Perk.SHORT -> tr("×2 за короткие сессии", "×2 for short sessions"); Perk.FRIENDS -> tr("×2 за игру с друзьями", "×2 for playing with friends")
    Perk.BREAK -> tr("×2 золота за перерывы", "×2 gold for breaks"); Perk.COMEBACK -> tr("×3 за возвращение к игре", "×3 for coming back to a game")
    Perk.DEVOTED -> tr("×2 за любимые игры", "×2 for long-loved games"); Perk.PLATFORMS -> tr("×2 за вторую платформу дня", "×2 for a day's second platform")
    Perk.NOTE -> tr("×2 за сессии с заметкой", "×2 for sessions with a note"); Perk.TROPHY -> tr("×2 золота за трофеи", "×2 gold for trophies")
    Perk.ALL -> tr("+25% ко всему", "+25% to everything")
}
