package app.questling.domain

import app.questling.data.Game

enum class Tier(val ru: String, val en: String, val color: Long, val gold: Int, val xp: Int, val prop: String) {
    BRONZE("Бронза", "Bronze", 0xFFB8733A, 20, 20, "bronze"), SILVER("Серебро", "Silver", 0xFF9AA4B0, 50, 40, "silver"),
    GOLD("Золото", "Gold", 0xFFE0A820, 120, 80, "gold"), PLATINUM("Платина", "Platinum", 0xFF7AC0D8, 300, 150, "platinum");
    val title get() = tr(ru, en)
}

enum class TGroup(val ru: String, val en: String) {
    ROAD("Путь", "The road"), SHELF("Коллекция", "Collection"), TABLE("За столом", "At the table"), GENRE("Жанры", "Genres"),
    TAVERN("Трактир", "The tavern"), DIARY("Летопись", "The diary"), SECRET("Секретные", "Secret");
    val title get() = tr(ru, en)
}

/** An achievement: [p] is how far the player got towards [need]. [hidden] ones show «???» until earned. */
class Trophy(val id: String, val tier: Tier, val group: TGroup, val ru: String, val en: String, val dRu: String, val dEn: String, val need: Int,
             val hidden: Boolean = false, val hours: Boolean = false, val family: String = id, val famRu: String = ru, val famEn: String = en, val p: (Facts) -> Int) {
    val title get() = tr(ru, en)
    val desc get() = tr(dRu, dEn)
    /** The name of its ladder, without the step's numeral. */
    val familyTitle get() = tr(famRu, famEn)
}

/** A ladder of trophies for the same deed at growing numbers (or a single trophy on its own). */
class Family(val key: String, val steps: List<Trophy>) {
    val group get() = steps[0].group
    val title get() = steps[0].familyTitle
    val hidden get() = steps[0].hidden
    fun earned(got: Map<String, Long>) = steps.count { it.id in got }
    /** The step to go for next, or null when the whole ladder is done. */
    fun next(got: Map<String, Long>) = steps.firstOrNull { it.id !in got }
    fun top(got: Map<String, Long>) = steps.lastOrNull { it.id in got }
}

object Trophies {
    private val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    private val list = ArrayList<Trophy>()

    /** Tiers along a ladder: the first third bronze, then silver, then gold; the very last platinum when [plat]. */
    private fun tierAt(i: Int, n: Int, plat: Boolean): Tier = when {
        plat && i == n - 1 && n >= 4 -> Tier.PLATINUM
        i * 3 < n -> Tier.BRONZE
        i * 3 < n * 2 -> Tier.SILVER
        else -> Tier.GOLD
    }

    /** A ladder of the same deed at growing numbers: «Игрок I … VIII». [d] writes the description for a number. */
    private fun ladder(id: String, group: TGroup, ru: String, en: String, steps: List<Int>, dRu: (Int) -> String, dEn: (Int) -> String, plat: Boolean = true, hours: Boolean = false, p: (Facts) -> Int) {
        steps.forEachIndexed { i, n ->
            val num = if (steps.size == 1) "" else " " + ROMAN[i]
            list += Trophy("$id$n", tierAt(i, steps.size, plat), group, ru + num, en + num, dRu(n), dEn(n), n, hours = hours, family = id, famRu = ru, famEn = en, p = p)
        }
    }
    private fun one(id: String, tier: Tier, group: TGroup, ru: String, en: String, dRu: String, dEn: String, need: Int = 1, hidden: Boolean = false, p: (Facts) -> Int) {
        list += Trophy(id, tier, group, ru, en, dRu, dEn, need, hidden, p = p)
    }
    private fun b(v: Boolean) = if (v) 1 else 0
    private fun ru(n: Int, one: String, few: String, many: String) = "${fmt(n)} ${plural(n.toLong(), one, few, many)}"

