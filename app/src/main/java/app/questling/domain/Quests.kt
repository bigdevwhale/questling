package app.questling.domain

import app.questling.data.Game
import app.questling.data.Play
import java.time.LocalDate

/** What happened this week, for the quests. */
class WeekFacts(val monday: Long, plays: List<Play>, games: List<Game>) {
    private val week = monday..(monday + 6)
    private val byId = games.associateBy { it.id }
    private val before = plays.filter { it.day < monday }
    private val mine = plays.filter { it.day in week }
    val days = mine.map { it.day }.toSet().size
    val minutes = mine.sumOf { it.minutes }
    val sessions = mine.size
    val games = mine.map { it.gameId }.toSet().size
    val genres = mine.flatMap { byId[it.gameId]?.genreList.orEmpty() }.toSet().size
    val social = mine.count { it.friends.isNotBlank() }
    val table = mine.count { Kinds.table(byId[it.gameId]?.kind ?: "") }
    val shots = mine.sumOf { it.shotList.size }
    val notes = mine.count { it.note.isNotBlank() }
    val wins = mine.count { it.result == Play.WIN || it.result == Play.COOP_WIN }
    val short = mine.count { it.minutes in 1..20 }
    val breaks = mine.sumOf { it.breaks }
    val weekend = mine.map { it.day }.toSet().count { LocalDate.ofEpochDay(it).dayOfWeek.value >= 6 }
    /** Games played this week for the first time ever. */
    val fresh = mine.map { it.gameId }.toSet().count { id -> before.none { it.gameId == id } }
    /** Games come back to after 30 days. */
    val old = mine.map { it.gameId }.toSet().count { id -> before.filter { it.gameId == id }.maxOfOrNull { it.day }?.let { monday - it >= 24 } == true }
    val moments = mine.count { it.moment.isNotBlank() }
    val moods = mine.count { it.mood > 0 }
}

data class Quest(val id: String, val ru: String, val en: String, val need: Int, val gold: Int, val only: String = "", val progress: (WeekFacts) -> Int) {
    val title get() = tr(ru, en)
    /** How far it got, for the eye: «2 ч 10 мин из 5 ч» for the hours, «3 из 5» for the rest. */
    fun shown(f: WeekFacts): String { val p = progress(f).coerceAtMost(need)
        return if (id.startsWith("hours")) tr("${dur(p * 60)} из ${need / 60} ч", "${dur(p * 60)} of ${need / 60} h") else tr("$p из $need", "$p of $need") }
}

object Quests {
    val all = listOf(
        Quest("days4", "Играть 4 дня из 7", "Play on 4 days of 7", 4, 30) { it.days },
        Quest("hours4", "Наиграть 4 часа за неделю", "Play 4 hours this week", 240, 30) { it.minutes },
        Quest("new1", "Попробовать новую игру", "Try a new game", 1, 30) { it.fresh },
        Quest("games3", "Сыграть в три разные игры", "Play three different games", 3, 25) { it.games },
        Quest("genres3", "Три разных жанра", "Three different genres", 3, 25) { it.genres },
        Quest("friend1", "Сыграть с кем-нибудь", "Play with someone", 1, 25, "friends") { it.social },
        Quest("table1", "Устроить настольный вечер", "Have a tabletop evening", 1, 30, "table") { it.table },
        Quest("shots3", "Сохранить 3 скриншота или фото", "Keep 3 screenshots or photos", 3, 20) { it.shots },
        Quest("notes2", "Написать заметки к двум сессиям", "Write notes for two sessions", 2, 20) { it.notes },
        Quest("win1", "Победить", "Win a game", 1, 20, "table") { it.wins },
        Quest("short3", "Три короткие сессии – до 20 минут", "Three short sessions – up to 20 minutes", 3, 20) { it.short },
        Quest("break2", "Сделать два перерыва во время игры", "Take two breaks while playing", 2, 20) { it.breaks },
        Quest("weekend1", "Сыграть в выходные", "Play at the weekend", 1, 15) { it.weekend },
        Quest("old1", "Вернуться к игре, в которую давно не играли", "Come back to a game you haven't played in a while", 1, 30, "old") { it.old },
        Quest("moment1", "Отметить важный момент: босса, главу или титры", "Mark a moment: a boss, a chapter or the credits", 1, 25) { it.moments },
        Quest("mood3", "Отметить настроение после трёх сессий", "Mark your mood after three sessions", 3, 15) { it.moods },
        Quest("sessions5", "Сыграть 5 сессий", "Play 5 sessions", 5, 25) { it.sessions },
    )
    val byId = all.associateBy { it.id }

