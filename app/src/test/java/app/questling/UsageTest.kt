package app.questling

import app.questling.data.Usage
import app.questling.data.Usage.Event
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTest {
    private val m = 60_000L
    private fun play(pkg: String, from: Long, to: Long) = listOf(Event(pkg, true, from * m), Event(pkg, false, to * m))
    private fun cut(events: List<Event>, since: Long, to: Long) = Usage.cut(events.sortedBy { it.time }, since * m, to * m) { it }

    @Test fun aSecondSessionAfterTheFirstLookIsFound() {
        val first = play("g", 100, 110)
        val a = cut(first, 0, 112)
        assertEquals(listOf(10), a.found.map { it.minutes })
        assertEquals(112 * m, a.upTo)
        val b = cut(first + play("g", 200, 215), a.upTo / m, 216)
        assertEquals(listOf(15), b.found.map { it.minutes })
        assertEquals(200 * m, b.found.single().start)
    }

    @Test fun aGameStillOnScreenWaitsAndIsFoundWhole() {
        val going = listOf(Event("g", true, 100 * m))
        val a = cut(going, 0, 102)
        assertEquals(emptyList<Any>(), a.found)
        assertEquals(100 * m, a.upTo)
        val b = cut(going + Event("g", false, 120 * m), a.upTo / m, 121)
        assertEquals(listOf(20), b.found.map { it.minutes })
    }

    @Test fun goingBackToTheGameAfterALookIsANewSession() {
        val a = cut(play("g", 100, 110), 0, 111)
        assertEquals(1, a.found.size)
        val b = cut(play("g", 100, 110) + play("g", 112, 120), a.upTo / m, 121)
        assertEquals(listOf(8), b.found.map { it.minutes })   // not lost by merging into the first, not counted twice
    }

    @Test fun shortBreaksJoinAndShortStretchesAreSkipped() {
        val r = cut(play("g", 100, 110) + play("g", 113, 120) + play("h", 130, 132), 0, 140)
        assertEquals(listOf(17), r.found.map { it.minutes })
    }

    // #4: an ad (or a loader) comes up inside the game; the main screen's late "stopped" arrives after the ad is up
    @Test fun aScreenChangeInsideTheGameDoesNotEndTheSession() {
        val ev = listOf(
            Event("g", true, 100 * m, "Main"), Event("g", false, 110 * m, "Main"), Event("g", true, 110 * m, "Ad"),
            Event("g", false, 110 * m + 500, "Main"),                       // STOPPED of Main, after Ad resumed
            Event("g", false, 111 * m, "Ad"), Event("g", true, 111 * m, "Main"),
            Event("g", false, 111 * m + 500, "Ad"),                         // STOPPED of Ad, after Main resumed
            Event("g", false, 140 * m, "Main"), Event("g", false, 140 * m + 500, "Main"))
        val r = cut(ev, 0, 150)
        assertEquals(listOf(40), r.found.map { it.minutes })
        assertEquals(150 * m, r.upTo)
    }

    @Test fun aScreenWhoseLeavingGotLostDoesNotHoldTheGameForever() {
        val ev = listOf(Event("g", true, 0, "Old")) + listOf(Event("g", true, 500 * m, "Main"), Event("g", false, 520 * m, "Main"))
        val r = cut(ev, 400, 530)
        assertEquals(listOf(20), r.found.map { it.minutes })
        assertEquals(530 * m, r.upTo)
    }

    @Test fun aLostPauseLongAgoDoesNotHoldEveryLookBack() {
        val r = cut(listOf(Event("g", true, 0)) + play("h", 500, 510), 400, 520)
        assertEquals(listOf("h"), r.found.map { it.pkg })
        assertEquals(520 * m, r.upTo)
    }
}
