package app.questling.data

import app.questling.AppContainer
import app.questling.domain.Lang
import app.questling.domain.Shop
import app.questling.domain.today
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Half a year of somebody who plays a bit of everything, lived through the real engine day by day (the clock is moved),
 * so creatures, gold, quests, trophies and the season are what they would be. Demo builds only.
 */
object DemoSeed {
    /**
     * Real covers for the demo's games, from the catalogue (Wikidata and Wikipedia): the seed only knows the titles.
     * Runs after the seed, a game at a time; games that have a cover already are left alone, so it is cheap to call again.
     */
    suspend fun fetchCovers(c: AppContainer) {
        val ru = Lang.ru
        for (g in c.dao.gameList().filter { it.cover.isBlank() }) {
            // the article is known for the demo's own games; anything else is looked up
            val url = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { WIKI.entries.firstOrNull { g.title.startsWith(it.key) }?.let { Catalog.poster(it.value, "en") } }
                ?: Catalog.search(g.title.substringBefore(':'), ru)?.let { found -> (found.firstOrNull { it.kind == g.kind && it.coverUrl != null } ?: found.firstOrNull { it.coverUrl != null })?.coverUrl }
                ?: continue                                                                    // this one failed: the next launch tries again
            val name = Catalog.download(url)?.let { Media.saveCover(c.app, it) } ?: continue
            c.dao.gameList().firstOrNull { it.id == g.id }?.let { c.dao.update(it.copy(cover = name)) }
        }
    }

    /** English Wikipedia articles of the demo's games, by the start of the title in either language. */
    private val WIKI = mapOf(
        "Hades" to "Hades (video game)", "Stardew Valley" to "Stardew Valley", "Ведьмак 3" to "The Witcher 3: Wild Hunt", "The Witcher 3" to "The Witcher 3: Wild Hunt",
        "Hollow Knight" to "Hollow Knight", "Disco Elysium" to "Disco Elysium", "Civilization VI" to "Civilization VI", "Resident Evil 4" to "Resident Evil 4 (2023 video game)",
        "Tetris Effect" to "Tetris Effect", "Forza Horizon 5" to "Forza Horizon 5", "Monument Valley" to "Monument Valley (video game)",
        "Каркассон" to "Carcassonne (board game)", "Carcassonne" to "Carcassonne (board game)", "Крылья" to "Wingspan (board game)", "Wingspan" to "Wingspan (board game)",
        "Пандемия" to "Pandemic (board game)", "Pandemic" to "Pandemic (board game)", "Кодовые имена" to "Codenames (board game)", "Codenames" to "Codenames (board game)",
        "Колонизаторы" to "Catan", "Catan" to "Catan", "Манчкин" to "Munchkin (card game)", "Munchkin" to "Munchkin (card game)",
        "Dungeons & Dragons" to "Curse of Strahd", "Elden Ring" to "Elden Ring", "Серп" to "Scythe (board game)", "Scythe" to "Scythe (board game)", "Celeste" to "Celeste (video game)",
    )

