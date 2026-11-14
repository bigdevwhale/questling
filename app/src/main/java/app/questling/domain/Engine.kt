package app.questling.domain

import app.questling.data.Friend
import app.questling.data.Game
import app.questling.data.Pet
import app.questling.data.Play
import app.questling.data.Seat
import app.questling.data.Settings
import java.time.LocalDate

/**
 * Everything counted once from the whole diary: what trophies, finds and quests are judged on.
 * [plays] may come in any order.
 */
class Facts(val s: Settings, val games: List<Game>, plays: List<Play>, val seats: List<Seat>, val friends: List<Friend>, val pets: List<Pet>, val today: Long) {
    val plays = plays.sortedBy { it.start }
    val gameById = games.associateBy { it.id }
    fun game(p: Play) = gameById[p.gameId]
    fun kind(p: Play) = game(p)?.kind ?: Game.OTHER

    val n get() = plays.size
    val minutes = plays.sumOf { it.minutes }
    val hours get() = minutes / 60
    val days: Set<Long> = plays.map { it.day }.toSet()
    val byDay = this.plays.groupBy { it.day }
    val byGame = this.plays.groupBy { it.gameId }
    val gameMin = byGame.mapValues { e -> e.value.sumOf { it.minutes } }
    val played get() = byGame.size
    val finished = games.count { it.status == Game.FINISHED || it.status == Game.HUNDRED }
    val hundred = games.count { it.status == Game.HUNDRED }

    /** Minutes by genre: a session counts in full for every genre of its game. */
    val genreMin: Map<String, Int> = HashMap<String, Int>().also { m -> this.plays.forEach { p -> game(p)?.genreList?.forEach { g -> m[g] = (m[g] ?: 0) + p.minutes } } }
    val genres get() = genreMin.keys.size
    val kindMin: Map<String, Int> = this.plays.groupBy { kind(it) }.mapValues { e -> e.value.sumOf { it.minutes } }
    val kindCount: Map<String, Int> = this.plays.groupBy { kind(it) }.mapValues { it.value.size }
    val platforms: Set<String> = this.plays.map { it.platform }.filter { it.isNotBlank() }.toSet()

    val wins = this.plays.count { it.result == Play.WIN }
    val coopWins = this.plays.count { it.result == Play.COOP_WIN }
    val losses = this.plays.count { it.result == Play.LOSS }
    val draws = this.plays.count { it.result == Play.DRAW }
    private val decided = this.plays.filter { it.result == Play.WIN || it.result == Play.LOSS }
    val winStreak = runOf(decided) { it.result == Play.WIN }
    val lossStreak = runOf(decided) { it.result == Play.LOSS }
    /** Three losses in a row and a session after them. */
    val roseAgain: Boolean = decided.windowed(3).any { w -> w.all { it.result == Play.LOSS } && this.plays.any { it.start > w.last().start } }

    val social = this.plays.count { it.friends.isNotBlank() }
    val duels = this.plays.count { it.friendIds.size == 1 && (it.result == Play.WIN || it.result == Play.LOSS || it.result == Play.DRAW) }
    val maxTable = this.plays.maxOfOrNull { it.friendIds.size + 1 } ?: 0
    val shots = this.plays.sumOf { it.shotList.size }
    val maxShots = this.plays.maxOfOrNull { it.shotList.size } ?: 0
    val notes = this.plays.count { it.note.isNotBlank() }
    val longNote = this.plays.any { it.note.length >= 200 }
    val rated = games.count { it.rating > 0 }
    val night = this.plays.count { hourOf(it.start).let { h -> h >= 23 || h < 4 } }
    val early = this.plays.count { hourOf(it.start) in 4..6 }
    val oneMoreTurn = this.plays.any { p -> val end = p.start + p.minutes * 60_000L; hourOf(end) in 3..5 && hourOf(p.start).let { it >= 20 || it < 3 } }
    val short = this.plays.count { it.minutes in 1..20 }
    val breaks = this.plays.sumOf { it.breaks }
    val longest = this.plays.maxOfOrNull { it.minutes } ?: 0
    val wiseMarathon = this.plays.any { it.minutes >= 180 && it.breaks >= 2 }
    val phone = this.plays.count { kind(it) == Game.MOBILE || it.platform == "Android" || it.platform == "iOS" }

