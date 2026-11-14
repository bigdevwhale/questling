package app.questling.domain

import app.questling.data.Game

/** The six stats every creature has; each genre trains one. */
enum class Stat(val ru: String, val en: String, val whyRu: String, val whyEn: String, val color: Long, val classRu: String, val classEn: String) {
    STR("Сила", "Might", "растёт от экшена, шутеров, файтингов и спорта", "grows on action, shooters, fighting and sports", 0xFFD0503A, "Воин", "Warrior"),
    WIT("Ум", "Wits", "растёт от стратегий, головоломок и евроигр", "grows on strategy, puzzles and eurogames", 0xFF4A7AD0, "Маг", "Mage"),
    DEX("Ловкость", "Agility", "растёт от платформеров, гонок и ритм-игр", "grows on platformers, racing and rhythm games", 0xFF3FA36B, "Плут", "Rogue"),
    CHA("Обаяние", "Charm", "растёт от игр с людьми: пати, кооп, ролёвки", "grows on games with people: party, co-op, role-play", 0xFFD0609A, "Бард", "Bard"),
    WIL("Стойкость", "Grit", "растёт от долгих путешествий: RPG, рогалики, хоррор", "grows on long journeys: RPGs, roguelikes, horror", 0xFF8A6AD0, "Паладин", "Paladin"),
    LCK("Удача", "Luck", "растёт от кубиков, карт и казуальных игр", "grows on dice, cards and casual games", 0xFFE0A030, "Везунчик", "Gambler");
    val title get() = tr(ru, en)
    val why get() = tr(whyRu, whyEn)
    val klass get() = tr(classRu, classEn)
}

/** A genre. [board] true for tabletop genres, false for video ones, null for both. [keys] English words Wikidata uses. */
data class Genre(val id: String, val ru: String, val en: String, val color: Long, val stat: Stat, val board: Boolean?, val keys: List<String>) {
    val title get() = tr(ru, en)
}

object Genres {
    private fun g(id: String, ru: String, en: String, color: Long, stat: Stat, board: Boolean?, keys: String) = Genre(id, ru, en, color, stat, board, keys.split(','))

    val all = listOf(
        g("rpg", "RPG", "RPG", 0xFF8A5AC8, Stat.WIL, false, "role-playing,rpg,jrpg"),
        g("action", "Экшен", "Action", 0xFFD0503A, Stat.STR, false, "action,hack and slash,beat 'em up,soulslike"),
        g("adventure", "Приключения", "Adventure", 0xFF3F9A8A, Stat.WIL, false, "adventure,metroidvania,open world"),
        g("shooter", "Шутер", "Shooter", 0xFFB0603A, Stat.STR, false, "shooter,fps,shoot 'em up"),
        g("strategy", "Стратегия", "Strategy", 0xFF4A7AD0, Stat.WIT, false, "strategy,tactic,4x,tower defense,moba"),
        g("puzzle", "Головоломка", "Puzzle", 0xFFE08AB0, Stat.WIT, null, "puzzle,logic"),
        g("platformer", "Платформер", "Platformer", 0xFF3FA36B, Stat.DEX, false, "platform"),
        g("horror", "Хоррор", "Horror", 0xFF5A4A6A, Stat.WIL, null, "horror"),
        g("sim", "Симулятор", "Simulation", 0xFFB09040, Stat.WIL, false, "simulat,farm,life sim,management,city-building,tycoon"),
        g("racing", "Гонки", "Racing", 0xFFE06A30, Stat.DEX, false, "racing,driving"),
        g("sports", "Спорт", "Sports", 0xFF4AA0C0, Stat.STR, false, "sport,football,basketball"),
        g("fighting", "Файтинг", "Fighting", 0xFFC03A4A, Stat.STR, false, "fighting"),
        g("roguelike", "Рогалик", "Roguelike", 0xFF7A8A5A, Stat.WIL, false, "rogue"),
        g("sandbox", "Песочница", "Sandbox", 0xFF6A9A4A, Stat.WIL, false, "sandbox,survival,craft"),
        g("rhythm", "Ритм-игра", "Rhythm", 0xFFD060D0, Stat.DEX, false, "rhythm,music"),
        g("story", "Сюжетная", "Story", 0xFFC8A070, Stat.CHA, false, "visual novel,interactive fiction,narrative,interactive film"),
        g("mmo", "Онлайн", "Online", 0xFF5A8AE0, Stat.CHA, false, "mmo,massively,battle royale,online"),
        g("arcade", "Аркада", "Arcade", 0xFFE0B030, Stat.DEX, false, "arcade,pinball"),
        g("stealth", "Стелс", "Stealth", 0xFF4A5A6A, Stat.DEX, false, "stealth"),
        g("casual", "Казуальная", "Casual", 0xFF8AC0E0, Stat.LCK, false, "casual,match-three,idle,clicker"),
        g("euro", "Евро", "Eurogame", 0xFF4A8A5A, Stat.WIT, true, "euro,worker placement,engine,economic,tile-placement,resource"),
        g("thematic", "Америтреш", "Thematic", 0xFFA0503A, Stat.STR, true, "thematic,dungeon crawl,miniature,adventure board"),
        g("party", "Пати", "Party", 0xFFE07AA0, Stat.CHA, null, "party,word game,trivia,social"),
        g("coop", "Кооператив", "Co-op", 0xFF5AA0A0, Stat.CHA, null, "cooperative,co-op"),
        g("deck", "Карточная", "Card game", 0xFFB04A6A, Stat.LCK, null, "card,deck-building,collectible,trick-taking"),
        g("abstract", "Абстрактная", "Abstract", 0xFF6A6A8A, Stat.WIT, true, "abstract,chess,go,checkers"),
        g("wargame", "Варгейм", "Wargame", 0xFF6A7A4A, Stat.STR, true, "wargame,war game"),
        g("family", "Семейная", "Family", 0xFFE0A060, Stat.CHA, true, "family,children"),
        g("dice", "Кубики", "Dice game", 0xFFD04A4A, Stat.LCK, true, "dice,roll-and-write,push your luck"),
        g("detective", "Детектив", "Deduction", 0xFF5A5A9A, Stat.WIT, null, "deduction,detective,mystery,hidden role"),
        g("trpg", "Ролевая (НРИ)", "Tabletop RPG", 0xFF9A4AB0, Stat.CHA, true, "tabletop role-playing,pen-and-paper"),
    )
    val byId = all.associateBy { it.id }
    fun of(id: String?) = byId[id]

