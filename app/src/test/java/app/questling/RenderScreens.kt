package app.questling

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.questling.data.DemoSeed
import app.questling.domain.Lang
import app.questling.ui.Onboarding
import app.questling.ui.Root
import app.questling.ui.Start
import app.questling.ui.share.CardKind
import app.questling.ui.share.Look
import app.questling.ui.share.Painter
import app.questling.ui.share.ShareReq
import app.questling.ui.theme.QuestTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every screen of the demo drawn to PNGs in app/build/art/screens (run with `-Prender`).
 * The screen is very tall, so whole scrolling pages fit in one picture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h2400dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val dark = System.getProperty("render.dark") == "1"

    private fun shot(name: String) {
        repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "${if (dark) "d-" else ""}$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<QuestApp>().container
        var i by mutableIntStateOf(-1)
        rule.mainClock.autoAdvance = false
        // onboarding first, on an empty tavern
        rule.setContent { QuestTheme(dark) { key(i) { if (i == -2) Onboarding() else { val s by c.settings.settings.collectAsState(null); s?.let { st -> if (i >= 0 && st.onboarded) Root(st) } } } } }
        rule.runOnUiThread { i = -2 }; shot("0-onboarding")
        runBlocking { DemoSeed.seedIfEmpty(c) }
        // the first-visit introductions are put away for the screenshots
        runBlocking { c.settings.update { it.copy(seen = it.seen + listOf("i.home", "i.journal", "i.trophies", "i.pets", "i.live", "i.game", "i.tools", "i.shop", "i.friends")) } }
        val pets = runBlocking { c.dao.petList() }
        val games = runBlocking { c.dao.gameList() }
        val plays = runBlocking { c.dao.playList() }
        val top = plays.groupBy { it.gameId }.maxBy { it.value.size }.key
        val table = plays.last { it.friends.isNotBlank() && it.note.isNotBlank() }
        val friend = runBlocking { c.dao.friendList().first() }
        val shots = listOf(
            "1-home" to (0 to null), "2-journal" to (1 to null), "3-trophies" to (2 to null), "4-pets" to (3 to null),
            "5-games" to (1 to "games"), "6-stats" to (1 to "stats"), "7-season" to (2 to "season"),
            "8-pet" to (3 to "pet:${pets.first { it.slot == 0 }.id}"), "9-start" to (0 to "start"), "10-play" to (0 to "play"),
            "11-game" to (1 to "game:$top"), "12-session" to (1 to "session:${table.id}"), "13-friends" to (1 to "friends"), "14-friend" to (1 to "friend:${friend.id}"),
            "15-dice" to (0 to "tools:0"), "16-first" to (0 to "tools:1"), "17-timer" to (0 to "tools:2"), "18-album" to (1 to "album"), "19-found" to (0 to "found"),
            "20-addgame" to (1 to "addgame"), "21-shop" to (0 to "shop"), "22-settings" to (0 to "settings"), "23-wrapped" to (1 to "wrapped"), "24-share" to (0 to "share:WEEK"),
        )
        for ((n, sh) in shots.withIndex()) {
            Start.tab = sh.second.first; Start.open = sh.second.second
            rule.runOnUiThread { i = n }
            shot(sh.first)
        }
        // a session running
        runBlocking { c.startLive(top, "PC", listOf(friend.id)); c.liveScores(mapOf(0L to 12, friend.id to 9)) }
        Start.tab = 0; Start.open = "live"; rule.runOnUiThread { i = 200 }; shot("25-live")
        runBlocking { c.cancelLive() }
        // every piece of furniture at once, to check the layout
        runBlocking { c.settings.update { it.copy(items = app.questling.domain.Shop.all.map { i -> i.id }.toSet(), hidden = emptySet()) } }
        Start.tab = 0; Start.open = null; rule.runOnUiThread { i = 300 }; shot("1c-home-full")
        val data = runBlocking { app.questling.ui.Data(c.settings.now(), c.dao.gameList(), c.dao.playList().sortedByDescending { it.start }, c.dao.seatList(), c.dao.friendList(), c.dao.petList()) }
        val cards = File(out.parentFile, "cards").apply { mkdirs() }
        val reqs = listOf(ShareReq(CardKind.WEEK), ShareReq(CardKind.MONTH), ShareReq(CardKind.YEAR), ShareReq(CardKind.SESSION, table.id), ShareReq(CardKind.GAME, top),
            ShareReq(CardKind.PET, pets.first().id), ShareReq(CardKind.TAVERN), ShareReq(CardKind.COLLECTION), ShareReq(CardKind.TROPHIES))
        reqs.forEachIndexed { k, r -> listOf(false, true).forEach { story ->
            val bmp = Painter(rule.activity, data, Look(story, k % 6, false)).render(r).first
            File(cards, "${k}-${r.kind.name.lowercase()}${if (story) "-story" else ""}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) } } }
    }
}