    /** The longest run of days in a row with a session. */
    val dayStreak: Int = streak(days.sorted(), 1)
    val weekStreak: Int = streak(days.map { weekStart(it) }.distinct().sorted(), 7)
    /** The run ending today or yesterday. */
    val streakNow: Int = run { var d = if (today in days) today else today - 1; var k = 0; while (d in days) { k++; d-- }; k }
    val weekends: Int = streak(days.filter { LocalDate.ofEpochDay(it).dayOfWeek.value >= 6 }.map { weekStart(it) }.distinct().sorted(), 7)

    /** The longest pause before coming back to the same game, in days. */
    val gameGap: Long = byGame.values.maxOfOrNull { l -> l.map { it.day }.distinct().zipWithNext().maxOfOrNull { (a, b) -> b - a } ?: 0 } ?: 0
    val anyGap: Long = days.sorted().zipWithNext().maxOfOrNull { (a, b) -> b - a } ?: 0
    val maxGameMin = gameMin.values.maxOrNull() ?: 0
    val maxGameDays = byGame.values.maxOfOrNull { l -> l.map { it.day }.toSet().size } ?: 0
    val gameYears = byGame.values.maxOfOrNull { l -> l.map { LocalDate.ofEpochDay(it.day).year }.toSet().size } ?: 0
    val onceOnly = byGame.values.count { it.size == 1 }
    val dayPlatforms = byDay.values.maxOfOrNull { l -> l.map { it.platform }.filter { it.isNotBlank() }.toSet().size } ?: 0
    val dayGames = byDay.values.maxOfOrNull { l -> l.map { it.gameId }.toSet().size } ?: 0
    val weekGenres = this.plays.groupBy { weekStart(it.day) }.values.maxOfOrNull { w -> w.flatMap { game(it)?.genreList.orEmpty() }.toSet().size } ?: 0
    val dayBoardWins = byDay.values.maxOfOrNull { l -> l.count { it.result == Play.WIN && Kinds.table(kind(it)) } } ?: 0
    val records = this.plays.count { it.moment == Play.M_RECORD }
    val bosses = this.plays.count { it.moment == Play.M_BOSS }
    val bigScore = this.plays.any { it.hasScore && it.score >= 100 }

    /** Finished within a week of the first session. */
    val quickFinish = games.any { g -> g.finished > 0 && byGame[g.id]?.minOfOrNull { it.day }?.let { g.finished - it <= 7 } == true }
    /** Finished after a year on the shelf. */
    val shelfKill = games.any { g -> g.finished > 0 && g.finished - g.added >= 365 }
    val retro = this.plays.any { (game(it)?.year ?: 0) in 1..1995 }
    val fresh = this.plays.any { p -> game(p)?.year == LocalDate.ofEpochDay(p.day).year }
    val newYear = days.any { LocalDate.ofEpochDay(it).let { d -> d.monthValue == 1 && d.dayOfMonth == 1 } }
    val halloween = this.plays.any { p -> LocalDate.ofEpochDay(p.day).let { d -> d.monthValue == 10 && d.dayOfMonth == 31 } && game(p)?.genreList?.contains("horror") == true }
    val friday13 = days.any { LocalDate.ofEpochDay(it).let { d -> d.dayOfMonth == 13 && d.dayOfWeek.value == 5 } }
    val lunch = this.plays.count { it.minutes <= 30 && hourOf(it.start) in 12..13 }

    /** Wins and losses against a friend, from the seats: (won, lost). */
    fun versus(friendId: Long): Pair<Int, Int> {
        var w = 0; var l = 0
        seats.groupBy { it.playId }.values.forEach { t ->
            val me = t.firstOrNull { it.friendId == 0L } ?: return@forEach; val he = t.firstOrNull { it.friendId == friendId } ?: return@forEach
            if (me.won && !he.won) w++ else if (he.won && !me.won) l++
        }
        return w to l
    }
    private val seatsByPlay = seats.groupBy { it.playId }
    /** The most losses in a row to one and the same friend, and whether a win followed them. */
    val nemesis: Int; val revenge: Boolean
    init {
        var worst = 0; var rev = false
        friends.forEach { f ->
            var runL = 0
            this.plays.forEach { p ->
                val t = seatsByPlay[p.id] ?: return@forEach
                val me = t.firstOrNull { it.friendId == 0L } ?: return@forEach; val he = t.firstOrNull { it.friendId == f.id } ?: return@forEach
                if (he.won && !me.won) { runL++; worst = maxOf(worst, runL) } else if (me.won && !he.won) { if (runL >= 1) rev = true; runL = 0 }
            }
        }
        nemesis = worst; revenge = rev
    }