    /** The genres offered for a kind of game, the fitting ones first. */
    fun forKind(kind: String): List<Genre> {
        val table = kind == Game.BOARD || kind == Game.RPG || kind == Game.CARD
        return all.filter { it.board == null || it.board == table } + all.filter { it.board != null && it.board != table }
    }

    /** Genre ids from Wikidata's English genre names. */
    fun guess(text: String, kind: String): List<String> {
        val t = text.lowercase()
        val out = forKind(kind).filter { g -> g.keys.any { it in t } }.map { it.id }.distinct().take(3)
        return if (out.isEmpty() && kind == Game.RPG) listOf("trpg") else if (out.isEmpty() && kind == Game.CARD) listOf("deck") else out
    }
}

object Kinds {
    fun title(kind: String) = when (kind) {
        Game.VIDEO -> tr("Видеоигра", "Video game"); Game.BOARD -> tr("Настольная", "Board game"); Game.RPG -> tr("Ролевая (НРИ)", "Tabletop RPG")
        Game.CARD -> tr("Карточная", "Card game"); Game.MOBILE -> tr("На телефоне", "Phone game"); else -> tr("Другое", "Other")
    }
    fun plural(kind: String) = when (kind) {
        Game.VIDEO -> tr("Видеоигры", "Video games"); Game.BOARD -> tr("Настолки", "Board games"); Game.RPG -> tr("Ролёвки", "Tabletop RPGs")
        Game.CARD -> tr("Карточные", "Card games"); Game.MOBILE -> tr("На телефоне", "Phone games"); else -> tr("Другое", "Other")
    }
    fun color(kind: String): Long = when (kind) {
        Game.VIDEO -> 0xFF4A7AD0; Game.BOARD -> 0xFFD0803A; Game.RPG -> 0xFF9A4AB0; Game.CARD -> 0xFFB04A6A; Game.MOBILE -> 0xFF3FA36B; else -> 0xFF8A8A8A
    }
    /** Played at a table with people around, so results and scores matter. */
    fun table(kind: String) = kind == Game.BOARD || kind == Game.RPG || kind == Game.CARD

    val PLATFORMS = listOf("PC", "PlayStation", "Xbox", "Switch", "Steam Deck", "Android", "iOS", "VR", "Retro")
    fun platformTitle(p: String) = when (p) { "Retro" -> tr("Ретро-консоль", "Retro console"); "table" -> tr("За столом", "At the table"); else -> p }

    fun status(st: String) = when (st) {
        Game.PLAYING -> tr("Играю", "Playing"); Game.SHELF -> tr("На полке", "On the shelf"); Game.FINISHED -> tr("Пройдена", "Finished")
        Game.HUNDRED -> "100%"; Game.DROPPED -> tr("Отложена", "Put aside"); else -> tr("Хочу", "Wishlist")
    }
    val STATUSES = listOf(Game.PLAYING, Game.SHELF, Game.FINISHED, Game.HUNDRED, Game.DROPPED, Game.WISH)

    fun result(r: Int) = when (r) {
        1 -> tr("Победа", "Win"); 2 -> tr("Поражение", "Loss"); 3 -> tr("Ничья", "Draw"); 4 -> tr("Победили вместе", "Won together"); 5 -> tr("Проиграли вместе", "Lost together"); else -> ""
    }
    fun moment(m: String) = when (m) {
        "finish" -> tr("Титры!", "Credits!"); "hundred" -> tr("Пройдено на 100%", "100% done"); "boss" -> tr("Босс повержен", "Boss beaten"); "first" -> tr("Первый запуск", "First launch")
        "record" -> tr("Рекорд", "Record"); "chapter" -> tr("Глава пройдена", "Chapter done"); else -> ""
    }
}
