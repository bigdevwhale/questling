package app.questling.domain

import app.questling.data.Friend
import app.questling.data.Game
import app.questling.data.Play
import app.questling.data.Seat
import java.time.LocalDate
import java.time.LocalTime

fun hm(min: Int): String = "%d:%02d".format(min / 60, min % 60)
fun nowMin(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
/** Minutes of the day a moment falls on. */
fun minOfDay(ms: Long): Int = java.time.Instant.ofEpochMilli(ms).atZone(zone).let { it.hour * 60 + it.minute }

/** A stretch of days to look at. */
data class Span(val from: Long, val to: Long, val label: String) {
    val days get() = (to - from + 1).toInt().coerceAtLeast(1)
    fun has(d: Long) = d in from..to
}

enum class SpanKind { WEEK, MONTH, SEASON, YEAR, ALL }

object Spans {
    fun of(kind: SpanKind, back: Int, first: Long, today: Long): Span = when (kind) {
        SpanKind.WEEK -> { val m = weekStart(today) - 7L * back; Span(m, m + 6, "${fmtDayShort(m)} – ${fmtDayShort(m + 6)}") }
        SpanKind.MONTH -> { val m = LocalDate.ofEpochDay(today).withDayOfMonth(1).minusMonths(back.toLong())
            Span(m.toEpochDay(), m.plusMonths(1).toEpochDay() - 1, "${monthName(m.monthValue).cap()} ${m.year}") }
        SpanKind.SEASON -> { val d = LocalDate.ofEpochDay(today); val q = LocalDate.of(d.year, (d.monthValue - 1) / 3 * 3 + 1, 1).minusMonths(3L * back)
            Span(q.toEpochDay(), q.plusMonths(3).toEpochDay() - 1, Season.name(Season.id(q.toEpochDay()))) }
        SpanKind.YEAR -> { val y = LocalDate.ofEpochDay(today).year - back
            Span(LocalDate.of(y, 1, 1).toEpochDay(), LocalDate.of(y, 12, 31).toEpochDay(), "$y") }
        SpanKind.ALL -> Span(minOf(first, today), today, tr("За всё время", "All time"))
    }
}

data class Sum<T>(val key: T, val minutes: Int, val count: Int, val share: Float)

/** Numbers about play in a span. [all] is every session, for comparisons and history. */
class Stats(val all: List<Play>, val span: Span, val games: Map<Long, Game>, val today: Long) {
    val plays = all.filter { span.has(it.day) }
    val minutes = plays.sumOf { it.minutes }
    val count = plays.size
    val days = plays.map { it.day }.toSet().size
    /** Days of the span that have passed. */
    val lived = (minOf(span.to, today) - span.from + 1).toInt().coerceAtLeast(1)
    val perDay = minutes / lived
    val avg = if (count > 0) minutes / count else 0
    val longest = plays.maxByOrNull { it.minutes }
    /** The span before, as far into it as this one has got. */
    val prev = Span(span.from - span.days, span.from - 1 - (span.days - lived).coerceAtLeast(0), "")
    val minutesBefore = all.filter { prev.has(it.day) }.sumOf { it.minutes }
    val gamesPlayed = plays.map { it.gameId }.toSet().size
    val fresh = plays.map { it.gameId }.toSet().count { id -> all.none { it.gameId == id && it.day < span.from } }
    val finished = games.values.count { it.finished > 0 && span.has(it.finished) }
    val shots = plays.sumOf { it.shotList.size }
    val wins = plays.count { it.result == Play.WIN }
    val losses = plays.count { it.result == Play.LOSS }
    val coop = plays.count { it.result == Play.COOP_WIN }
    val social = plays.count { it.friends.isNotBlank() }
    val moods = plays.filter { it.mood > 0 }.map { it.mood }
    val mood = if (moods.isEmpty()) 0.0 else moods.average()

    private fun <T> sums(groups: Map<T, List<Play>>): List<Sum<T>> { val tot = maxOf(1, minutes)
        return groups.map { (k, l) -> val m = l.sumOf { it.minutes }; Sum(k, m, l.size, m.toFloat() / tot) }.sortedByDescending { it.minutes } }

    fun byGame(): List<Sum<Game?>> = sums(plays.groupBy { games[it.gameId] })
    fun byKind(): List<Sum<String>> = sums(plays.groupBy { games[it.gameId]?.kind ?: Game.OTHER })
    fun byPlatform(): List<Sum<String>> = sums(plays.filter { it.platform.isNotBlank() }.groupBy { it.platform })
    /** A session counts in full for each genre of its game, so the shares may add up to more than one. */
    fun byGenre(): List<Sum<Genre>> { val m = HashMap<Genre, MutableList<Play>>()
        plays.forEach { p -> games[p.gameId]?.genreList?.mapNotNull { Genres.of(it) }?.forEach { g -> m.getOrPut(g) { ArrayList() } += p } }
        return sums(m) }
    /** Minutes by stat, through the genres: the shape of the player. */
    fun byStat(): List<Pair<Stat, Int>> { val m = IntArray(Stat.entries.size)
        plays.forEach { p -> val g = games[p.gameId]; m[Engine.statOf(g, p).ordinal] += p.minutes }
        return Stat.entries.map { it to m[it.ordinal] } }

    fun byWeekday(): List<Int> = (1..7).map { wd -> plays.filter { LocalDate.ofEpochDay(it.day).dayOfWeek.value == wd }.sumOf { it.minutes } }
    fun byHour(): List<Int> = (0 until 24).map { h -> plays.filter { hourOf(it.start) == h }.sumOf { it.minutes } }
    fun byDay(): Map<Long, Int> = plays.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.minutes } }

    /** Bars over the span: days for a week or month, weeks for a season, months for a year or all time. */
    fun series(): List<Pair<String, Double>> = when {
        span.days <= 7 -> (0 until 7).map { i -> dayShort(i) to (byDay()[span.from + i] ?: 0) / 60.0 }
        span.days <= 31 -> (0 until span.days).map { i -> (if (i % 7 == 0) "${LocalDate.ofEpochDay(span.from + i).dayOfMonth}" else "") to (byDay()[span.from + i] ?: 0) / 60.0 }
        span.days <= 100 -> { val m0 = weekStart(span.from); val n = ((span.to - m0) / 7 + 1).toInt()
            (0 until n).map { w -> val a = m0 + w * 7; "${LocalDate.ofEpochDay(a).dayOfMonth}" to plays.filter { it.day in a..(a + 6) }.sumOf { it.minutes } / 60.0 } }
        else -> { val a = LocalDate.ofEpochDay(span.from).withDayOfMonth(1); val b = LocalDate.ofEpochDay(minOf(span.to, today)).withDayOfMonth(1)
            val out = ArrayList<Pair<String, Double>>(); var m = a
            while (!m.isAfter(b) && out.size < 60) { val f = m.toEpochDay(); val t = m.plusMonths(1).toEpochDay() - 1
                out += monthShort(m.monthValue) to plays.filter { it.day in f..t }.sumOf { it.minutes } / 60.0; m = m.plusMonths(1) }
            out.takeLast(24) }
    }
}

/** How things stand with one friend. */
class With(val friend: Friend, plays: List<Play>, seats: List<Seat>, games: Map<Long, Game>) {
    val plays = plays.filter { friend.id in it.friendIds }
    val minutes = this.plays.sumOf { it.minutes }
    val won: Int; val lost: Int
    val coop = this.plays.count { it.result == Play.COOP_WIN }
    val top: Game? = this.plays.groupBy { it.gameId }.maxByOrNull { e -> e.value.sumOf { it.minutes } }?.key?.let { games[it] }
    val last = this.plays.maxOfOrNull { it.day } ?: 0
    init {
        var w = 0; var l = 0; val ids = this.plays.map { it.id }.toSet()
        seats.filter { it.playId in ids }.groupBy { it.playId }.values.forEach { t ->
            val me = t.firstOrNull { it.friendId == 0L } ?: return@forEach; val he = t.firstOrNull { it.friendId == friend.id } ?: return@forEach
            if (me.won && !he.won) w++ else if (he.won && !me.won) l++ }
        won = w; lost = l
    }
}