    val hatched = pets.count { it.hatched > 0 }
    val adults = pets.count { it.stage() >= Stage.ADULT }
    val legends = pets.count { it.stage() == Stage.MASTER }
    val shiny = pets.any { it.shiny && it.hatched > 0 }
    val trophies get() = s.trophies.size
    fun tool(id: String) = s.tools[id] ?: 0

    companion object {
        private fun runOf(l: List<Play>, ok: (Play) -> Boolean): Int { var best = 0; var cur = 0; l.forEach { if (ok(it)) { cur++; best = maxOf(best, cur) } else cur = 0 }; return best }
        private fun streak(sorted: List<Long>, step: Long): Int { var best = 0; var cur = 0; var prev = Long.MIN_VALUE
            sorted.forEach { d -> cur = if (d - prev == step) cur + 1 else 1; best = maxOf(best, cur); prev = d }; return best }
    }
}

/**
 * The rules of the tavern. Gold and growth come from playing with pleasure – sessions, friends, new games, breaks –
 * and the hours past three a day count for less: nobody is rewarded for a binge.
 */
object Engine {
    const val DAILY_CAP = 60            // gold for minutes a day
    const val FIRST_OF_DAY = 3
    const val NEW_GAME = 5
    const val FRIENDS = 4
    const val WIN = 3
    const val BREAK = 2
    const val NOTE = 1
    const val SHOTS = 1
    const val FINISH = 30
    const val HUNDRED = 50

    fun team(pets: List<Pet>) = pets.filter { it.slot in 0..2 && it.hatched > 0 }.sortedBy { it.slot }

    /** Minutes that feed the creatures: the first three hours of a day in full, the next three by half, the rest by a quarter. */
    fun food(minutes: Int, before: Int): Int {
        var left = minutes; var at = before; var out = 0.0
        while (left > 0) {
            val (room, k) = when { at < 180 -> (180 - at) to 1.0; at < 360 -> (360 - at) to .5; else -> Int.MAX_VALUE to .25 }
            val take = minOf(left, room); out += take * k; left -= take; at += take
        }
        return out.toInt().coerceAtLeast(if (minutes > 0) 1 else 0)
    }

    /** Gold for an act, doubled when a creature with the matching perk is in the tavern; the legendary ones add a quarter. */
    fun bonus(base: Int, perks: Set<Perk>, pets: List<Pet>, genres: List<String> = emptyList()): Int {
        val t = team(pets).map { Pets.of(it.species) }
        var v = base
        val hit = t.firstOrNull { sp -> sp.perk in perks && (sp.perk != Perk.GENRE || sp.likes.any { it in genres }) }
        if (hit != null) v *= if (hit.perk == Perk.COMEBACK || hit.perk == Perk.NEW) 3 else 2
        if (t.any { it.perk == Perk.ALL }) v = (v * 1.25).toInt().coerceAtLeast(v + 1)
        return v
    }

    data class Fed(val pets: List<Pet>, val hatched: List<Pet>)

    /**
     * Play feeds the tavern: the leader's belly gets the food, the two by the fire half; every egg gets warmer.
     * The bellies turn into growth at night ([digest]). [stat] rises for the whole team; [genres] are remembered.
     */
    fun feed(pets: List<Pet>, food: Int, genres: List<String>, stat: Stat?, today: Long, statBy: Int = 1, warm: Int = 1): Fed {
        val hatched = ArrayList<Pet>()
        val out = pets.map { p ->
            if (p.hatched == 0L) {
                val g = p.growth + warm
                if (g >= Pets.HATCH) p.copy(growth = Pets.HATCH, hatched = today).also { hatched += it } else p.copy(growth = g)
            } else if (p.slot in 0..2) {
                val share = if (p.slot == 0) food else (food + 1) / 2
                var q = p.copy(belly = p.belly + share)
                if (share > 0) genres.forEach { q = q.withGenre(it, share) }
                if (stat != null) q = q.bump(stat, statBy)
                q
            } else p
        }
        return Fed(out, hatched)
    }

