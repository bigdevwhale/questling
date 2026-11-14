package app.questling.domain

import app.questling.data.Pet

/** What a creature doubles (in gold) while it lives in the tavern: the leader or one of the two by the fire. */
enum class Perk { DAYS, GENRE, FINISH, QUEST, BOARD, NIGHT, SHOTS, NEW, MOBILE, WIN, SHORT, FRIENDS, BREAK, COMEBACK, DEVOTED, PLATFORMS, NOTE, TROPHY, ALL }

/**
 * A kind of creature. All but the questling are found by playing a certain way ([find]);
 * [perk] is what it doubles; [likes] the genres a GENRE perk counts. [world] true for creatures out of game worlds.
 */
data class Species(
    val id: String, val ru: String, val en: String, val color: Long,
    val names: List<String>, val namesEn: List<String>,
    val perk: Perk, val likes: List<String>,
    val perkRu: String, val perkEn: String,
    val findRu: String, val findEn: String,
    val world: Boolean, val rare: Boolean = false,
) {
    val title get() = tr(ru, en)
    val perkText get() = tr(perkRu, perkEn)
    val find get() = tr(findRu, findEn)
}

/** The stages: an egg, then four steps of adventure. */
enum class Stage { EGG, BABY, TEEN, ADULT, MASTER }

object Pets {
    const val HATCH = 6
    const val TEEN = 600; const val ADULT = 3000; const val MASTER = 12000

    private fun sp(id: String, ru: String, en: String, color: Long, names: String, namesEn: String, perk: Perk, likes: String, perkRu: String, perkEn: String,
                   findRu: String, findEn: String, world: Boolean, rare: Boolean = false) =
        Species(id, ru, en, color, names.split(","), namesEn.split(","), perk, likes.split(",").filter { it.isNotEmpty() }, perkRu, perkEn, findRu, findEn, world, rare)

