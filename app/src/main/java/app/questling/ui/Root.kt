package app.questling.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.questling.BuildConfig
import app.questling.container
import app.questling.data.Friend
import app.questling.data.Game
import app.questling.data.Pet
import app.questling.data.Play
import app.questling.data.Seat
import app.questling.data.Settings
import app.questling.data.Usage
import app.questling.domain.Engine
import app.questling.domain.Facts
import app.questling.domain.today
import app.questling.domain.tr
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.home.HomeScreen
import app.questling.ui.home.Inbox
import app.questling.ui.journal.AlbumPage
import app.questling.ui.journal.FoundPage
import app.questling.ui.journal.FriendPage
import app.questling.ui.journal.FriendsPage
import app.questling.ui.journal.GameEditPage
import app.questling.ui.journal.GamePage
import app.questling.ui.journal.JournalScreen
import app.questling.ui.journal.ViewerPage
import app.questling.ui.pets.PetPage
import app.questling.ui.pets.PetsScreen
import app.questling.ui.play.LivePage
import app.questling.ui.play.PlayEditPage
import app.questling.ui.play.PlayPage
import app.questling.ui.play.StartSheet
import app.questling.ui.play.ToolsPage
import app.questling.ui.share.CardKind
import app.questling.ui.share.ShareReq
import app.questling.ui.share.ShareSheet
import app.questling.ui.stats.WrappedPage
import app.questling.ui.tavern.SettingsPage
import app.questling.ui.tavern.ShopPage
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import app.questling.ui.trophies.TrophiesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Where a demo/screenshot launch opens (set from the intent in demo builds only). */
object Start { var tab = 0; var open: String? = null }

/** A page over the tabs. */
sealed class Screen {
    data class PetInfo(val id: Long) : Screen()
    data object Shop : Screen()
    data object Settings : Screen()
    data object Live : Screen()
    /** A session written by hand ([id] 0) or an existing one being edited. */
    data class PlayEdit(val id: Long = 0, val gameId: Long = 0) : Screen()
    data class PlayInfo(val id: Long) : Screen()
    data class GameInfo(val id: Long) : Screen()
    data class GameEdit(val id: Long = 0, val title: String = "", val thenPlay: Boolean = false) : Screen()
    data object Friends : Screen()
    data class FriendInfo(val id: Long) : Screen()
    data class Tools(val tab: Int = 0) : Screen()
    data class Album(val gameId: Long = 0) : Screen()
    data class Viewer(val shots: List<String>, val at: Int) : Screen()
    data object Found : Screen()
    data class Wrapped(val year: Int) : Screen()
}

class Nav {
    var tab by mutableIntStateOf(0)
    /** The part of the journal tab: sessions, games, the summary. */
    var journal by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()
    var share by mutableStateOf<ShareReq?>(null)
    var start by mutableStateOf(false)
    /** The game the start sheet opens on (a game just added, or «play» on a game's page). */
    var startGame by mutableLongStateOf(0L)
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(s: Screen) { pop(); push(s) }
}

/** Everything the screens read, collected once. [plays] the newest first. */
class Data(val s: Settings, val games: List<Game>, val plays: List<Play>, val seats: List<Seat>, val friends: List<Friend>, val pets: List<Pet>, val today: Long = today()) {
    val gameById = games.associateBy { it.id }
    val friendById = friends.associateBy { it.id }
    val team = Engine.team(pets)
    val leader = team.firstOrNull { it.slot == 0 } ?: pets.firstOrNull { it.hatched > 0 }
    val first = plays.minOfOrNull { it.day } ?: today
    val byGame = plays.groupBy { it.gameId }
    val seatsByPlay = seats.groupBy { it.playId }
    val facts by lazy { Facts(s, games, plays, seats, friends, pets, today) }
    fun minutes(gameId: Long) = byGame[gameId]?.sumOf { it.minutes } ?: 0
    fun last(gameId: Long) = byGame[gameId]?.firstOrNull()
    /** Games to offer first: the ones being played, by the last session. */
    val recent: List<Game> = games.filter { it.status != Game.WISH }.sortedWith(compareByDescending<Game> { last(it.id)?.start ?: 0L }.thenByDescending { it.id })
    val playing = recent.filter { it.status == Game.PLAYING }
    val liveGame get() = gameById[s.live.gameId]
}