    fun Pet.bump(s: Stat, n: Int): Pet = when (s) {
        Stat.STR -> copy(str = str + n); Stat.WIT -> copy(wit = wit + n); Stat.DEX -> copy(dex = dex + n)
        Stat.CHA -> copy(cha = cha + n); Stat.WIL -> copy(wil = wil + n); Stat.LCK -> copy(lck = lck + n)
    }

    /** XP straight away for the team (a break, a quest, a trophy) and its stat. */
    fun cheer(pets: List<Pet>, xp: Int, stat: Stat? = null, also: String = ""): List<Pet> {
        val team = team(pets).map { it.id }.toSet()
        return pets.map { p -> if (p.hatched > 0 && (p.id in team || p.species == also)) (if (stat != null) p.bump(stat, 1) else p).copy(xp = p.xp + xp) else p }
    }

    /** Night: what was eaten becomes growth. Returns the pets and who moved to a new stage. */
    fun digest(pets: List<Pet>): Pair<List<Pet>, List<Pair<Pet, Stage>>> {
        val grown = ArrayList<Pair<Pet, Stage>>()
        val out = pets.map { p ->
            if (p.belly <= 0 || p.hatched == 0L) p else {
                val before = p.stage(); val q = p.copy(xp = p.xp + p.belly, belly = 0)
                if (q.stage() != before) grown += q to q.stage()
                q
            }
        }
        return out to grown
    }

    /** The stat a session trains: the game's first genre's, charm when there is company. */
    fun statOf(g: Game?, p: Play): Stat = when {
        g == null -> Stat.WIL
        g.genreList.isNotEmpty() -> Genres.of(g.genreList[0])?.stat ?: Stat.WIL
        p.friends.isNotBlank() -> Stat.CHA
        Kinds.table(g.kind) -> Stat.WIT
        else -> Stat.WIL
    }

    /** Creatures found by how one plays, not yet owned. */
    fun finds(f: Facts, owned: Set<String>): List<String> {
        val out = ArrayList<String>()
        fun add(id: String, ok: Boolean) { if (ok && id !in owned) out += id }
        fun h(vararg g: String) = g.sumOf { f.genreMin[it] ?: 0 } / 60
        add("slime", h("rpg") >= 20)
        add("mimic", f.finished >= 1)
        add("goblin", f.s.questsEver >= 10)
        add("shroom", h("platformer", "arcade") >= 8)
        add("ghost", h("horror", "detective") >= 8)
        add("meeple", (f.kindCount[Game.BOARD] ?: 0) >= 10)
        add("dicey", (f.kindCount[Game.RPG] ?: 0) >= 1 || f.tool("dice") >= 30)
        add("joker", (f.kindCount[Game.CARD] ?: 0) + f.plays.count { f.kind(it) != Game.CARD && f.game(it)?.genreList?.contains("deck") == true } >= 10)
        add("robo", h("shooter", "stealth") >= 15)
        add("dragon", f.finished >= 10)
        add("frogknight", h("action") >= 15)
        add("necrocat", f.night >= 15)
        add("starjelly", f.shots >= 40)
        add("raccoon", h("racing", "sports") >= 8)
        add("capybara", h("sim", "sandbox") >= 20)
        add("axolotl", h("puzzle") >= 8)
        add("fox", f.played >= 15)
        add("owl", h("strategy", "euro") >= 20)
        add("hamster", f.phone >= 20)
        add("penguin", f.wins >= 15)
        add("bunny", f.short >= 25)
        add("bear", f.social >= 20)
        add("cat", f.breaks >= 20)
        add("corgi", f.gameGap > 30)
        add("tortoise", f.maxGameMin >= 50 * 60)
        add("crab", f.duels >= 10)
        add("bat", h("mmo") >= 15)
        add("golem", f.maxGameDays >= 30)
        add("fairy", f.notes >= 20)
        add("skelly", f.roseAgain)
        add("octopus", f.platforms.size >= 5)
        add("bee", f.weekGenres >= 4)
        add("phoenix", f.hundred >= 1)
        add("unicorn", f.hours >= 300)
        add("coinling", f.trophies >= 50)
        return out
    }

    /** A species for an egg from the merchant or the season: one not owned yet, never a legendary one. */
    fun anyEgg(owned: Set<String>, seed: Int): String? {
        val pool = Pets.all.filter { it.id !in owned && !it.rare && it.id != "questling" }
        if (pool.isEmpty()) return null
        return pool[Rand(seed).int(pool.size)].id
    }
}