    val all: List<Species> = listOf(
        sp("questling", "Квестлинг", "Questling", 0xFFE8DCC0, "Квест,Рюкзачок,Искатель,Пуговка,Чекпойнт", "Quest,Satchel,Seeker,Button,Checkpoint", Perk.DAYS, "",
            "Зовёт в путь каждый день: первая сессия дня приносит вдвое больше золота", "Calls you out every day: the first session of a day brings double gold",
            "Живёт с вами с первого дня", "Lives with you from day one", true),
        sp("slime", "Слайм", "Slime", 0xFF7AD0A0, "Желе,Буль,Капля,Мятный,Плюх", "Jelly,Blob,Droplet,Minty,Plop", Perk.GENRE, "rpg",
            "Первый враг любого героя: RPG приносят вдвое больше", "Every hero's first foe: RPGs bring double",
            "Наиграйте 20 часов в RPG", "Play RPGs for 20 hours", true),
        sp("mimic", "Мимик", "Mimic", 0xFFA0703A, "Сундучок,Щёлк,Ам,Ларчик,Замочек", "Chesty,Snap,Chomp,Coffer,Latch", Perk.FINISH, "",
            "Глотает пройденные игры: за титры и 100% – вдвое больше", "Swallows finished games: credits and 100% bring double",
            "Пройдите первую игру до конца", "Finish your first game", true),
        sp("goblin", "Гоблин-торговец", "Goblin merchant", 0xFF6AA04A, "Барыга,Звяк,Торгаш,Ушастик,Скидка", "Haggle,Clink,Trader,Ears,Bargain", Perk.QUEST, "",
            "Знает цену делам: задания недели приносят вдвое больше", "Knows what deeds are worth: weekly quests bring double",
            "Выполните 10 заданий недели", "Finish 10 weekly quests", true),
        sp("shroom", "Грибок", "Shroomling", 0xFF9A6AC8, "Шляпка,Спора,Боровик,Прыг,Крапинка", "Cap,Spore,Bolete,Hop,Speckle", Perk.GENRE, "platformer,arcade",
            "Растёт там, где прыгают: платформеры и аркады приносят вдвое больше", "Grows where there's jumping: platformers and arcade games bring double",
            "Наиграйте 8 часов в платформеры или аркады", "Play platformers or arcade games for 8 hours", true),
        sp("ghost", "Привидение", "Ghost", 0xFFA8D0E8, "Бу,Простынка,Шорох,Пиксель,Ойк", "Boo,Sheet,Rustle,Pixel,Eek", Perk.GENRE, "horror,detective",
            "Любит, когда страшно: хорроры и детективы приносят вдвое больше", "Loves a scare: horror and mystery bring double",
            "Наиграйте 8 часов в хорроры или детективы", "Play horror or mystery games for 8 hours", true),
        sp("meeple", "Мипл", "Meeple", 0xFFE08A3A, "Мипл,Фишка,Ходок,Деревяшка,Рыжий", "Meep,Token,Walker,Woody,Ginger", Perk.BOARD, "",
            "Живёт на игровом столе: настолки приносят вдвое больше", "Lives on the game table: board games bring double",
            "Сыграйте 10 партий в настольные игры", "Play 10 board game sessions", true),
        sp("dicey", "Двадцатигранник", "Dicey", 0xFFC8303A, "Крит,Двадцатка,Грань,Бросок,Натурал", "Crit,Twenty,Facet,Roll,Natural", Perk.GENRE, "trpg,dice",
            "Всегда падает двадцаткой: ролёвки и игры с кубиками приносят вдвое больше", "Always lands on twenty: tabletop RPGs and dice games bring double",
            "Сыграйте в ролёвку или 30 раз бросьте кубики в инструментах", "Play a tabletop RPG or roll the dice 30 times in the tools", true),
        sp("joker", "Джокер", "Joker", 0xFFE8E0D0, "Туз,Бубенчик,Козырь,Шут,Валет", "Ace,Jingle,Trump,Jester,Jack", Perk.GENRE, "deck",
            "Козырь в рукаве: карточные игры приносят вдвое больше", "An ace up the sleeve: card games bring double",
            "Сыграйте 10 партий в карточные игры", "Play 10 card game sessions", true),
        sp("robo", "Робот-NPC", "Robo NPC", 0xFF3FA0A0, "Бип,Квестодатель,Болтик,Экран,Нолик", "Beep,Questgiver,Bolt,Screen,Zero", Perk.GENRE, "shooter,stealth",
            "Сканирует всё вокруг: шутеры и стелс приносят вдвое больше", "Scans everything around: shooters and stealth bring double",
            "Наиграйте 15 часов в шутеры или стелс", "Play shooters or stealth games for 15 hours", true),
        sp("dragon", "Дракон", "Dragon", 0xFF3FA05A, "Изумруд,Клад,Пламень,Дракоша,Горыч", "Emerald,Hoard,Blaze,Draco,Smoulder", Perk.ALL, "",
            "Финальный босс. С ним всё золото приходит с прибавкой", "The final boss. With it, all gold comes with a bonus",
            "Пройдите 10 игр", "Finish 10 games", true, rare = true),
        sp("frogknight", "Рыцарь-лягушонок", "Frog knight", 0xFF5AA050, "Ква,Перекат,Сэр Прыг,Щиток,Забрало", "Ribbit,Roll,Sir Hop,Buckler,Visor", Perk.GENRE, "action,fighting",
            "Перекатывается от любой беды: экшен и файтинги приносят вдвое больше", "Rolls away from any trouble: action and fighting games bring double",
            "Наиграйте 15 часов в экшен", "Play action games for 15 hours", true),
        sp("necrocat", "Некрокот", "Necro cat", 0xFF2A3A30, "Косточка,Полночь,Фонарик,Мурзомби,Уголёк", "Bones,Midnight,Lantern,Purrgatory,Coal", Perk.NIGHT, "",
            "Не спит по ночам: сессии после одиннадцати вечера приносят вдвое больше", "Up all night: sessions after 11 pm bring double",
            "Сыграйте 15 сессий после одиннадцати вечера", "Play 15 sessions after 11 pm", true),
        sp("starjelly", "Звёздная медуза", "Star jelly", 0xFF3A4AA0, "Туманность,Комета,Орбита,Искра,Глубина", "Nebula,Comet,Orbit,Spark,Deep", Perk.SHOTS, "",
            "Собирает звёздные снимки: сессии со скриншотами приносят вдвое больше", "Collects star pictures: sessions with screenshots bring double",
            "Сохраните 40 скриншотов или фото", "Keep 40 screenshots or photos", true),
        sp("raccoon", "Енот-гонщик", "Racer raccoon", 0xFF8A8A8A, "Дрифт,Шарф,Форсаж,Полосатик,Нитро", "Drift,Scarf,Turbo,Stripes,Nitro", Perk.GENRE, "racing,sports",
            "Любит скорость: гонки и спорт приносят вдвое больше", "Loves speed: racing and sports bring double",
            "Наиграйте 8 часов в гонки или спортивные игры", "Play racing or sports games for 8 hours", false),
        sp("capybara", "Капибара", "Capybara", 0xFFB08A5A, "Грядка,Покой,Мандаринка,Кабачок,Дзен", "Patch,Calm,Tangerine,Squash,Zen", Perk.GENRE, "sim,casual,sandbox",
            "Никуда не спешит: симуляторы, песочницы и казуалки приносят вдвое больше", "Never in a hurry: sims, sandboxes and casual games bring double",
            "Наиграйте 20 часов в симуляторы или песочницы", "Play sims or sandbox games for 20 hours", false),
        sp("axolotl", "Аксолотль", "Axolotl", 0xFFF0A0B8, "Пазл,Жабрик,Улыбка,Розочка,Кусочек", "Puzzle,Gills,Smiley,Rosy,Piece", Perk.GENRE, "puzzle,abstract",
            "Находит, куда встанет кусочек: головоломки и абстрактные игры приносят вдвое больше", "Finds where the piece goes: puzzles and abstract games bring double",
            "Наиграйте 8 часов в головоломки", "Play puzzle games for 8 hours", false),
        sp("fox", "Лис-первопроходец", "Explorer fox", 0xFFE0803A, "Компас,Тропа,Рыжик,Карта,Следопыт", "Compass,Trail,Rusty,Map,Scout", Perk.NEW, "",
            "Любит новое: первая сессия в новой игре приносит втрое больше", "Loves the new: the first session of a new game brings triple",
            "Попробуйте 15 разных игр", "Try 15 different games", false),
        sp("owl", "Сова-стратег", "Strategist owl", 0xFF8A8A9A, "Генерал,Гамбит,Фланг,Ухтыш,Маршал", "General,Gambit,Flank,Hoot,Marshal", Perk.GENRE, "strategy,wargame,euro",
            "Думает на три хода вперёд: стратегии, варгеймы и евро приносят вдвое больше", "Thinks three moves ahead: strategy, wargames and eurogames bring double",
            "Наиграйте 20 часов в стратегии или евро", "Play strategy or eurogames for 20 hours", false),
        sp("hamster", "Хомяк", "Hamster", 0xFFE0B060, "Колесо,Щёчка,Тап,Зёрнышко,Пушок", "Wheel,Cheeks,Tap,Seed,Fluff", Perk.MOBILE, "",
            "Крутит колесо в кармане: игры на телефоне приносят вдвое больше", "Spins the wheel in your pocket: phone games bring double",
            "Сыграйте 20 сессий на телефоне", "Play 20 sessions on the phone", false),
        sp("penguin", "Пингвин-чемпион", "Champion penguin", 0xFF2A3A4A, "Свисток,Гол,Чемпион,Льдинка,Финт", "Whistle,Goal,Champ,Icy,Feint", Perk.WIN, "",
            "Играет на победу: каждая победа приносит вдвое больше", "Plays to win: every win brings double",
            "Победите 15 раз", "Win 15 times", false),
        sp("bunny", "Зайка-диджей", "DJ bunny", 0xFFC0A0E0, "Бит,Такт,Ушастик,Ремикс,Прыг-скок", "Beat,Tempo,Ears,Remix,Hophop", Perk.SHORT, "rhythm",
            "Успевает за один трек: короткие сессии до 20 минут приносят вдвое больше", "Done in one track: short sessions up to 20 minutes bring double",
            "Сыграйте 25 коротких сессий – до 20 минут", "Play 25 short sessions – up to 20 minutes", false),
        sp("bear", "Мишка", "Bear", 0xFF8A5A3A, "Обнимашка,Шарф,Топтыжка,Мёд,Компанейский", "Hug,Scarf,Bruin,Honey,Buddy", Perk.FRIENDS, "",
            "С друзьями веселее: сессии с кем-то приносят вдвое больше", "Better together: sessions with someone bring double",
            "Сыграйте 20 сессий с друзьями", "Play 20 sessions with friends", false),
        sp("cat", "Рыжий кот", "Ginger cat", 0xFFE0903A, "Джойстик,Мандарин,Мурчало,Геймпад,Лапка", "Joystick,Tangerine,Purr,Gamepad,Paws", Perk.BREAK, "",
            "Знает толк в отдыхе: перерывы посреди сессии приносят золото", "Knows how to rest: breaks in the middle of a session bring gold",
            "Сделайте 20 перерывов во время сессий", "Take 20 breaks during sessions", false),
        sp("corgi", "Корги", "Corgi", 0xFFE0A050, "Картридж,Апорт,Булочка,Хвостик,Сейв", "Cartridge,Fetch,Bun,Wiggle,Save", Perk.COMEBACK, "",
            "Приносит забытое: возвращение к игре после месяца перерыва приносит втрое больше", "Fetches the forgotten: coming back to a game after a month brings triple",
            "Вернитесь к игре, в которую не играли больше 30 дней", "Come back to a game you haven't played for over 30 days", false),
        sp("tortoise", "Черепаха-крепость", "Castle tortoise", 0xFF5A8A5A, "Цитадель,Тихоход,Бастион,Панцирь,Век", "Citadel,Slowpoke,Bastion,Shell,Aeon", Perk.DEVOTED, "",
            "Никогда не бросает начатое: игры, где уже больше 20 часов, приносят вдвое больше", "Never drops what it started: games with over 20 hours bring double",
            "Наиграйте 50 часов в одну игру", "Play one game for 50 hours", false),
        sp("crab", "Краб-боксёр", "Boxer crab", 0xFFD04030, "Хук,Раунд,Клешня,Нокаут,Джеб", "Hook,Round,Pincer,Knockout,Jab", Perk.GENRE, "fighting,thematic",
            "Не отступает: файтинги и америтреш приносят вдвое больше", "Never backs down: fighting games and thematic board games bring double",
            "Сыграйте 10 дуэлей – партий один на один", "Play 10 duels – one-on-one sessions", false),
        sp("bat", "Летучая мышь", "Bat", 0xFF6A4A8A, "Пинг,Гарнитура,Эхо,Ночница,Лаг", "Ping,Headset,Echo,Dusk,Lag", Perk.GENRE, "mmo",
            "Всегда на связи: онлайн-игры приносят вдвое больше", "Always online: online games bring double",
            "Наиграйте 15 часов в онлайн-игры", "Play online games for 15 hours", false),
        sp("golem", "Кубический голем", "Block golem", 0xFF7A8A6A, "Кирка,Блок,Валун,Мох,Крафт", "Pickaxe,Block,Boulder,Moss,Craft", Perk.GENRE, "sandbox,strategy",
            "Строит мир по кубику: песочницы и стратегии приносят вдвое больше", "Builds a world block by block: sandboxes and strategy bring double",
            "Сыграйте в одну игру в 30 разных дней", "Play one game on 30 different days", true),
        sp("fairy", "Фея-рассказчица", "Storyteller fairy", 0xFFE0B0D0, "Сказка,Глава,Пыльца,Закладка,Эпилог", "Tale,Chapter,Pollen,Bookmark,Epilogue", Perk.NOTE, "story,adventure",
            "Записывает истории: сессии с заметкой приносят вдвое больше", "Writes the stories down: sessions with a note bring double",
            "Напишите заметки к 20 сессиям", "Write notes for 20 sessions", true),
        sp("skelly", "Скелетик", "Skelly", 0xFFE0E0D0, "Косточка,Респаун,Череп,Щиток,Ещё раз", "Bones,Respawn,Skully,Buckler,Again", Perk.GENRE, "roguelike",
            "Собирается заново после каждой неудачи: рогалики приносят вдвое больше", "Puts itself back together after every fall: roguelikes bring double",
            "Проиграйте три раза подряд – и сыграйте снова", "Lose three times in a row – and play again", true),
        sp("octopus", "Осьминог", "Octopus", 0xFF3A9A9A, "Мультитап,Щупалец,Восьмёрка,Комбо,Присоска", "Multitap,Tentacle,Eight,Combo,Sucker", Perk.PLATFORMS, "",
            "Держит четыре геймпада сразу: вторая платформа за день приносит вдвое больше", "Holds four controllers at once: a day's second platform brings double",
            "Сыграйте на пяти разных платформах", "Play on five different platforms", false),
        sp("bee", "Пчела-строитель", "Builder bee", 0xFFE0B020, "Сота,Жужа,Каска,Прораб,Медок", "Comb,Buzz,Hardhat,Foreman,Honey", Perk.GENRE, "euro,family,coop",
            "Тайл к тайлу: евро, семейные и кооперативные игры приносят вдвое больше", "Tile by tile: euro, family and co-op games bring double",
            "Сыграйте в четырёх разных жанрах за одну неделю", "Play four different genres in one week", false),
        sp("phoenix", "Феникс", "Phoenix", 0xFFE08020, "Пепел,Жар,Финист,Заря,Огнекрыл", "Ash,Flare,Fenix,Dawnfire,Pyre", Perk.ALL, "",
            "Возрождается из пепла. С ним всё золото приходит с прибавкой", "Rises from the ashes. With it, all gold comes with a bonus",
            "Пройдите игру на 100%", "Finish a game 100%", true, rare = true),
        sp("unicorn", "Единорог", "Unicorn", 0xFFE8D0F0, "Сияние,Радуга,Кубок,Перламутр,Звездопад", "Glow,Rainbow,Trophy,Pearl,Starfall", Perk.ALL, "",
            "Приходит к тем, кто играет давно. С ним всё золото приходит с прибавкой", "Comes to those who have played long. With it, all gold comes with a bonus",
            "Наиграйте 300 часов", "Reach 300 hours of play", false, rare = true),
        sp("coinling", "Монетка", "Coinling", 0xFFF0C030, "Звяк,Грошик,Орёл,Решка,Блеск", "Clink,Penny,Heads,Tails,Shine", Perk.TROPHY, "",
            "Тянется к блестящему: трофеи приносят вдвое больше золота", "Drawn to shiny things: trophies bring double gold",
            "Соберите 50 трофеев", "Collect 50 trophies", true),
    )
    val byId = all.associateBy { it.id }
    fun of(id: String) = byId[id] ?: all[0]
}

