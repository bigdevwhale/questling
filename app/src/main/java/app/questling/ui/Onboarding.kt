package app.questling.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.data.Game
import app.questling.domain.Kinds
import app.questling.domain.Pets
import app.questling.domain.Stage
import app.questling.domain.Trophies
import app.questling.domain.tr
import app.questling.ui.art.Creature
import app.questling.ui.art.Pose
import app.questling.ui.components.Btn
import app.questling.ui.components.Chip
import app.questling.ui.components.Field
import app.questling.ui.components.Ghost
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.card
import app.questling.ui.components.kindColor
import app.questling.ui.components.kindIcon
import app.questling.ui.components.tap
import app.questling.ui.theme.C
import app.questling.ui.theme.T

/** First launch: the questling, what one plays and on what, and how the tavern works. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var kinds by remember { mutableStateOf(setOf(Game.VIDEO)) }
    var plats by remember { mutableStateOf(emptyList<String>()) }
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i -> Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(if (i <= step) C.acc else C.track)) } }
            when (step) {
                0 -> {
                    Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) { Creature("questling", Stage.BABY, Pose.CHEER, 200.dp) }
                    Text(tr("Добро пожаловать в трактир", "Welcome to the tavern"), style = T.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(tr("Это Квестлинг. Он растёт, пока вы играете – в видеоигры, настолки, во что угодно. А за то, как вы играете, в трактир приходят ещё ${Pets.all.size - 1} существ.",
                        "This is the Questling. It grows while you play – video games, board games, anything. And the way you play brings ${Pets.all.size - 1} more creatures to the tavern."),
                        style = T.body.copy(color = C.mute), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Field(name, { name = it.take(30) }, tr("Как вас зовут? (по желанию)", "What's your name? (optional)"), modifier = Modifier.padding(top = 20.dp))
                }
                1 -> {
                    Text(tr("Во что играете?", "What do you play?"), style = T.title, modifier = Modifier.padding(top = 24.dp))
                    Text(tr("Можно всё сразу – потом легко поменять.", "All at once is fine – easy to change later."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                    listOf(Game.VIDEO to tr("ПК, консоли, портативки", "PC, consoles, handhelds"), Game.BOARD to tr("евро, пати, кооперативы – с протоколом очков", "euro, party, co-op – with a score sheet"),
                        Game.CARD to tr("колодостроение, коллекционные, обычная колода", "deck-builders, collectible, a plain deck"), Game.RPG to tr("кампании и ваншоты за столом", "campaigns and one-shots at the table"),
                        Game.MOBILE to tr("сессии находятся в телефоне сами", "sessions are found on the phone by themselves")).forEach { (k, sub) ->
                        val on = k in kinds
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).card(16.dp, if (on) C.accSoft else null).clickable { kinds = if (on) kinds - k else kinds + k }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(kindColor(k).copy(alpha = .16f)), contentAlignment = Alignment.Center) { Icon(kindIcon(k), kindColor(k), 21.dp) }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(Kinds.plural(k), style = T.bodyB); Text(sub, style = T.small) }
                            Box(Modifier.size(22.dp).clip(CircleShape).background(if (on) C.acc else C.track), contentAlignment = Alignment.Center) { if (on) Icon(Icons.CHECK, C.accInk, 14.dp) }
                        }
                    }
                    if (Game.VIDEO in kinds) {
                        Text(tr("На чём", "On what").uppercase(), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Kinds.PLATFORMS.forEach { p -> Chip(Kinds.platformTitle(p), p in plats) { plats = if (p in plats) plats - p else plats + p } } }
                    }
                }
                else -> {
                    Text(tr("Как всё устроено", "How it works"), style = T.title, modifier = Modifier.padding(top = 24.dp))
                    Spacer(Modifier.height(10.dp))
                    listOf(Icons.PLAY to tr("Нажмите большую кнопку – пойдёт сессия. Или запишите ту, что уже сыграли.", "Press the big button and a session starts. Or write down one you have already played."),
                        Icons.PAW to tr("Сессии кормят питомцев. Ночью съеденное превращается в рост.", "Sessions feed the creatures. At night what was eaten turns into growth."),
                        Icons.TROPHY to tr("${Trophies.all.size} трофеев, задания недели и сезон – за игру в своё удовольствие, а не за часы подряд.", "${Trophies.all.size} trophies, weekly quests and a season – for playing with pleasure, not for hours on end."),
                        Icons.MOON to tr("Засидитесь – питомец зевнёт и позовёт размяться. Перерывы здесь вознаграждаются.", "Play too long and your pet yawns and calls for a stretch. Breaks are rewarded here."),
                        Icons.SAVE to tr("Без аккаунта. Всё хранится на телефоне.", "No account. Everything stays on the phone.")).forEach { (ic, text) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).card(16.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(C.accSoft), contentAlignment = Alignment.Center) { Icon(ic, C.acc, 20.dp) }
                            Spacer(Modifier.width(12.dp)); Text(text, style = T.body, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) Ghost(tr("Назад", "Back"), Modifier.weight(1f)) { step-- }
            Btn(if (step == 2) tr("Открыть трактир", "Open the tavern") else tr("Дальше", "Next"), Modifier.weight(1.6f), enabled = step != 1 || kinds.isNotEmpty()) {
                if (step == 2) c.write { begin(name.trim(), kinds, plats) } else step++
            }
        }
    }
}

/** A first-visit introduction to one screen: who explains it, the title and a few short points. */
class Intro(val id: String, val species: String, val title: String, val points: List<String>)