    private class G(val ru: String, val en: String, val kind: String, val platform: String, val genres: String, val year: Int, val maker: String, val weight: Int, val from: Int = 0, val until: Int = 999)

    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.settings.now().onboarded || c.dao.playList().isNotEmpty()) return
        val ru = Lang.ru
        fun n(r: String, e: String) = if (ru) r else e
        val t0 = today()
        val days = 175
        val start = t0 - days
        val r = Random(7)
        c.quiet = true
        c.clock = { start }
        c.begin(n("Марат", "Sam"), setOf(Game.VIDEO, Game.BOARD, Game.CARD, Game.RPG, Game.MOBILE), listOf("PC", "Switch", "PlayStation"))
        // the screens wait until the whole history is in
        c.settings.update { it.copy(onboarded = false) }
        val list = listOf(
            G("Hades", "Hades", Game.VIDEO, "PC", "roguelike,action", 2020, "Supergiant Games", 9, 0, 95),
            G("Stardew Valley", "Stardew Valley", Game.VIDEO, "Switch", "sim,casual", 2016, "ConcernedApe", 7),
            G("Ведьмак 3: Дикая Охота", "The Witcher 3: Wild Hunt", Game.VIDEO, "PC", "rpg,adventure", 2015, "CD Projekt Red", 10, 30),
            G("Hollow Knight", "Hollow Knight", Game.VIDEO, "Switch", "platformer,adventure", 2017, "Team Cherry", 6, 0, 120),
            G("Disco Elysium", "Disco Elysium", Game.VIDEO, "PC", "story,rpg", 2019, "ZA/UM", 6, 10, 60),
            G("Civilization VI", "Civilization VI", Game.VIDEO, "PC", "strategy", 2016, "Firaxis", 4, 50),
            G("Resident Evil 4", "Resident Evil 4", Game.VIDEO, "PlayStation", "horror,action", 2023, "Capcom", 4, 100),
            G("Tetris Effect", "Tetris Effect", Game.VIDEO, "PlayStation", "puzzle,arcade", 2018, "Monstars", 3),
            G("Forza Horizon 5", "Forza Horizon 5", Game.VIDEO, "PC", "racing", 2021, "Playground Games", 3, 70),
            G("Monument Valley", "Monument Valley", Game.MOBILE, "Android", "puzzle", 2014, "ustwo", 3, 20, 80),
            G("Каркассон", "Carcassonne", Game.BOARD, "", "euro,family", 2000, "Hans im Glück", 5),
            G("Крылья", "Wingspan", Game.BOARD, "", "euro", 2019, "Stonemaier Games", 5, 25),
            G("Пандемия", "Pandemic", Game.BOARD, "", "coop", 2008, "Z-Man Games", 4, 10),
            G("Кодовые имена", "Codenames", Game.BOARD, "", "party,detective", 2015, "CGE", 3),
            G("Колонизаторы", "Catan", Game.BOARD, "", "euro,family", 1995, "Kosmos", 3, 40),
            G("Манчкин", "Munchkin", Game.CARD, "", "deck,party", 2001, "Steve Jackson Games", 3),
            G("Dungeons & Dragons: Проклятие Страда", "Dungeons & Dragons: Curse of Strahd", Game.RPG, "", "trpg,horror", 2016, "Wizards of the Coast", 4, 35),
        )
        val ids = HashMap<G, Long>()
        list.forEach { g -> ids[g] = c.saveGame(Game(title = n(g.ru, g.en), kind = g.kind, platform = g.platform, genres = g.genres, year = g.year, maker = g.maker,
            status = if (g.from == 0) Game.PLAYING else Game.SHELF, added = start + (g.from - 12).coerceAtLeast(0))) }
        // on the shelf and wished for
        c.saveGame(Game(title = "Elden Ring", kind = Game.VIDEO, platform = "PC", genres = "action,rpg", year = 2022, maker = "FromSoftware", status = Game.SHELF, added = start + 60))
        c.saveGame(Game(title = n("Серп", "Scythe"), kind = Game.BOARD, genres = "euro,wargame", year = 2016, maker = "Stonemaier Games", status = Game.WISH, added = start + 90))
        c.saveGame(Game(title = "Celeste", kind = Game.VIDEO, platform = "Switch", genres = "platformer", year = 2018, maker = "Maddy Makes Games", status = Game.DROPPED, added = start + 5))
        val friends = listOf(n("Аня", "Anna"), n("Миша", "Mike"), n("Оля", "Olga"), n("Дима", "Dan")).map { c.saveFriend(Friend(name = it, created = start)) }

        val notes = mapOf(
            "Hades" to n("Дошёл до Аида с копьём и благословениями Афины. Ещё чуть-чуть!|Наконец-то победил. Заг, ты свободен. Ненадолго.|Попробовал щит – совсем другая игра.",
                "Reached Hades with the spear and Athena's boons. So close!|Finally won. Zag, you're free. For a while.|Tried the shield – a whole different game."),
            "Stardew Valley" to n("Первый урожай клубники, починил мост на пляж.|Осень, тыквы и ярмарка. Подарил Лие салат.", "First strawberry harvest, fixed the bridge to the beach.|Autumn, pumpkins and the fair. Gave Leah a salad."),
            "witcher" to n("Кровавый барон. Это не побочный квест, это отдельный роман.|Приплыл на Скеллиге. Музыка!|Гвинт затянул сильнее основной истории.",
                "The Bloody Baron. Not a side quest – a novel of its own.|Sailed to Skellige. The music!|Gwent pulled me in harder than the main story."),
            "board" to n("Аня перекрыла мне город в последний ход.|Сыграли две партии подряд, вторая – на реванш.|Долго считали очки: всё решила последняя карта.",
                "Anna blocked my city on the very last turn.|Two games in a row, the second for revenge.|A long count at the end: the last card decided it all."),
            "rpg" to n("Добрались до Баровии. Туман, вороны, пирожки подозрительного вкуса.|Ужин у Страда. Никто не умер, но плут потерял тень.", "Reached Barovia. Fog, ravens and suspicious pastries.|Dinner at Strahd's. Nobody died, but the rogue lost his shadow."),
        )
        fun note(key: String) = notes[key]?.split('|')?.random(r).orEmpty()
        val zone = ZoneId.systemDefault()

        for (i in 0..days) {
            val day = start + i
            c.clock = { day }
            c.tick()
            val wd = LocalDate.ofEpochDay(day).dayOfWeek.value
            // a break in the middle: a holiday without games, and a quiet week
            if (i in 62..70 || (i in 130..133)) continue
            val sessions = when { wd >= 6 -> listOf(0, 1, 1, 2, 2, 3).random(r); else -> listOf(0, 0, 1, 1, 1, 2).random(r) }
            var hour = if (wd >= 6) 11 + r.nextInt(3) else 19 + r.nextInt(2)
            repeat(sessions) { k ->
                val table = (wd == 5 || wd == 6) && k == sessions - 1 && r.nextInt(10) < 7
                val pool = list.filter { i in it.from..it.until && (if (table) it.kind != Game.VIDEO && it.kind != Game.MOBILE else it.kind == Game.VIDEO || it.kind == Game.MOBILE) }
                if (pool.isEmpty()) return@repeat
                val g = pool.flatMap { x -> List(x.weight) { x } }.random(r)
                val gid = ids[g]!!
                val minutes = when {
                    g.kind == Game.RPG -> 150 + r.nextInt(90)
                    table -> 40 + r.nextInt(70)
                    g.kind == Game.MOBILE -> 8 + r.nextInt(18)
                    r.nextInt(6) == 0 -> 12 + r.nextInt(10)
                    else -> 35 + r.nextInt(100)
                }
                val late = !table && r.nextInt(9) == 0
                val startMs = LocalDate.ofEpochDay(day).atStartOfDay(zone).toInstant().toEpochMilli() + ((if (late) 23 else hour) * 60 + r.nextInt(50)) * 60_000L
                hour = (hour + 2).coerceAtMost(22)
                val who = if (table) friends.shuffled(r).take(if (g.kind == Game.RPG) 3 else 1 + r.nextInt(3)) else if (g.en.startsWith("Forza") || g.en.startsWith("Tetris")) friends.shuffled(r).take(r.nextInt(2)) else emptyList()
                val coop = g.genres.contains("coop") || g.kind == Game.RPG
                val result = when { who.isEmpty() -> Play.NONE; coop -> if (r.nextInt(3) > 0) Play.COOP_WIN else Play.COOP_LOSS; else -> if (r.nextInt(5) < 2) Play.WIN else if (r.nextInt(12) == 0) Play.DRAW else Play.LOSS }
                val scored = table && !coop && g.genres.contains("euro")
                val mine = if (scored) 40 + r.nextInt(60) else Int.MIN_VALUE
                val seats = if (who.isEmpty() || coop && !scored) emptyList() else {
                    val winner = if (result == Play.LOSS) who.random(r) else -1L
                    (listOf(0L) + who).map { id -> val sc = if (!scored) Int.MIN_VALUE else if (id == 0L) mine else if (id == winner) mine + 1 + r.nextInt(20) else (mine - 1 - r.nextInt(25)).coerceAtLeast(5).let { if (result == Play.LOSS) it else minOf(it, mine - 1) }
                        Seat(playId = 0, friendId = id, score = sc, won = if (result == Play.WIN) id == 0L else id == winner) }
                        .let { l -> val order = l.sortedByDescending { it.score }.map { it.friendId }; l.map { it.copy(place = if (scored) order.indexOf(it.friendId) + 1 else 0) } }
                }
                val moment = when {
                    g.en == "Hades" && i == 88 -> Play.M_FINISH
                    g.en == "Disco Elysium" && i >= 58 -> Play.M_FINISH
                    g.en == "Hollow Knight" && i >= 116 -> Play.M_HUNDRED
                    g.en == "Monument Valley" && i >= 78 -> Play.M_FINISH
                    (g.genres.contains("action") || g.genres.contains("rpg")) && r.nextInt(7) == 0 -> Play.M_BOSS
                    g.genres.contains("story") && r.nextInt(4) == 0 -> Play.M_CHAPTER
                    else -> ""
                }
                val key = when { g.en == "Hades" || g.en == "Stardew Valley" -> g.en; g.en.startsWith("The Witcher") -> "witcher"; g.kind == Game.RPG -> "rpg"; table -> "board"; else -> "" }
                c.now = { startMs + minutes * 60_000L }
                c.addPlay(Play(gameId = gid, day = day, start = startMs, minutes = minutes, platform = if (table) "table" else g.platform, result = result, score = mine,
                    friends = who.joinToString(","), mood = if (r.nextInt(3) > 0) 3 + r.nextInt(3) else 0, note = if (key.isNotEmpty() && r.nextInt(3) == 0) note(key) else "",
                    breaks = if (minutes >= 90 && r.nextInt(2) == 0) 1 + r.nextInt(2) else 0, moment = moment, auto = g.kind == Game.MOBILE && r.nextInt(2) == 0,
                    tools = if (table) r.nextInt(4) else 0), seats)
                if (table && r.nextInt(2) == 0) { c.tool("dice", 1 + r.nextInt(6)); c.tool("first"); if (scored) c.tool("score") }
            }
            // the tavern gets furnished as the gold comes
            val s = c.settings.now()
            Shop.all.firstOrNull { it.id !in s.items }?.let { if (s.gold >= it.price + 150) c.buy(it.id) }
        }
        // ratings and notes on the games
        val games = c.dao.gameList().associateBy { it.title }
        fun rate(ru0: String, en0: String, rating: Int, text: String = "") = games[n(ru0, en0)]?.copy(rating = rating, note = text)
        listOfNotNull(rate("Hades", "Hades", 10, n("Любимое оружие – копьё. Не забыть: Зеркало ночи перенастраивается.", "Favourite weapon: the spear. Remember the Mirror of Night can be respecced.")),
            rate("Disco Elysium", "Disco Elysium", 10), rate("Hollow Knight", "Hollow Knight", 9), rate("Ведьмак 3: Дикая Охота", "The Witcher 3: Wild Hunt", 9, n("Остановился в Новиграде, квест с Трисс.", "Stopped in Novigrad, Triss's quest.")),
            rate("Stardew Valley", "Stardew Valley", 8), rate("Крылья", "Wingspan", 9), rate("Каркассон", "Carcassonne", 8), rate("Пандемия", "Pandemic", 8), rate("Civilization VI", "Civilization VI", 7),
            rate("Манчкин", "Munchkin", 6)).forEach { c.saveGame(c.dao.gameList().first { g -> g.id == it.id }.copy(rating = it.rating, note = it.note)) }
        c.clock = { today() }
        c.now = { System.currentTimeMillis() }
        c.tick()
        c.quiet = false
        val pets = c.dao.petList().filter { it.hatched > 0 }.sortedByDescending { it.xp }
        c.settings.update { it.copy(onboarded = true, inbox = emptyList(), title = n("Завсегдатай", "Regular").takeIf { _ -> app.questling.domain.Season.level(it.seasonXp) >= 10 || it.seasons.values.any { v -> v >= 10 } }.orEmpty(),
            town = n("Москва", "London"), lat = if (ru) 55.75 else 51.5, lon = if (ru) 37.62 else -0.12, guessed = true, tracker = false,
            found = listOf(Found("com.demo.puzzle", "Monument Valley 2", System.currentTimeMillis() - 3 * 3600_000L, 24))) }
        // a full house by the fire
        pets.take(3).forEachIndexed { i, p -> if (p.slot != i) c.setSlot(c.dao.petList().first { it.id == p.id }, i) }
    }
}