fun Pet.stage(): Stage = when {
    hatched == 0L -> Stage.EGG
    xp < Pets.TEEN -> Stage.BABY
    xp < Pets.ADULT -> Stage.TEEN
    xp < Pets.MASTER -> Stage.ADULT
    else -> Stage.MASTER
}

fun Stage.title() = when (this) {
    Stage.EGG -> tr("Яйцо", "Egg"); Stage.BABY -> tr("Малыш", "Hatchling"); Stage.TEEN -> tr("Искатель", "Seeker")
    Stage.ADULT -> tr("Герой", "Hero"); Stage.MASTER -> tr("Легенда", "Legend")
}

/** XP to the next stage, as (have, need) within the stage. */
fun Pet.toNext(): Pair<Int, Int> = when (stage()) {
    Stage.EGG -> growth to Pets.HATCH
    Stage.BABY -> xp to Pets.TEEN
    Stage.TEEN -> (xp - Pets.TEEN) to (Pets.ADULT - Pets.TEEN)
    Stage.ADULT -> (xp - Pets.ADULT) to (Pets.MASTER - Pets.ADULT)
    Stage.MASTER -> 1 to 1
}

/** The genre it was raised on most, once it has grown up. */
fun Pet.speciality(): Genre? = if (stage() < Stage.ADULT) null else genreMap().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { Genres.of(it.key) }

/** Its class, by its highest stat, once it has grown up: warrior, mage, rogue, bard, paladin, gambler. */
fun Pet.klass(): Stat? = if (stage() < Stage.TEEN || stats.all { it == 0 }) null else Stat.entries[stats.indexOf(stats.max())]

/** Level 1..99 out of xp, for the eye. */
fun Pet.level(): Int = (1 + kotlin.math.sqrt(xp / 30.0)).toInt().coerceIn(1, 99)