@Composable
fun Root(s: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val games by c.dao.games().collectAsState(initial = emptyList())
    val plays by c.dao.plays().collectAsState(initial = emptyList())
    val seats by c.dao.seats().collectAsState(initial = emptyList())
    val friends by c.dao.friends().collectAsState(initial = emptyList())
    val pets by c.dao.pets().collectAsState(initial = emptyList())
    val data = remember(s, games, plays, seats, friends, pets) { Data(s, games, plays, seats, friends, pets, c.clock()) }
    val nav = remember { Nav().apply { tab = Start.tab } }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.news.collect { note = it; delay(2600); note = null } }
    LaunchedEffect(Unit) { c.tick() }
    LaunchedEffect(Unit) { if (!BuildConfig.SHOTS) while (true) { c.refreshOutside(); delay(30 * 60_000L) } }
    // phone games played since the last look: on every return to the app, not only on its first start
    val life = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(s.tracker) {
        if (!s.tracker || BuildConfig.DEMO) return@LaunchedEffect
        life.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!Usage.allowed(ctx)) return@repeatOnLifecycle
            val st = c.settings.now()
            val now = System.currentTimeMillis()
            val since = if (st.scanned == 0L) now - 3 * 86400_000L else maxOf(st.scanned, now - 14 * 86400_000L)
            val known = c.dao.gameList().map { it.pkg }.filter { it.isNotBlank() }.toSet()
            val scan = withContext(Dispatchers.IO) { Usage.scan(ctx, since, now, known, st.ignored) }
            c.foundNew(scan.found, scan.upTo)
        }
    }
    // a session running when the app opens: straight to it
    LaunchedEffect(Unit) { if (s.live.on && Start.open == null) nav.push(Screen.Live) }
    LaunchedEffect(Unit) {
        when (val o = Start.open) {
            null -> {}
            "shop" -> nav.push(Screen.Shop); "settings" -> nav.push(Screen.Settings); "live" -> nav.push(Screen.Live); "start" -> nav.start = true
            "play" -> nav.push(Screen.PlayEdit()); "friends" -> nav.push(Screen.Friends); "tools" -> nav.push(Screen.Tools()); "album" -> nav.push(Screen.Album())
            "found" -> nav.push(Screen.Found); "addgame" -> nav.push(Screen.GameEdit())
            "games" -> nav.journal = 1; "stats" -> nav.journal = 2; "season" -> {}
            "wrapped" -> nav.push(Screen.Wrapped(java.time.LocalDate.ofEpochDay(c.clock()).year))
            else -> {
                val id = o.substringAfter(':').toLongOrNull() ?: -1
                when {
                    o.startsWith("pet:") -> nav.push(Screen.PetInfo(id))
                    o.startsWith("game:") -> nav.push(Screen.GameInfo(id))
                    o.startsWith("session:") -> nav.push(Screen.PlayInfo(id))
                    o.startsWith("friend:") -> nav.push(Screen.FriendInfo(id))
                    o.startsWith("tools:") -> nav.push(Screen.Tools(id.toInt()))
                    o.startsWith("share:") -> nav.share = ShareReq(CardKind.valueOf(o.drop(6).substringBefore(':')), o.drop(6).substringAfter(':', "0").toLong())
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                when (nav.tab) {
                    0 -> HomeScreen(data, nav)
                    1 -> JournalScreen(data, nav)
                    2 -> TrophiesScreen(data, nav, Start.open == "season")
                    else -> PetsScreen(data, nav)
                }
            }
            Dock(s, nav.tab, { nav.tab = it }) { if (s.live.on) nav.push(Screen.Live) else nav.start = true }
        }
        for (scr in nav.stack) Box(Modifier.fillMaxSize()) {
            when (scr) {
                is Screen.PetInfo -> PetPage(scr.id, data, nav)
                Screen.Shop -> ShopPage(data, nav)
                Screen.Settings -> SettingsPage(data, nav)
                Screen.Live -> LivePage(data, nav)
                is Screen.PlayEdit -> PlayEditPage(scr, data, nav)
                is Screen.PlayInfo -> PlayPage(scr.id, data, nav)
                is Screen.GameInfo -> GamePage(scr.id, data, nav)
                is Screen.GameEdit -> GameEditPage(scr, data, nav)
                Screen.Friends -> FriendsPage(data, nav)
                is Screen.FriendInfo -> FriendPage(scr.id, data, nav)
                is Screen.Tools -> ToolsPage(scr.tab, data, nav)
                is Screen.Album -> AlbumPage(scr.gameId, data, nav)
                is Screen.Viewer -> ViewerPage(scr, nav)
                Screen.Found -> FoundPage(data, nav)
                is Screen.Wrapped -> WrappedPage(scr.year, data, nav)
            }
        }
        if (nav.start) StartSheet(data, nav) { nav.start = false }
        if (nav.stack.isEmpty() && !nav.start && !BuildConfig.SHOTS) Inbox(data, nav)
        // a first-visit introduction to whatever is on top (after any news)
        if (!BuildConfig.SHOTS && s.inbox.isEmpty() && nav.share == null && !nav.start) introFor(nav.stack.lastOrNull(), nav.tab)?.takeIf { it.id !in s.seen }?.let { IntroSheet(it) }
        nav.share?.let { ShareSheet(it, data) { nav.share = null } }
        AnimatedVisibility(note != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 18.dp, end = 18.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(note ?: "", style = T.bodyB.copy(color = C.bg), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.ink).padding(horizontal = 16.dp, vertical = 11.dp))
        }
    }
    LaunchedEffect(Unit) { Start.open = null }
}