    /** Three quests for a week, the same for the same week; those that need friends, a table or old games only when they make sense. */
    fun deal(monday: Long, friends: Boolean, table: Boolean, old: Boolean): List<String> {
        val r = Rand(monday.toInt() * 7919)
        val pool = all.filter { when (it.only) { "friends" -> friends; "table" -> table; "old" -> old; else -> true } }.toMutableList()
        return (0 until 3).map { pool.removeAt(r.int(pool.size)).id }
    }
}

/** Furniture for the tavern, bought with gold. Placed by [x], [y] (parts of the picture) and drawn [w] wide. */
data class Item(val id: String, val ru: String, val en: String, val price: Int, val x: Float, val y: Float, val w: Float, val wall: Boolean) {
    val title get() = tr(ru, en)
}

object Shop {
    val all = listOf(
        Item("banner", "Знамя гильдии", "Guild banner", 200, .648f, .35f, .05f, true),
        Item("darts", "Мишень для дартса", "Dartboard", 240, .745f, .79f, .06f, false),
        Item("barrels", "Бочки с элем", "Ale barrels", 300, .955f, .84f, .075f, false),
        Item("board", "Доска объявлений", "Notice board", 360, .385f, .37f, .1f, true),
        Item("beanbag", "Кресло-мешок", "Bean bag", 420, .165f, .95f, .10f, false),
        Item("armchair", "Кресло с пледом", "Armchair with a blanket", 480, .835f, .90f, .11f, false),
        Item("trophies", "Полка с кубками", "Trophy shelf", 540, .52f, .37f, .1f, true),
        Item("shelf", "Шкаф с настолками", "Board game shelf", 620, .325f, .705f, .085f, false),
        Item("table", "Игровой стол", "Game table", 700, .625f, .86f, .14f, false),
        Item("tv", "Телевизор с приставкой", "TV with a console", 800, .405f, .705f, .085f, false),
        Item("jukebox", "Музыкальный автомат", "Jukebox", 900, .60f, .705f, .065f, false),
        Item("arcade", "Игровой автомат", "Arcade cabinet", 1000, .672f, .705f, .07f, false),
        Item("pinball", "Пинбол", "Pinball machine", 1100, .095f, .83f, .095f, false),
        Item("portal1", "Портал в Зелёный лес", "Portal to the Greenwood", 1300, .085f, .975f, .10f, false),
        Item("portal2", "Портал к звёздам", "Portal to the stars", 1500, .215f, .775f, .085f, false),
        Item("portal3", "Портал в Туманную лощину", "Portal to the Misty Hollow", 1700, .925f, .975f, .10f, false),
    )
    val byId = all.associateBy { it.id }
    const val EGG = 600
}

/** What a season level gives. */
sealed class Reward {
    data class Gold(val n: Int) : Reward()
    data object Egg : Reward()
    data class Title(val ru: String, val en: String, val gold: Int) : Reward()
}

/**
 * A season is a quarter of the year. Its xp comes from sessions, new games, company, quests and trophies –
 * never from money. Thirty levels, [STEP] xp each.
 */
object Season {
    const val LEVELS = 30
    const val STEP = 280
    const val SESSION = 10; const val NEW = 30; const val FRIENDS = 15; const val QUEST = 60

    fun id(day: Long): String { val d = LocalDate.ofEpochDay(day); return "${d.year}-${(d.monthValue - 1) / 3 + 1}" }
    fun level(xp: Int) = (xp / STEP).coerceAtMost(LEVELS)
    /** Xp within the level: (have, need). */
    fun within(xp: Int): Pair<Int, Int> = if (level(xp) >= LEVELS) STEP to STEP else (xp % STEP) to STEP
    fun xpFor(minutes: Int) = SESSION + (minutes / 3).coerceAtMost(50)

    fun name(id: String): String = when (id.substringAfter('-').toIntOrNull()) {
        1 -> tr("Сезон Очага", "Season of the Hearth"); 2 -> tr("Сезон Ростков", "Season of Sprouts")
        3 -> tr("Сезон Странствий", "Season of Wandering"); else -> tr("Сезон Урожая", "Season of Harvest")
    } + " " + id.substringBefore('-')

    /** Days left in the season of [day]. */
    fun left(day: Long): Long { val d = LocalDate.ofEpochDay(day); val q = (d.monthValue - 1) / 3
        return LocalDate.of(d.year, q * 3 + 1, 1).plusMonths(3).toEpochDay() - day }

    fun reward(level: Int): Reward = when {
        level == 10 -> Reward.Title("Завсегдатай", "Regular", 200)
        level == 20 -> Reward.Title("Герой сезона", "Hero of the season", 300)
        level == 30 -> Reward.Title("Легенда сезона", "Legend of the season", 500)
        level % 10 == 5 -> Reward.Egg
        else -> Reward.Gold(40 + level * 4)
    }
}
