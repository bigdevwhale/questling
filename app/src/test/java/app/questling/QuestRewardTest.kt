package app.questling

import androidx.test.core.app.ApplicationProvider
import app.questling.data.Game
import app.questling.data.Play
import app.questling.domain.today
import app.questling.domain.weekStart
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A weekly quest is paid as soon as it is done, each on its own (#3). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestRewardTest {
    @Test fun threeShortSessionsPayTheQuest() = runBlocking {
        val c = ApplicationProvider.getApplicationContext<QuestApp>().container
        c.begin("Тест", setOf("video"), listOf("Android"))
        val monday = weekStart(today())
        c.settings.update { it.copy(questWeek = monday, quests = listOf("short3", "moment1", "shots3"), questsDone = emptySet()) }
        val g = c.saveGame(Game(title = "Тетрис", kind = Game.MOBILE, platform = "Android", status = Game.PLAYING, added = today()))
        val gold0 = c.settings.now().gold
        repeat(3) { i -> c.addPlay(Play(gameId = g, day = today(), start = System.currentTimeMillis() - (i + 1) * 3_600_000L, minutes = 15, platform = "Android")) }
        c.addPlay(Play(gameId = g, day = today(), start = System.currentTimeMillis() - 10 * 3_600_000L, minutes = 40, platform = "Android", moment = Play.M_BOSS))
        val s = c.settings.now()
        println("done=${s.questsDone} week=${s.questWeek} monday=$monday quests=${s.quests} gold ${gold0}->${s.gold}")
        assertTrue("short3 in ${s.questsDone}", "short3" in s.questsDone)
        assertTrue("moment1 in ${s.questsDone}", "moment1" in s.questsDone)
    }
}