/** The introduction for what is on top now: the page over the tabs, or the tab itself. */
fun introFor(top: Screen?, tab: Int): Intro? = when (top) {
    null -> when (tab) {
        0 -> Intro("i.home", "questling", tr("Это ваш трактир", "This is your tavern"), listOf(
            tr("Здесь живут питомцы. Пока вы играете, они едят, а ночью растут.", "Your creatures live here. While you play they eat, and at night they grow."),
            tr("Большая кнопка начинает сессию: выберите игру, платформу и компанию.", "The big button starts a session: pick a game, a platform and the company."),
            tr("Ниже – во что играете сейчас, задания недели и сезон.", "Below: what you are playing now, the week's quests and the season."),
            tr("Нажмите на питомца, чтобы открыть его страницу.", "Tap a creature to open its page.")))
        1 -> Intro("i.journal", "fairy", tr("Журнал", "The journal"), listOf(
            tr("«Сессии» – всё сыгранное по дням, со снимками и заметками.", "«Sessions» – everything played, day by day, with pictures and notes."),
            tr("«Игры» – коллекция: что играете, что на полке, что пройдено.", "«Games» – the collection: what you play, what's on the shelf, what's finished."),
            tr("«Итоги» – часы, жанры, привычки и «Год в играх».", "«Summary» – hours, genres, habits and the year in games."),
            tr("Вверху – игроки и альбом со всеми снимками.", "At the top – the players and the album of every picture.")))
        2 -> Intro("i.trophies", "coinling", tr("Трофеи", "Trophies"), listOf(
            tr("${Trophies.all.size} трофеев четырёх металлов. Каждый приносит золото и опыт сезона.", "${Trophies.all.size} trophies in four metals. Each brings gold and season xp."),
            tr("Секретные скрыты, пока не выполните условие.", "The secret ones stay hidden until you meet their condition."),
            tr("«Сезон» – пропуск на тридцать уровней: квартал, новые награды. Купить его нельзя.", "«Season» – a pass of thirty levels: a quarter, fresh rewards. It can't be bought.")))
        else -> Intro("i.pets", "slime", tr("Питомцы", "Creatures"), listOf(
            tr("${Pets.all.size} существ: одни – из игровых миров, другие – обычные звери.", "${Pets.all.size} creatures: some out of game worlds, others ordinary animals."),
            tr("Кроме квестлинга, все находятся за то, как вы играете. Нажмите на серый силуэт – узнаете за что.", "Apart from the questling, each is found by the way you play. Tap a grey shadow to see how."),
            tr("Вожак и двое у очага удваивают золото за своё дело.", "The leader and the two by the fire double the gold for their thing."),
            tr("У каждого шесть характеристик, класс и любимый жанр – по играм, на которых он вырос.", "Each has six stats, a class and a favourite genre – from the games it grew on.")))
    }
    Screen.Live -> Intro("i.live", "cat", tr("Сессия идёт", "A session is on"), listOf(
        tr("Таймер идёт и в фоне. «Перерыв» останавливает его.", "The clock runs in the background too. «Break» stops it."),
        tr("Кубики, выбор первого игрока, таймер хода и протокол очков – под рукой.", "Dice, the first-player pick, a turn timer and a score sheet are at hand."),
        tr("В конце запишите, как прошло: итог, момент, настроение, снимки.", "At the end write down how it went: the result, a moment, the mood, pictures.")))
    is Screen.GameInfo -> Intro("i.game", "mimic", tr("Страница игры", "A game's page"), listOf(
        tr("Статус: играю, на полке, пройдена, 100%, отложена.", "Status: playing, on the shelf, finished, 100%, put aside."),
        tr("Оценка от 1 до 10 и заметки – чтобы помнить, на чём остановились.", "A rating from 1 to 10 and notes – to remember where you stopped."),
        tr("Ниже – цифры, альбом и все сессии.", "Below: the numbers, the album and every session.")))
    is Screen.Tools -> Intro("i.tools", "dicey", tr("Инструменты", "Tools"), listOf(
        tr("Кубики от d4 до d100, до восьми сразу.", "Dice from d4 to d100, up to eight at once."),
        tr("«Первый ход» честно выбирает, кто начинает.", "«First move» fairly picks who starts."),
        tr("Таймер хода вибрирует, когда время вышло.", "The turn timer buzzes when the time is up.")))
    Screen.Shop -> Intro("i.shop", "goblin", tr("Лавка гоблина", "The goblin's shop"), listOf(
        tr("Золото тратится здесь: на вещи для трактира и яйца-сюрпризы.", "Gold is spent here: on things for the tavern and surprise eggs."),
        tr("Нажмите на вещь, чтобы примерить её в трактире до покупки.", "Tap a thing to try it in the tavern before buying.")))
    Screen.Friends -> Intro("i.friends", "bear", tr("Игроки", "Players"), listOf(
        tr("Добавьте тех, с кем играете, и отмечайте их в сессиях.", "Add the people you play with and tick them in sessions."),
        tr("Приложение ведёт счёт между вами и помнит любимую игру на двоих.", "The app keeps the score between you and remembers your game together.")))
    else -> null
}

/** The introduction as a card sliding up over the screen; «Got it» stores its id in Settings.seen. */
@Composable
fun IntroSheet(intro: Intro) {
    val c = LocalContext.current.container
    fun done() = c.write { settings.update { it.copy(seen = it.seen + intro.id) } }
    BackHandler { done() }
    val a = remember(intro.id) { Animatable(0f) }
    LaunchedEffect(intro.id) { a.animateTo(1f, tween(320)) }
    Box(Modifier.fillMaxSize().background(Color(0x99000000).copy(alpha = .6f * a.value)).tap {}, contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().offset(y = ((1 - a.value) * 220).dp).alpha(a.value).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.bg)
            .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Creature(intro.species, Stage.ADULT, Pose.CHEER, 96.dp)
                Column(Modifier.weight(1f).padding(start = 6.dp, bottom = 14.dp)) { Text(intro.title, style = T.h2) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                intro.points.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(C.accSoft), contentAlignment = Alignment.Center) { Text("${i + 1}", style = T.smallB.copy(color = C.acc)) }
                        Spacer(Modifier.width(10.dp))
                        Text(s, style = T.body, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth()) { done() }
        }
    }
}