/**
 * The bottom bar as the tavern's counter: honey planks with a brass trim and rivets, drawn in code, and pixel icons
 * from the sheet (nav_*). The open tab rises a little in a warm glow; the medallion in the middle starts a session.
 */
@Composable
private fun Dock(s: Settings, sel: Int, onSel: (Int) -> Unit, onPlay: () -> Unit) {
    val tabs = listOf(tr("Трактир", "Tavern") to ("tavern" to Icons.MUG), tr("Журнал", "Journal") to ("journal" to Icons.LOG),
        tr("Трофеи", "Trophies") to ("trophies" to Icons.TROPHY), tr("Питомцы", "Creatures") to ("pets" to Icons.PAW))
    val wood = listOf(androidx.compose.ui.graphics.Color(0xFF8A5A33), androidx.compose.ui.graphics.Color(0xFF6E4426), androidx.compose.ui.graphics.Color(0xFF55331C))
    val brass = androidx.compose.ui.graphics.Color(0xFFE2B45A)
    Box(Modifier.fillMaxWidth()) {
        // the counter
        androidx.compose.foundation.Canvas(Modifier.matchParentSize().padding(top = 14.dp)) {
            drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(wood))
            val seam = androidx.compose.ui.graphics.Color(0x33000000)
            // planks: two long seams, and joints staggered between them
            for (k in 1..2) drawRect(seam, androidx.compose.ui.geometry.Offset(0f, size.height * k / 3f), androidx.compose.ui.geometry.Size(size.width, 2f))
            for (k in 0..5) { val x = size.width * (k + if (k % 2 == 0) .3f else .8f) / 6f; val row = k % 3
                drawRect(seam, androidx.compose.ui.geometry.Offset(x, size.height * row / 3f), androidx.compose.ui.geometry.Size(2f, size.height / 3f)) }
            // the brass trim along the top, with rivets
            drawRect(androidx.compose.ui.graphics.Color(0xFF3A2414), size = androidx.compose.ui.geometry.Size(size.width, 7.dp.toPx()))
            drawRect(brass, androidx.compose.ui.geometry.Offset(0f, 1.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
            drawRect(androidx.compose.ui.graphics.Color(0x66FFFFFF), androidx.compose.ui.geometry.Offset(0f, 1.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
            val n = 9
            for (k in 0 until n) { val x = size.width * (k + .5f) / n
                drawCircle(androidx.compose.ui.graphics.Color(0xFF3A2414), 3.dp.toPx(), androidx.compose.ui.geometry.Offset(x, 3.dp.toPx()))
                drawCircle(brass, 2.dp.toPx(), androidx.compose.ui.geometry.Offset(x, 3.dp.toPx())) }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
            tabs.forEachIndexed { i, (label, art) ->
                if (i == 2) Box(Modifier.weight(1.15f), contentAlignment = Alignment.BottomCenter) {
                    // the medallion: a brass ring standing proud of the counter
                    // the medallion is its own brass ring; the hourglass sits on a dark disc while a session runs
                    Box(Modifier.offset(y = (-6).dp).size(68.dp).shadow(10.dp, CircleShape).clip(CircleShape)
                        .background(if (s.live.on) androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFF2F6F62), androidx.compose.ui.graphics.Color(0xFF173A33))) else androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color(0xFF3A2414)))
                        .then(if (s.live.on) Modifier.border(3.dp, brass, CircleShape) else Modifier).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
                        NavArt(if (s.live.on) "live" else "play", if (s.live.on) Icons.TIMER else Icons.PLAY, if (s.live.on) 44.dp else 68.dp, 1f)
                    }
                }
                val on = i == sel
                val lift by androidx.compose.animation.core.animateDpAsState(if (on) (-5).dp else 0.dp, label = "lift")
                Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.offset(y = lift).size(44.dp), contentAlignment = Alignment.Center) {
                        if (on) Box(Modifier.size(44.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0x88FFC966), androidx.compose.ui.graphics.Color(0x00FFC966)))))
                        NavArt(art.first, art.second, if (on) 38.dp else 32.dp, if (on) 1f else .72f)
                    }
                    Text(label, style = T.tiny.copy(color = if (on) androidx.compose.ui.graphics.Color(0xFFFFD98A) else androidx.compose.ui.graphics.Color(0xCCF3E3C8),
                        fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.Bold else null), modifier = Modifier.padding(top = 1.dp))
                }
            }
        }
    }
}

/** A bar icon from the sheet, or the plain vector one until the sheet is cut. */
@Composable
private fun NavArt(name: String, fallback: Icons, size: androidx.compose.ui.unit.Dp, alpha: Float) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val img = remember(name) { app.questling.ui.art.Art.get(ctx, "nav_$name") }
    if (img == null) { Icon(fallback, androidx.compose.ui.graphics.Color(0xFFF3E3C8).copy(alpha = alpha), size * .7f); return }
    androidx.compose.foundation.Image(img, null, Modifier.size(size), alpha = alpha, filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium)
}
