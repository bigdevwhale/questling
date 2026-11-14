package app.questling.ui.tavern

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.questling.container
import app.questling.currentLanguage
import app.questling.data.Backup
import app.questling.data.Game
import app.questling.data.Media
import app.questling.data.Settings
import app.questling.data.Usage
import app.questling.domain.Kinds
import app.questling.domain.Pets
import app.questling.domain.Shop
import app.questling.domain.tr
import app.questling.setLanguage
import app.questling.ui.Data
import app.questling.ui.Nav
import app.questling.ui.art.Art
import app.questling.ui.art.EggView
import app.questling.ui.art.Pose
import app.questling.ui.art.Sky
import app.questling.ui.art.TavernScene
import app.questling.ui.components.Badge
import app.questling.ui.components.Btn
import app.questling.ui.components.Card
import app.questling.ui.components.Chip
import app.questling.ui.components.Field
import app.questling.ui.components.Fold
import app.questling.ui.components.Ghost
import app.questling.ui.components.GoldPill
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.Page
import app.questling.ui.components.Section
import app.questling.ui.components.Segments
import app.questling.ui.components.card
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.launch

/** The goblin's shop: things for the tavern and surprise eggs, for gold. */
@Composable
fun ShopPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var preview by remember { mutableStateOf<String?>(null) }
    Page(tr("Лавка гоблина", "The goblin's shop"), { nav.pop() }, action = { GoldPill(d.s.gold, Modifier.padding(end = 10.dp)) }) {
        Box(Modifier.fillMaxWidth().card(20.dp)) { TavernScene(d.team, d.s.items - d.s.hidden, Pose.IDLE, preview = preview, sky = Sky.of(d.s)) }
        Section(tr("Яйцо из-под прилавка", "An egg from under the counter"))
        val left = Pets.all.count { sp -> !sp.rare && sp.id != "questling" && d.pets.none { it.species == sp.id } }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EggView("goblin", 64.dp, .3f); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Яйцо-сюрприз", "A surprise egg"), style = T.bodyB)
                    Text(if (left == 0) tr("Все уже живут в трактире", "Everyone already lives here") else tr("Кто-то, кого ещё нет в трактире", "Someone not yet in the tavern"), style = T.small)
                }
                Btn("${Shop.EGG}", small = true, enabled = d.s.gold >= Shop.EGG && left > 0) { c.write { if (buyEgg()) say(tr("Яйцо в трактире!", "An egg in the tavern!")) } }
            }
        }
        Section(tr("Для трактира", "For the tavern"))
        Shop.all.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { it0 ->
                val have = it0.id in d.s.items; val hidden = it0.id in d.s.hidden
                Column(Modifier.weight(1f).card(16.dp).clickable { preview = if (preview == it0.id) null else it0.id }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ItemPic(it0.id)
                    Text(it0.title, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    when {
                        have -> Text(if (hidden) tr("Достать", "Bring out") else tr("Убрать", "Put away"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable {
                            c.write { settings.update { s -> s.copy(hidden = if (hidden) s.hidden - it0.id else s.hidden + it0.id) } } }.padding(4.dp))
                        else -> Btn("${it0.price}", Modifier.padding(top = 4.dp), small = true, enabled = d.s.gold >= it0.price) { c.write { if (buy(it0.id)) say(tr("Куплено: ${it0.title}", "Bought: ${it0.title}")) }; preview = null }
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        } }
        Text(tr("Золото дают за сессии, задания недели, трофеи и уровни сезона. За деньги его не купить.", "Gold comes from sessions, weekly quests, trophies and season levels. It can't be bought with money."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ItemPic(id: String) {
    val ctx = LocalContext.current
    val img = Art.item(ctx, id)
    if (img != null) Image(img, null, Modifier.size(72.dp), filterQuality = FilterQuality.Medium) else Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { Icon(Icons.GIFT, C.mute, 34.dp) }
}

// ---------------- settings ----------------

object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "questling"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
    const val SITE = "https://bigdevwhale.github.io/stillcraft/"
    const val CODE = "https://github.com/bigdevwhale/questling"
}

@Composable
private fun LinkRow(icon: Icons, tint: Color, title: String, sub: String?, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(icon, tint, 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Icon(Icons.NEXT, C.mute, 18.dp)
    }
}

@Composable
private fun Step(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Ghost("−", small = true, onClick = onMinus)
        Text(value, style = T.bodyB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Ghost("+", small = true, onClick = onPlus)
    }
}

@Composable
private fun Toggle(title: String, sub: String?, on: Boolean, onSet: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSet(!on) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Chip(if (on) tr("вкл", "on") else tr("выкл", "off"), on) { onSet(!on) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = d.s
    fun up(f: (Settings) -> Settings) = c.write { settings.update(f) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { val text = Backup.export(c); runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            c.say(tr("Копия сохранена", "Backup saved")) } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull().orEmpty()
            c.say(if (Backup.import(c, text)) tr("Трактир восстановлен", "The tavern is restored") else tr("Это не копия Questling", "That's not a Questling backup")) } }
    val askRead = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> up { it.copy(shotsAuto = ok) } }
    Page(tr("Настройки", "Settings"), { nav.pop() }) {
        Fold(Icons.PAD, C.acc, tr("Игрок", "The player"), s.player.ifBlank { tr("имя и платформы", "name and platforms") }) {
            var name by remember { mutableStateOf(s.player) }
            Field(name, { name = it; up { st -> st.copy(player = it.trim()) } }, tr("Имя для карточек и протокола очков", "A name for cards and the score sheet"))
            Text(tr("Во что играете", "What you play"), style = T.small, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Game.KINDS.dropLast(1).forEach { k -> Chip(Kinds.plural(k), k in s.kinds) { up { it.copy(kinds = if (k in it.kinds) it.kinds - k else it.kinds + k) } } } }
            Text(tr("Ваши платформы – их предложат первыми", "Your platforms – offered first"), style = T.small, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Kinds.PLATFORMS.forEach { p -> Chip(Kinds.platformTitle(p), p in s.platforms) { up { it.copy(platforms = if (p in it.platforms) it.platforms - p else it.platforms + p) } } } }
        }
        Fold(Icons.MOON, C.gold, tr("Перерывы", "Breaks"), if (s.breakAfter == 0) tr("питомец не напоминает", "the pet doesn't remind") else tr("питомец зевнёт через ${s.breakAfter} мин", "the pet yawns after ${s.breakAfter} min")) {
            Text(tr("Через сколько минут игры без перерыва питомец предложит размяться", "After how many minutes of play without a break the pet suggests a stretch"), style = T.small)
            Step(if (s.breakAfter == 0) tr("выключено", "off") else tr("${s.breakAfter} мин", "${s.breakAfter} min"),
                { up { it.copy(breakAfter = if (it.breakAfter <= 30) 0 else it.breakAfter - 15) } }, { up { it.copy(breakAfter = if (it.breakAfter == 0) 30 else (it.breakAfter + 15).coerceAtMost(240)) } })
            Text(tr("Он спросит один раз за сессию и не будет настаивать. Долгие часы подряд кормят питомцев слабее – отдых выгоднее.", "It asks once a session and never insists. Long hours on end feed the creatures less – rest pays better."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
            if (android.os.Build.VERSION.SDK_INT >= 33) { val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                Ghost(tr("Разрешить уведомления", "Allow notifications"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { ask.launch(android.Manifest.permission.POST_NOTIFICATIONS) } }
        }
        Fold(Icons.CAMERA, C.ok, tr("Скриншоты", "Screenshots"), if (s.shotsAuto && Media.canRead(ctx)) tr("подхватываются сами", "picked up by themselves") else tr("добавляются вручную", "added by hand")) {
            Toggle(tr("Подхватывать скриншоты сессии", "Pick up a session's screenshots"), tr("когда заканчиваете сессию, приложение предложит снимки экрана, сделанные за это время", "when you finish a session the app offers the screenshots made meanwhile"), s.shotsAuto && Media.canRead(ctx)) { on ->
                if (on && !Media.canRead(ctx)) askRead.launch(Media.readPermission) else up { it.copy(shotsAuto = on) } }
            Text(tr("Снимки копируются в приложение и никуда не отправляются. Нужен доступ к фото – только чтобы найти скриншоты.", "The pictures are copied into the app and sent nowhere. Access to photos is needed only to find the screenshots."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        Fold(Icons.PHONE, C.ok, tr("Игры на телефоне", "Phone games"), if (s.tracker && Usage.allowed(ctx)) tr("сессии находятся сами", "sessions are found by themselves") else tr("выключено", "off")) {
            val allowed = Usage.allowed(ctx)
            Toggle(tr("Находить сессии в телефоне", "Find sessions on the phone"), tr("телефон знает, в какие игры и сколько вы играли; приложение предложит записать", "the phone knows which games you played and for how long; the app offers to write them down"), s.tracker && allowed) { on ->
                if (on && !allowed) runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                up { it.copy(tracker = on) } }
            if (s.tracker && !allowed) Text(tr("Включите Questling в списке «Доступ к данным об использовании» и вернитесь.", "Turn Questling on in the «Usage access» list and come back."), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 4.dp))
            if (s.ignored.isNotEmpty()) Ghost(tr("Забыть «это не игра» (${s.ignored.size})", "Forget «not a game» (${s.ignored.size})"), Modifier.padding(top = 8.dp), small = true) { up { it.copy(ignored = emptySet()) } }
            Text(tr("Данные не покидают телефон. Найденное попадает в журнал только после вашего «да».", "The data never leaves the phone. What is found goes into the journal only after your «yes»."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        Fold(Icons.PALETTE, C.acc, tr("Вид и язык", "Look and language"), null) {
            Segments(listOf(tr("Светлая", "Light"), tr("Тёмная", "Dark"), tr("Как в системе", "System")), when (s.theme) { "light" -> 0; "dark" -> 1; else -> 2 }) { i -> up { it.copy(theme = listOf("light", "dark", "system")[i]) } }
            Spacer(Modifier.height(10.dp))
            val lang = currentLanguage()
            Segments(listOf("Русский", "English", tr("Как в системе", "System")), when (lang) { "ru" -> 0; "en" -> 1; else -> 2 }) { i -> setLanguage(listOf("ru", "en", "")[i]) }
            Text(tr("За окном – погода в городе: ${s.town.ifBlank { "не определён" }}", "Out of the window – the weather in: ${s.town.ifBlank { "unknown" }}"), style = T.small, modifier = Modifier.padding(top = 10.dp))
        }
        Fold(Icons.SAVE, C.ok, tr("Резервная копия", "Backup"), tr("файл со всем журналом", "one file with the whole journal")) {
            Btn(tr("Сохранить копию", "Save a backup"), Modifier.fillMaxWidth(), small = true) { save.launch("questling-${java.time.LocalDate.now()}.json") }
            Ghost(tr("Восстановить из файла", "Restore from a file"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { open.launch(arrayOf("application/json", "*/*")) }
            Text(tr("В копии – игры, сессии, игроки, питомцы и трофеи. Обложки и снимки в файл не попадают. Восстановление заменит всё, что есть сейчас.", "The backup holds games, sessions, players, creatures and trophies. Covers and pictures are not in the file. Restoring replaces everything there is now."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        Fold(Icons.HEART, C.bad, tr("Поддержать", "Support"), tr("StillCraft держится на поддержке пользователей", "StillCraft lives on goodwill")) { LinkRow(Icons.HEART, C.bad, tr("Поддержать разработку", "Support development"), null, Community.SUPPORT) }
        Fold(Icons.GLOBE, C.ok, "StillCraft", tr("Questling – одно из небольших приложений StillCraft", "Questling is one of the small StillCraft apps")) {
            LinkRow(Icons.HEART, C.acc, tr("Сообщество в Telegram", "Telegram community"), tr("новости, обсуждения, другие приложения", "news, talk and the other apps"), Community.CHAT)
            LinkRow(Icons.CLOSE, C.bad, tr("Сообщить об ошибке", "Report a bug"), tr("через бота StillCraft", "through the StillCraft bot"), Community.BUG)
            LinkRow(Icons.SPARK, C.gold, tr("Предложить идею", "Suggest an idea"), null, Community.IDEA)
            LinkRow(Icons.STAR, C.ok, tr("Следить за обновлениями", "Follow updates"), tr("бот напишет, когда выйдет новая версия", "the bot writes when a new version is out"), Community.FOLLOW)
            LinkRow(Icons.HOME, C.acc, tr("Все приложения StillCraft", "All StillCraft apps"), null, Community.SITE)
            LinkRow(Icons.LIST, C.mute, tr("Исходный код", "Source code"), "GitHub", Community.CODE)
        }
        Fold(Icons.SPARK, C.acc, tr("Подсказки", "Hints"), tr("короткие пояснения на экранах", "short notes on the screens")) {
            Ghost(tr("Показать заново", "Show again"), Modifier.fillMaxWidth(), small = true) { up { it.copy(seen = emptySet()) }; c.say(tr("Подсказки покажутся снова", "Hints will show again")) } }
        Text(tr("Questling работает без аккаунта. Всё хранится только на телефоне. В интернет уходят лишь название игры, когда вы ищете её в каталоге (Wikidata и Википедия), и координаты города для погоды за окном (Open-Meteo и MET Norway).",
            "Questling needs no account. Everything stays on the phone. Only two things go online: a game's title when you look it up in the catalogue (Wikidata and Wikipedia), and the town's coordinates for the weather outside the window (Open-Meteo and MET Norway)."), style = T.tiny, modifier = Modifier.padding(top = 14.dp))
        Spacer(Modifier.height(20.dp))
    }
}