    init {
        // ---------- the road ----------
        ladder("plays", TGroup.ROAD, "Игрок", "Player", listOf(1, 10, 25, 50, 100, 250, 500, 1000),
            { if (it == 1) "Записать первую сессию" else "Сыграть ${ru(it, "сессию", "сессии", "сессий")}" }, { if (it == 1) "Log your first session" else "Play ${fmt(it)} sessions" }) { it.n }
        ladder("hours", TGroup.ROAD, "Странник", "Wanderer", listOf(1, 10, 25, 50, 100, 250, 500, 1000, 2000),
            { "Наиграть ${ru(it, "час", "часа", "часов")}" }, { "Play for ${fmt(it)} ${if (it == 1) "hour" else "hours"}" }, hours = true) { it.minutes }
        ladder("streak", TGroup.ROAD, "Дни подряд", "Days in a row", listOf(3, 7, 14, 30, 60, 100),
            { "Играть ${ru(it, "день", "дня", "дней")} подряд" }, { "Play $it days in a row" }) { it.dayStreak }
        ladder("weeks", TGroup.ROAD, "Постоялец", "Regular", listOf(4, 12, 26, 52),
            { "Играть хотя бы раз в неделю ${ru(it, "неделю", "недели", "недель")} подряд" }, { "Play at least once a week for $it weeks in a row" }) { it.weekStreak }
        ladder("days", TGroup.ROAD, "Игровые дни", "Play days", listOf(10, 50, 100, 365),
            { "Играть в течение ${ru(it, "дня", "дней", "дней")}" }, { "Play on $it different days" }) { it.days.size }
        ladder("short", TGroup.ROAD, "Перекус", "Snack", listOf(5, 25, 100),
            { "${ru(it, "короткая сессия", "короткие сессии", "коротких сессий")} – до 20 минут" }, { "$it short sessions – up to 20 minutes" }, plat = false) { it.short }
        ladder("breaks", TGroup.ROAD, "Пауза", "Pause", listOf(1, 10, 50, 100),
            { if (it == 1) "Сделать перерыв во время сессии" else "Сделать ${ru(it, "перерыв", "перерыва", "перерывов")} во время сессий" }, { "Take $it ${if (it == 1) "break" else "breaks"} during sessions" }, plat = false) { it.breaks }
        ladder("yawn", TGroup.ROAD, "Знаю меру", "Know when to stop", listOf(1, 5, 20),
            { "${ru(it, "раз", "раза", "раз")} отдохнуть, когда питомец зевнул" }, { "Rest $it ${if (it == 1) "time" else "times"} when your pet yawned" }, plat = false) { it.s.yawnsHeeded }

        // ---------- the collection ----------
        ladder("games", TGroup.SHELF, "Первооткрыватель", "Discoverer", listOf(1, 5, 10, 25, 50, 100, 200),
            { if (it == 1) "Сыграть в первую игру" else "Сыграть в ${ru(it, "разную игру", "разные игры", "разных игр")}" }, { "Play $it different ${if (it == 1) "game" else "games"}" }) { it.played }
        ladder("library", TGroup.SHELF, "Коллекционер", "Collector", listOf(10, 25, 50, 100, 250),
            { "${ru(it, "игра", "игры", "игр")} в коллекции" }, { "$it games in the collection" }) { it.games.size }
        ladder("finish", TGroup.SHELF, "Титры", "Credits", listOf(1, 5, 10, 25, 50, 100),
            { "Пройти ${ru(it, "игру", "игры", "игр")}" }, { "Finish $it ${if (it == 1) "game" else "games"}" }) { it.finished }
        ladder("hundred", TGroup.SHELF, "Перфекционист", "Completionist", listOf(1, 5, 10, 25),
            { "Пройти на 100% ${ru(it, "игру", "игры", "игр")}" }, { "Finish $it ${if (it == 1) "game" else "games"} 100%" }) { it.hundred }
        ladder("genres", TGroup.SHELF, "Всеядный", "Omnivore", listOf(3, 6, 10, 15, 20),
            { "Попробовать ${ru(it, "жанр", "жанра", "жанров")}" }, { "Try $it genres" }) { it.genres }
        ladder("platforms", TGroup.SHELF, "Мультиплатформа", "Multiplatform", listOf(2, 4, 6, 8),
            { "Сыграть на ${ru(it, "платформе", "платформах", "платформах")}" }, { "Play on $it platforms" }) { it.platforms.size }
        ladder("onegame", TGroup.SHELF, "Однолюб", "Devoted", listOf(10, 50, 100, 300),
            { "Наиграть ${ru(it, "час", "часа", "часов")} в одну игру" }, { "Play one game for $it hours" }, hours = true) { it.maxGameMin }
        ladder("faith", TGroup.SHELF, "Верность", "Loyalty", listOf(10, 30, 100),
            { "Возвращаться к одной игре ${ru(it, "день", "дня", "дней")}" }, { "Play one game on $it different days" }, plat = false) { it.maxGameDays }
        ladder("video", TGroup.SHELF, "Геймпад", "Gamepad", listOf(10, 50, 100, 500),
            { "${ru(it, "сессия", "сессии", "сессий")} в видеоигры" }, { "$it video game sessions" }) { it.kindCount[Game.VIDEO] ?: 0 }
        ladder("phone", TGroup.SHELF, "В кармане", "In the pocket", listOf(10, 50, 200),
            { "${ru(it, "сессия", "сессии", "сессий")} на телефоне" }, { "$it sessions on the phone" }, plat = false) { it.phone }
        ladder("bosses", TGroup.SHELF, "Гроза боссов", "Boss slayer", listOf(1, 10, 50),
            { "Отметить ${ru(it, "побеждённого босса", "побеждённых боссов", "побеждённых боссов")}" }, { "Mark $it ${if (it == 1) "boss" else "bosses"} beaten" }, plat = false) { it.bosses }
        ladder("records", TGroup.SHELF, "Рекордсмен", "Record breaker", listOf(1, 10, 50),
            { "Побить свой рекорд ${ru(it, "раз", "раза", "раз")}" }, { "Beat your own record $it ${if (it == 1) "time" else "times"}" }, plat = false) { it.records }

        // ---------- at the table ----------
        ladder("board", TGroup.TABLE, "Настольщик", "Board gamer", listOf(1, 10, 50, 100, 250),
            { "Сыграть ${ru(it, "партию", "партии", "партий")} в настолки" }, { "Play $it board game ${if (it == 1) "session" else "sessions"}" }) { it.kindCount[Game.BOARD] ?: 0 }
        ladder("rpg", TGroup.TABLE, "Искатель приключений", "Adventurer", listOf(1, 5, 20, 50),
            { if (it == 1) "Сыграть в настольную ролевую игру" else "Сыграть в ролёвку ${ru(it, "раз", "раза", "раз")}" }, { "Play $it tabletop RPG ${if (it == 1) "session" else "sessions"}" }) { it.kindCount[Game.RPG] ?: 0 }
        ladder("cards", TGroup.TABLE, "Картёжник", "Card player", listOf(1, 10, 50),
            { "Сыграть ${ru(it, "партию", "партии", "партий")} в карточные игры" }, { "Play $it card game ${if (it == 1) "session" else "sessions"}" }, plat = false) { it.kindCount[Game.CARD] ?: 0 }
        ladder("wins", TGroup.TABLE, "Победитель", "Winner", listOf(1, 10, 25, 50, 100, 250),
            { "Победить ${ru(it, "раз", "раза", "раз")}" }, { "Win $it ${if (it == 1) "time" else "times"}" }) { it.wins }
        ladder("coop", TGroup.TABLE, "Команда мечты", "Dream team", listOf(1, 5, 25),
            { "Победить вместе ${ru(it, "раз", "раза", "раз")}" }, { "Win together $it ${if (it == 1) "time" else "times"}" }, plat = false) { it.coopWins }
        ladder("winrow", TGroup.TABLE, "Непобедимый", "Unbeatable", listOf(3, 5, 10),
            { "${ru(it, "победа", "победы", "побед")} подряд" }, { "$it wins in a row" }, plat = false) { it.winStreak }
        ladder("friends", TGroup.TABLE, "Компания", "Company", listOf(1, 3, 5, 10, 20),
            { if (it == 1) "Добавить первого игрока" else "Добавить ${ru(it, "игрока", "игроков", "игроков")}" }, { "Add $it ${if (it == 1) "player" else "players"}" }) { it.friends.size }
        ladder("social", TGroup.TABLE, "Душа компании", "Life of the party", listOf(1, 10, 50, 100, 250),
            { if (it == 1) "Сыграть с кем-нибудь" else "Сыграть ${ru(it, "сессию", "сессии", "сессий")} в компании" }, { "Play $it ${if (it == 1) "session" else "sessions"} with someone" }) { it.social }
        ladder("duels", TGroup.TABLE, "Дуэлянт", "Duellist", listOf(1, 10, 50),
            { if (it == 1) "Сыграть один на один" else "Сыграть один на один ${ru(it, "раз", "раза", "раз")}" }, { "Play $it one-on-one ${if (it == 1) "duel" else "duels"}" }, plat = false) { it.duels }
        ladder("dice", TGroup.TABLE, "Кубик решил", "The dice decide", listOf(10, 100, 500),
            { "Бросить кубики ${ru(it, "раз", "раза", "раз")}" }, { "Roll the dice $it times" }, plat = false) { it.tool("dice") }
        ladder("first", TGroup.TABLE, "Первый ход", "First move", listOf(1, 10, 50),
            { "Выбрать первого игрока ${ru(it, "раз", "раза", "раз")}" }, { "Pick the first player $it ${if (it == 1) "time" else "times"}" }, plat = false) { it.tool("first") }
        ladder("score", TGroup.TABLE, "Счетовод", "Scorekeeper", listOf(1, 10, 50),
            { if (it == 1) "Вести протокол очков в партии" else "Вести протокол очков в ${ru(it, "партии", "партиях", "партиях")}" }, { "Keep the score sheet in $it ${if (it == 1) "session" else "sessions"}" }, plat = false) { it.tool("score") }
        ladder("timer", TGroup.TABLE, "Песочные часы", "Hourglass", listOf(1, 25),
            { "Запустить таймер хода ${ru(it, "раз", "раза", "раз")}" }, { "Start the turn timer $it ${if (it == 1) "time" else "times"}" }, plat = false) { it.tool("timer") }

        // ---------- genres ----------
        Genres.all.forEach { g ->
            list += Trophy("g10${g.id}", Tier.BRONZE, TGroup.GENRE, "Знаток: ${g.ru}", "Fan: ${g.en}", "Наиграть 10 часов в жанре «${g.ru}»", "Play 10 hours of ${g.en}", 10, hours = true, family = "genre.${g.id}", famRu = g.ru, famEn = g.en) { it.genreMin[g.id] ?: 0 }
            list += Trophy("g50${g.id}", Tier.SILVER, TGroup.GENRE, "Мастер: ${g.ru}", "Master: ${g.en}", "Наиграть 50 часов в жанре «${g.ru}»", "Play 50 hours of ${g.en}", 50, hours = true, family = "genre.${g.id}", famRu = g.ru, famEn = g.en) { it.genreMin[g.id] ?: 0 }
        }
        ladder("tablehours", TGroup.GENRE, "Часы за столом", "Hours at the table", listOf(10, 50, 100, 500),
            { "Наиграть ${ru(it, "час", "часа", "часов")} в настольные игры и ролёвки" }, { "Play $it hours of board games and tabletop RPGs" }, hours = true) { (it.kindMin[Game.BOARD] ?: 0) + (it.kindMin[Game.RPG] ?: 0) + (it.kindMin[Game.CARD] ?: 0) }

        // ---------- the tavern ----------
        ladder("pets", TGroup.TAVERN, "Зверинец", "Menagerie", listOf(1, 3, 5, 10, 15, 20, 25, 30, 36),
            { if (it == 36) "Собрать всех питомцев" else "Вырастить из яйца ${ru(it, "питомца", "питомцев", "питомцев")}" }, { if (it == 36) "Collect every creature" else "Hatch $it ${if (it == 1) "creature" else "creatures"}" }) { it.hatched }
        ladder("adults", TGroup.TAVERN, "Наставник", "Mentor", listOf(1, 5, 10, 20),
            { "Вырастить ${ru(it, "героя", "героев", "героев")}" }, { "Raise $it ${if (it == 1) "hero" else "heroes"}" }) { it.adults }
        ladder("legends", TGroup.TAVERN, "Легенды трактира", "Tavern legends", listOf(1, 3, 10),
            { "Вырастить ${ru(it, "легенду", "легенды", "легенд")}" }, { "Raise $it ${if (it == 1) "legend" else "legends"}" }, plat = false) { it.legends }
        ladder("quests", TGroup.TAVERN, "Наёмник", "Mercenary", listOf(1, 10, 50, 100),
            { "Выполнить ${ru(it, "задание", "задания", "заданий")} недели" }, { "Finish $it weekly ${if (it == 1) "quest" else "quests"}" }) { it.s.questsEver }
        ladder("gold", TGroup.TAVERN, "Казна", "Treasury", listOf(500, 2000, 10000, 50000),
            { "Заработать ${ru(it, "золотой", "золотых", "золотых")}" }, { "Earn ${fmt(it)} gold" }) { it.s.goldEver }
        ladder("items", TGroup.TAVERN, "Уютный трактир", "Cosy tavern", listOf(1, 4, 8, 16),
            { if (it == 16) "Обставить трактир полностью" else "Купить ${ru(it, "вещь", "вещи", "вещей")} для трактира" }, { if (it == 16) "Furnish the tavern in full" else "Buy $it ${if (it == 1) "thing" else "things"} for the tavern" }) { it.s.items.size }
        ladder("season", TGroup.TAVERN, "Сезонный пропуск", "Season pass", listOf(5, 10, 20, 30),
            { "Дойти до ${it}-го уровня сезона" }, { "Reach season level $it" }) { f -> maxOf(Season.level(f.s.seasonXp), f.s.seasons.values.maxOrNull() ?: 0) }
        ladder("seasons", TGroup.TAVERN, "Старожил", "Old-timer", listOf(2, 4, 8),
            { "Играть ${ru(it, "сезон", "сезона", "сезонов")}" }, { "Play in $it seasons" }, plat = false) { f -> f.s.seasons.size + b(f.s.seasonXp > 0) }
        ladder("trophies", TGroup.TAVERN, "Охотник за трофеями", "Trophy hunter", listOf(10, 25, 50, 100, 150),
            { "Собрать ${ru(it, "трофей", "трофея", "трофеев")}" }, { "Collect $it trophies" }) { it.trophies }

        // ---------- the diary ----------
        ladder("shots", TGroup.DIARY, "Фотомод", "Photo mode", listOf(1, 10, 50, 100, 500, 1000),
            { "Сохранить ${ru(it, "скриншот или фото", "скриншота или фото", "скриншотов и фото")}" }, { "Keep $it ${if (it == 1) "screenshot or photo" else "screenshots and photos"}" }) { it.shots }
        ladder("notes", TGroup.DIARY, "Летописец", "Chronicler", listOf(1, 10, 50, 100),
            { if (it == 1) "Написать заметку к сессии" else "Написать заметки к ${ru(it, "сессии", "сессиям", "сессиям")}" }, { "Write notes for $it ${if (it == 1) "session" else "sessions"}" }) { it.notes }
        ladder("rated", TGroup.DIARY, "Критик", "Critic", listOf(1, 10, 50),
            { "Оценить ${ru(it, "игру", "игры", "игр")}" }, { "Rate $it ${if (it == 1) "game" else "games"}" }, plat = false) { it.rated }
        one("gallery", Tier.SILVER, TGroup.DIARY, "Репортаж", "Photo report", "10 снимков в одной сессии", "10 pictures in one session") { b(it.maxShots >= 10) }
        one("essay", Tier.SILVER, TGroup.DIARY, "Мемуары", "Memoirs", "Написать заметку длиннее 200 знаков", "Write a note longer than 200 characters") { b(it.longNote) }
        one("ten", Tier.BRONZE, TGroup.DIARY, "Шедевр", "Masterpiece", "Поставить игре 10 из 10", "Give a game 10 out of 10") { f -> b(f.games.any { it.rating == 10 }) }
        one("mood", Tier.BRONZE, TGroup.DIARY, "По настроению", "In the mood", "Отметить настроение в 20 сессиях", "Mark your mood in 20 sessions", 20) { f -> f.plays.count { it.mood > 0 } }

        // ---------- secret ----------
        val S = TGroup.SECRET
        one("oneMore", Tier.SILVER, S, "Ещё один ход", "One more turn", "Закончить вечернюю сессию после трёх ночи", "End an evening session after 3 am", hidden = true) { b(it.oneMoreTurn) }
        one("early", Tier.BRONZE, S, "Ранняя пташка", "Early bird", "Начать сессию до семи утра", "Start a session before 7 am", hidden = true) { b(it.early >= 1) }
        one("raider", Tier.SILVER, S, "Ночной рейдер", "Night raider", "25 сессий после одиннадцати вечера", "25 sessions after 11 pm", 25, hidden = true) { it.night }
        one("lunch", Tier.BRONZE, S, "Обеденный перерыв", "Lunch break", "5 коротких сессий в обед", "5 short sessions at lunchtime", 5, hidden = true) { it.lunch }
        one("weekend", Tier.SILVER, S, "Воин выходного дня", "Weekend warrior", "Играть 5 выходных подряд", "Play on 5 weekends in a row", 5, hidden = true) { it.weekends }
        one("nemesis", Tier.SILVER, S, "Немезида", "Nemesis", "Три раза подряд проиграть одному и тому же игроку", "Lose to the same player three times in a row", hidden = true) { b(it.nemesis >= 3) }
        one("revenge", Tier.SILVER, S, "Реванш", "Revenge", "Победить того, кому только что проиграли", "Beat someone you have just lost to", hidden = true) { b(it.revenge) }
        one("saw", Tier.BRONZE, S, "Чёрная полоса", "Losing streak", "Проиграть три раза подряд", "Lose three times in a row", hidden = true) { b(it.lossStreak >= 3) }
        one("phoenix", Tier.SILVER, S, "Из пепла", "From the ashes", "Проиграть три раза подряд – и сыграть снова", "Lose three times in a row – and play again", hidden = true) { b(it.roseAgain) }
        one("draw", Tier.BRONZE, S, "Боевая ничья", "A fair draw", "Сыграть вничью", "Play a draw", hidden = true) { b(it.draws >= 1) }
        one("party", Tier.SILVER, S, "Полный стол", "Full table", "Сыграть вшестером или большей компанией", "Play with six or more at the table", hidden = true) { b(it.maxTable >= 6) }
        one("evening", Tier.GOLD, S, "Чемпион вечера", "Champion of the evening", "Выиграть три настольные партии за один день", "Win three tabletop games in one day", hidden = true) { b(it.dayBoardWins >= 3) }
        one("comeback", Tier.GOLD, S, "Старый друг", "Old friend", "Вернуться к игре через год", "Come back to a game after a year", hidden = true) { b(it.gameGap >= 365) }
        one("return", Tier.BRONZE, S, "С возвращением", "Welcome back", "Сыграть после месяца без игр", "Play after a month without games", hidden = true) { b(it.anyGap >= 30) }
        one("quick", Tier.SILVER, S, "Запоем", "In one go", "Пройти игру за неделю с первой сессии", "Finish a game within a week of the first session", hidden = true) { b(it.quickFinish) }
        one("backlog", Tier.GOLD, S, "Дождалась", "Worth the wait", "Пройти игру, что год пролежала в коллекции", "Finish a game that sat in the collection for a year", hidden = true) { b(it.shelfKill) }
        one("veteran", Tier.GOLD, S, "Ветеран", "Veteran", "Играть в одну игру три года подряд", "Play one game in three different years", hidden = true) { b(it.gameYears >= 3) }
        one("sampler", Tier.BRONZE, S, "Дегустатор", "Sampler", "10 игр, сыгранных всего по разу", "10 games played just once", 10, hidden = true) { it.onceOnly }
        one("hopper", Tier.SILVER, S, "Прыгун", "Platform hopper", "Три платформы за один день", "Three platforms in one day", hidden = true) { b(it.dayPlatforms >= 3) }
        one("buffet", Tier.SILVER, S, "Шведский стол", "Buffet", "Четыре разные игры за один день", "Four different games in one day", hidden = true) { b(it.dayGames >= 4) }
        one("marathon", Tier.GOLD, S, "Марафон с умом", "A wise marathon", "Сессия от трёх часов с двумя перерывами", "A session of three hours or more with two breaks", hidden = true) { b(it.wiseMarathon) }
        one("retro", Tier.SILVER, S, "Старая школа", "Old school", "Сыграть в игру, вышедшую до 1996 года", "Play a game released before 1996", hidden = true) { b(it.retro) }
        one("fresh", Tier.BRONZE, S, "С пылу с жару", "Hot off the press", "Сыграть в игру в год её выхода", "Play a game in the year it came out", hidden = true) { b(it.fresh) }
        one("newyear", Tier.SILVER, S, "С Новым годом!", "Happy New Year!", "Сыграть первого января", "Play on the first of January", hidden = true) { b(it.newYear) }
        one("halloween", Tier.SILVER, S, "Канун Дня всех святых", "All Hallows' Eve", "Сыграть в хоррор 31 октября", "Play a horror game on 31 October", hidden = true) { b(it.halloween) }
        one("friday13", Tier.SILVER, S, "Пятница, 13-е", "Friday the 13th", "Сыграть в пятницу тринадцатого", "Play on a Friday the 13th", hidden = true) { b(it.friday13) }
        one("century", Tier.BRONZE, S, "Сотня", "A hundred", "Набрать 100 очков в одной партии", "Score 100 points in one session", hidden = true) { b(it.bigScore) }
        one("shiny", Tier.GOLD, S, "Не такой, как все", "One of a kind", "Вырастить питомца редкой окраски", "Hatch a creature of a rare colour", hidden = true) { b(it.shiny) }
        one("legendary", Tier.GOLD, S, "Легендарная тройка", "The legendary three", "Поселить дракона, феникса и единорога", "Bring home the dragon, the phoenix and the unicorn", 3, hidden = true) { f -> f.pets.count { it.hatched > 0 && Pets.of(it.species).rare } }
        one("fullhouse", Tier.BRONZE, S, "Полный дом", "Full house", "Трое питомцев живут в трактире", "Three creatures live in the tavern", 3, hidden = true) { f -> Engine.team(f.pets).size }
        one("tracker", Tier.BRONZE, S, "Следопыт", "Tracker", "Подтвердить 10 сессий, найденных в телефоне", "Confirm 10 sessions found on the phone", 10, hidden = true) { f -> f.plays.count { it.auto } }
        one("allkinds", Tier.GOLD, S, "И швец, и жнец", "Jack of all games", "Сыграть в видеоигру, настолку, карточную игру и ролёвку", "Play a video game, a board game, a card game and a tabletop RPG", 4, hidden = true) { f ->
            listOf(Game.VIDEO, Game.BOARD, Game.CARD, Game.RPG).count { (f.kindCount[it] ?: 0) > 0 } }
        one("stats", Tier.GOLD, S, "Сбалансированный билд", "Balanced build", "Питомец, у которого все шесть характеристик не ниже 20", "A creature with all six stats at 20 or more", hidden = true) { f -> b(f.pets.any { p -> p.stats.all { it >= 20 } }) }
        one("thousand", Tier.PLATINUM, S, "Тысяча и одна ночь", "A thousand and one nights", "Сыграть 1001 сессию", "Play 1,001 sessions", 1001, hidden = true) { it.n }
    }

    val all: List<Trophy> = list
    val byId = all.associateBy { it.id }
    /** The trophies as ladders, in their order. */
    val families: List<Family> = all.groupBy { it.family }.map { Family(it.key, it.value) }

    /** Progress as the eye reads it: hours for the hour ones. */
    fun shown(t: Trophy, f: Facts): Pair<Int, Int> = (if (t.hours) t.p(f) / 60 else t.p(f)).coerceAtMost(t.need) to t.need
    fun earned(t: Trophy, f: Facts): Boolean = (if (t.hours) t.p(f) / 60 else t.p(f)) >= t.need
    /** Trophies earned by now and not yet written down. */
    fun fresh(f: Facts): List<Trophy> = all.filter { it.id !in f.s.trophies && earned(it, f) }
}
