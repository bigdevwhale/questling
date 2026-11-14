package app.questling

import app.questling.data.Game
import app.questling.data.Pet
import app.questling.data.Play
import app.questling.data.Settings
import app.questling.domain.Engine
import app.questling.domain.Facts
import app.questling.domain.Pets
import app.questling.domain.Season
import app.questling.domain.Stage
import app.questling.domain.Stat
import app.questling.domain.Trophies
import app.questling.domain.stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    private fun pet(species: String = "questling", slot: Int = 0, hatched: Long = 1, xp: Int = 0) = Pet(id = slot + 1L, species = species, name = "x", found = 1, hatched = hatched, slot = slot, xp = xp)
    private fun play(id: Long, game: Long, day: Long, minutes: Int, hour: Int = 19, result: Int = 0) =
        Play(id = id, gameId = game, day = day, start = day * 86_400_000L + hour * 3_600_000L, minutes = minutes, result = result)

    @Test fun longDaysFeedLess() {
        assertEquals(60, Engine.food(60, 0))
        assertEquals(180 + 30, Engine.food(240, 0))          // the fourth hour counts by half
        assertEquals(15, Engine.food(60, 360))               // past six hours – by a quarter
        assertTrue(Engine.food(600, 0) < 400)
    }

    @Test fun leaderEatsMostAndEggsHatch() {
        val pets = listOf(pet(slot = 0), pet("slime", 1), pet("mimic", -1), pet("fox", -1, hatched = 0).copy(growth = Pets.HATCH - 1))
        val fed = Engine.feed(pets, 60, listOf("rpg"), Stat.WIL, today = 10)
        assertEquals(60, fed.pets[0].belly); assertEquals(30, fed.pets[1].belly); assertEquals(0, fed.pets[2].belly)
        assertEquals(1, fed.hatched.size); assertEquals(10L, fed.pets[3].hatched)
        assertEquals(1, fed.pets[0].wil); assertEquals(60, fed.pets[0].genreMap()["rpg"])
    }

    @Test fun nightTurnsFoodIntoGrowth() {
        val (pets, grown) = Engine.digest(listOf(pet(xp = Pets.TEEN - 10).copy(belly = 20)))
        assertEquals(Stage.TEEN, pets[0].stage()); assertEquals(0, pets[0].belly); assertEquals(1, grown.size)
    }

    @Test fun perksDoubleAndLegendsAddAQuarter() {
        val team = listOf(pet("meeple", 0), pet("dragon", 1))
        assertEquals(25, Engine.bonus(10, setOf(app.questling.domain.Perk.BOARD), team))
        assertEquals(12, Engine.bonus(10, emptySet(), team))
        // a genre perk needs its genre
        assertEquals(10, Engine.bonus(10, setOf(app.questling.domain.Perk.GENRE), listOf(pet("slime", 0)), listOf("puzzle")))
        assertEquals(20, Engine.bonus(10, setOf(app.questling.domain.Perk.GENRE), listOf(pet("slime", 0)), listOf("rpg")))
    }

    @Test fun trophiesAndFinds() {
        val games = listOf(Game(id = 1, title = "A", kind = Game.BOARD, genres = "euro", added = 1), Game(id = 2, title = "B", genres = "rpg", added = 1, status = Game.FINISHED, finished = 5))
        val plays = (1..12).map { play(it.toLong(), if (it <= 10) 1 else 2, it.toLong(), 60, result = if (it <= 3) Play.LOSS else Play.NONE) }
        val f = Facts(Settings(), games, plays, emptyList(), emptyList(), listOf(pet()), 20)
        assertEquals(12, f.dayStreak); assertEquals(3, f.lossStreak); assertTrue(f.roseAgain)
        val got = Trophies.fresh(f).map { it.id }.toSet()
        assertTrue("plays10" in got); assertTrue("hours10" in got); assertTrue("board10" in got); assertTrue("finish1" in got); assertFalse("plays25" in got)
        val finds = Engine.finds(f, setOf("questling"))
        assertTrue("meeple" in finds); assertTrue("mimic" in finds); assertTrue("skelly" in finds); assertFalse("slime" in finds)
    }

    @Test fun trophyIdsAreUnique() {
        assertEquals(Trophies.all.size, Trophies.all.map { it.id }.toSet().size)
        assertTrue(Trophies.all.size >= 250)
        assertEquals(36, Pets.all.size)
    }

    @Test fun seasons() {
        assertEquals("2026-4", Season.id(java.time.LocalDate.of(2026, 11, 13).toEpochDay()))
        assertEquals(30, Season.level(Season.STEP * 40))
        assertEquals(1, Season.level(Season.STEP))
    }
}
