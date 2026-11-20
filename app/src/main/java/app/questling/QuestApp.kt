package app.questling

import android.app.Application
import android.content.Context
import app.questling.data.Db
import app.questling.data.Found
import app.questling.data.Friend
import app.questling.data.Game
import app.questling.data.Live
import app.questling.data.Outside
import app.questling.data.OutsideSource
import app.questling.data.Pet
import app.questling.data.Play
import app.questling.data.Seat
import app.questling.data.SettingsStore
import app.questling.domain.Engine
import app.questling.domain.Facts
import app.questling.domain.Kinds
import app.questling.domain.Lang
import app.questling.domain.Perk
import app.questling.domain.Pets
import app.questling.domain.Quests
import app.questling.domain.Reward
import app.questling.domain.Season
import app.questling.domain.Shop
import app.questling.domain.Stat
import app.questling.domain.Trophies
import app.questling.domain.WeekFacts
import app.questling.domain.dayOf
import app.questling.domain.hourOf
import app.questling.domain.today
import app.questling.domain.weekStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = Db.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val news = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private val lock = Mutex()
    /** The day the tavern lives by; the demo moves it while seeding. */
    @Volatile var clock: () -> Long = { today() }
    /** The moment «now» (ms); the demo moves it too. */
    @Volatile var now: () -> Long = { System.currentTimeMillis() }
    /** The demo seeds quietly: no popups pile up. */
    @Volatile var quiet = false

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }
    fun say(s: String) { news.tryEmit(s) }

    // ---------- the start ----------

    /** First launch: the player's name, what they play and on what; the questling moves in. */
    suspend fun begin(player: String, kinds: Set<String>, platforms: List<String>) = lock.withLock {
        if (dao.petList().none { it.species == "questling" }) dao.insert(newPet("questling", 0).copy(hatched = clock(), growth = Pets.HATCH, shiny = false))
        val t = clock()
        settings.update { it.copy(onboarded = true, player = player.trim(), kinds = kinds, platforms = platforms, digested = t, season = Season.id(t),
            questWeek = weekStart(t), quests = Quests.deal(weekStart(t), false, kinds.any { k -> Kinds.table(k) }, false)) }
    }

    fun newPet(species: String, slot: Int): Pet {
        val sp = Pets.of(species); val names = if (Lang.ru) sp.names else sp.namesEn
        return Pet(species = species, name = names.random(), found = clock(), slot = slot, shiny = !BuildConfig.DEMO && Random.nextInt(32) == 0)
    }

    // ---------- gold, news, eggs ----------

    /** Adds gold; [play] gold (for minutes) is capped per day. Returns what was given. */
    private suspend fun gold(n: Int, play: Boolean = false): Int {
        if (n <= 0) return 0
        val t = clock(); var given = n
        settings.update { s ->
            val todayGot = if (s.goldDay == t) s.goldToday else 0
            if (play) given = given.coerceAtMost((Engine.DAILY_CAP - todayGot).coerceAtLeast(0))
            s.copy(gold = s.gold + given, goldEver = s.goldEver + given, goldDay = t, goldToday = todayGot + if (play) given else 0)
        }
        return given
    }

    private suspend fun news(items: List<String>) { if (items.isNotEmpty() && !quiet) settings.update { it.copy(inbox = it.inbox + items) } }

    /** Lays eggs (into free places by the fire first) and writes the news. */
    private suspend fun lay(species: List<String>, why: String = "") {
        if (species.isEmpty()) return
        val free = (0..2).filter { sl -> dao.petList().none { it.slot == sl } }.toMutableList()
        val laid = ArrayList<String>()
        for (sp in species.distinct()) { if (dao.petList().any { it.species == sp }) continue
            dao.insert(newPet(sp, free.removeFirstOrNull() ?: -1)); laid += sp }
        news(laid.map { "egg:$it$why" })
    }

    private suspend fun cheer(xp: Int, stat: Stat?, also: String = "") = dao.updatePets(Engine.cheer(dao.petList(), xp, stat, also))

    /** Season xp; a new quarter puts the old season away. Levels reached hand out their rewards. */
    private suspend fun seasonXp(n: Int) {
        val id = Season.id(clock())
        var s = settings.now()
        if (s.season != id) {
            settings.update { it.copy(seasons = if (it.season.isNotEmpty() && it.seasonXp > 0) it.seasons + (it.season to Season.level(it.seasonXp)) else it.seasons,
                season = id, seasonXp = 0, seasonClaimed = 0) }
            s = settings.now()
        }
        if (n <= 0) return
        val xp = s.seasonXp + n; val level = Season.level(xp)
        settings.update { it.copy(seasonXp = xp, seasonClaimed = level) }
        for (l in (s.seasonClaimed + 1)..level) {
            when (val r = Season.reward(l)) {
                is Reward.Gold -> gold(r.n)
                Reward.Egg -> Engine.anyEgg(dao.petList().map { it.species }.toSet(), l * 31 + clock().toInt())?.let { lay(listOf(it), ":season") } ?: gold(300)
                is Reward.Title -> gold(r.gold)
            }
            news(listOf("level:$l"))
        }
    }

    // ---------- games and friends ----------

    suspend fun saveGame(g: Game): Long = lock.withLock {
        val id = if (g.id == 0L) dao.insert(g.copy(added = if (g.added == 0L) clock() else g.added)) else { dao.update(g); g.id }
        afterAct(); id
    }

    suspend fun deleteGame(id: Long) = lock.withLock {
        val ids = dao.playList().filter { it.gameId == id }.map { it.id }
        ids.forEach { dao.deleteSeatsOf(it) }; dao.deletePlaysOf(id); dao.deleteGame(id)
    }

    /** Moves a game between the shelf, «playing», finished, 100% and put aside. Finishing is a feast. */
    suspend fun setStatus(g: Game, status: String) = lock.withLock { status(g, status); afterAct() }

    private suspend fun status(g: Game, status: String) {
        val cur = dao.gameList().firstOrNull { it.id == g.id } ?: return
        if (cur.status == status) return
        val done = status == Game.FINISHED || status == Game.HUNDRED
        val first = done && cur.finished == 0L
        dao.update(cur.copy(status = status, finished = if (done) (if (cur.finished == 0L) clock() else cur.finished) else 0))
        val pets = dao.petList()
        if (first) { gold(Engine.bonus(Engine.FINISH, setOf(Perk.FINISH), pets)); cheer(30, Stat.WIL, "mimic"); seasonXp(Season.QUEST); news(listOf("finished:${g.id}")) }
        if (status == Game.HUNDRED && cur.status != Game.HUNDRED) { gold(Engine.bonus(Engine.HUNDRED, setOf(Perk.FINISH), pets)); cheer(40, Stat.WIL, "phoenix"); if (!first) news(listOf("hundred:${g.id}")) }
    }

    suspend fun saveFriend(f: Friend): Long = lock.withLock {
        val id = if (f.id == 0L) dao.insert(f.copy(created = clock(), color = if (f.color == 0) dao.friendList().size % 8 + 1 else f.color)) else { dao.update(f); f.id }
        afterAct(); id
    }

    // ---------- the session running now ----------

    suspend fun startLive(gameId: Long, platform: String, friends: List<Long>) = lock.withLock {
        settings.update { it.copy(live = Live(start = now(), gameId = gameId, platform = platform, friends = friends)) }
    }
    /** A break: the clock stops. [heeded] when the pet asked for it. */
    suspend fun pauseLive(heeded: Boolean = false) = lock.withLock {
        settings.update { s -> val l = s.live; if (!l.on || l.pausedSince > 0) s else s.copy(live = l.copy(pausedSince = now(), breaks = l.breaks + 1),
            yawnsHeeded = s.yawnsHeeded + if (heeded) 1 else 0) }
    }
    suspend fun resumeLive() = lock.withLock {
        settings.update { s -> val l = s.live; if (l.pausedSince == 0L) s else s.copy(live = l.copy(pausedMs = l.pausedMs + (now() - l.pausedSince), pausedSince = 0)) }
    }
    suspend fun yawned() = lock.withLock { settings.update { it.copy(live = it.live.copy(yawned = true)) } }
    suspend fun liveFriends(friends: List<Long>) = lock.withLock { settings.update { it.copy(live = it.live.copy(friends = friends)) } }
    suspend fun liveScores(scores: Map<Long, Int>) = lock.withLock { settings.update { it.copy(live = it.live.copy(scores = scores)) } }
    suspend fun cancelLive() = lock.withLock { settings.update { it.copy(live = Live()) } }

    /** A tool used: the dice, the first-player pick, the turn timer, the score sheet. */
    suspend fun tool(id: String, n: Int = 1) = lock.withLock {
        settings.update { s -> s.copy(tools = s.tools + (id to (s.tools[id] ?: 0) + n), live = if (s.live.on) s.live.copy(tools = s.live.tools + n) else s.live) }
        afterAct()
    }

    // ---------- sessions ----------

    /** A session written down: gold by minutes and perks, food for the team, season xp, the game's status and best score. Returns its id. */
    suspend fun addPlay(p0: Play, seats: List<Seat> = emptyList()): Long = lock.withLock {
        val before = dao.playList()
        var g = dao.gameList().firstOrNull { it.id == p0.gameId }
        var p = p0
        // a new best score is a record by itself
        if (g != null && p.hasScore && p.score > g.best) {
            if (g.best > 0 && p.moment.isBlank()) p = p.copy(moment = Play.M_RECORD)
            dao.update(g.copy(best = p.score)); g = g.copy(best = p.score)
        }
        val id = dao.insert(p)
        if (seats.isNotEmpty()) dao.insertSeats(seats.map { it.copy(id = 0, playId = id) })
        val pets = dao.petList()
        val genres = g?.genreList.orEmpty()
        val mine = before.filter { it.gameId == p.gameId }
        val sameDay = before.filter { it.day == p.day }
        val hour = hourOf(p.start)
        val fresh = mine.isEmpty()
        val won = p.result == Play.WIN || p.result == Play.COOP_WIN
        val perks = buildSet {
            add(Perk.GENRE)
            if (g != null && Kinds.table(g.kind)) add(Perk.BOARD)
            if (hour >= 23 || hour < 4) add(Perk.NIGHT)
            if (p.shots.isNotBlank()) add(Perk.SHOTS)
            if (fresh) add(Perk.NEW)
            if (g?.kind == Game.MOBILE || p.platform == "Android" || p.platform == "iOS") add(Perk.MOBILE)
            if (won) add(Perk.WIN)
            if (p.minutes <= 20) add(Perk.SHORT)
            if (p.friends.isNotBlank()) add(Perk.FRIENDS)
            if (mine.isNotEmpty() && p.day - mine.maxOf { it.day } > 30) add(Perk.COMEBACK)
            if (mine.sumOf { it.minutes } >= 20 * 60) add(Perk.DEVOTED)
            if (p.platform.isNotBlank() && sameDay.any { it.platform.isNotBlank() && it.platform != p.platform }) add(Perk.PLATFORMS)
            if (p.note.isNotBlank()) add(Perk.NOTE)
        }
        gold(Engine.bonus((p.minutes / 5).coerceAtLeast(1), perks, pets, genres), play = true)
        if (sameDay.isEmpty()) gold(Engine.bonus(Engine.FIRST_OF_DAY, setOf(Perk.DAYS), pets))
        if (fresh) gold(Engine.NEW_GAME)
        if (p.friends.isNotBlank()) gold(Engine.FRIENDS)
        if (won) gold(Engine.WIN)
        if (p.breaks > 0) gold(Engine.bonus(Engine.BREAK * p.breaks.coerceAtMost(5), setOf(Perk.BREAK), pets))
        val food = Engine.food(p.minutes, sameDay.sumOf { it.minutes })
        val fed = Engine.feed(pets, food, genres, Engine.statOf(g, p), clock(), statBy = 1 + p.minutes / 45, warm = if (p.minutes >= 45) 2 else 1)
        dao.updatePets(fed.pets)
        news(fed.hatched.map { "hatch:${it.id}" })
        seasonXp(Season.xpFor(p.minutes) + (if (fresh) Season.NEW else 0) + if (p.friends.isNotBlank()) Season.FRIENDS else 0)
        if (g != null) when {
            p.moment == Play.M_HUNDRED -> status(g, Game.HUNDRED)
            p.moment == Play.M_FINISH -> status(g, Game.FINISHED)
            g.status == Game.SHELF || g.status == Game.WISH || g.status == Game.DROPPED -> dao.update(g.copy(status = Game.PLAYING))
        }
        afterAct()
        id
    }

    /** The running session ends and is written down with what the finish sheet gathered. */
    suspend fun finishLive(result: Int, score: Int, mood: Int, note: String, shots: List<String>, moment: String, minutes: Int, seats: List<Seat>): Long {
        val l = settings.now().live
        if (!l.on) return 0
        settings.update { it.copy(live = Live()) }
        val day = if (clock() != today()) clock() else dayOf(l.start)
        return addPlay(Play(gameId = l.gameId, day = day, start = l.start, minutes = minutes.coerceAtLeast(1), platform = l.platform, result = result, score = score,
            friends = l.friends.joinToString(","), mood = mood, note = note.trim(), shots = shots.joinToString("\n"), breaks = l.breaks, moment = moment, tools = l.tools,
            place = seats.firstOrNull { it.friendId == 0L }?.place ?: 0), seats)
    }

    /** An edited session can finish a quest or bring a find too. */
    suspend fun updatePlay(p: Play, seats: List<Seat>? = null) = lock.withLock {
        dao.update(p)
        if (seats != null) { dao.deleteSeatsOf(p.id); if (seats.isNotEmpty()) dao.insertSeats(seats.map { it.copy(id = 0, playId = p.id) }) }
        val g = dao.gameList().firstOrNull { it.id == p.gameId }
        if (g != null) { if (p.moment == Play.M_HUNDRED) status(g, Game.HUNDRED) else if (p.moment == Play.M_FINISH) status(g, Game.FINISHED)
            if (p.hasScore && p.score > g.best) dao.update(dao.gameList().first { it.id == g.id }.copy(best = p.score)) }
        afterAct()
    }
    suspend fun deletePlay(id: Long) = lock.withLock { dao.deleteSeatsOf(id); dao.deletePlay(id); afterAct() }

    // ---------- phone games found by the usage tracker ----------

    suspend fun foundNew(list: List<Found>, upTo: Long) = lock.withLock {
        settings.update { s -> s.copy(scanned = upTo, found = (s.found + list.filter { f -> f.pkg !in s.ignored }).distinctBy { it.pkg to it.start }.takeLast(40)) }
    }
    /** A found stretch becomes a session of the game with that package (made if it isn't there). */
    suspend fun confirmFound(f: Found) {
        val g = lock.withLock { dao.gameList().firstOrNull { it.pkg == f.pkg } ?: Game(title = f.label, kind = Game.MOBILE, platform = "Android", status = Game.PLAYING, added = clock(), pkg = f.pkg)
            .let { it.copy(id = dao.insert(it)) } }
        settings.update { s -> s.copy(found = s.found - f) }
        addPlay(Play(gameId = g.id, day = dayOf(f.start), start = f.start, minutes = f.minutes, platform = "Android", auto = true))
    }
    suspend fun dropFound(f: Found, never: Boolean) = lock.withLock {
        settings.update { s -> s.copy(found = if (never) s.found.filter { it.pkg != f.pkg } else s.found - f, ignored = if (never) s.ignored + f.pkg else s.ignored) }
    }

    // ---------- the tavern ----------

    suspend fun buy(id: String): Boolean = lock.withLock {
        val it = Shop.byId[id] ?: return@withLock false; val s = settings.now()
        if (s.gold < it.price || id in s.items) return@withLock false
        settings.update { st -> st.copy(gold = st.gold - it.price, items = st.items + id) }
        afterAct(); true
    }

    suspend fun buyEgg(): Boolean = lock.withLock {
        val s = settings.now(); if (s.gold < Shop.EGG) return@withLock false
        val sp = Engine.anyEgg(dao.petList().map { it.species }.toSet(), clock().toInt() + s.gold) ?: return@withLock false
        settings.update { it.copy(gold = it.gold - Shop.EGG) }
        lay(listOf(sp), ":shop"); true
    }

    suspend fun setSlot(p: Pet, slot: Int) = lock.withLock {
        val pets = dao.petList()
        if (slot >= 0) pets.firstOrNull { it.slot == slot && it.id != p.id }?.let { dao.update(it.copy(slot = p.slot)) }
        dao.update(dao.petList().first { it.id == p.id }.copy(slot = slot))
        afterAct()
    }
    suspend fun rename(p: Pet, name: String) = lock.withLock { if (name.isNotBlank()) dao.update(dao.petList().first { it.id == p.id }.copy(name = name.trim())) }

    // ---------- the day ----------

    private suspend fun facts() = Facts(settings.now(), dao.gameList(), dao.playList(), dao.seatList(), dao.friendList(), dao.petList(), clock())

    /** After any act: finds, quests, then trophies (twice over, as trophies count trophies). */
    private suspend fun afterAct() {
        var f = facts()
        // quests first, on their own: nothing else going wrong may hold back their gold (#3)
        val s = f.s
        val t = clock(); val monday = weekStart(t)
        val wf = WeekFacts(monday, f.plays, f.games)
        val ids = if (s.questWeek == monday && s.quests.isNotEmpty()) s.quests
            else Quests.deal(monday, f.friends.isNotEmpty(), f.games.any { Kinds.table(it.kind) }, f.byGame.values.any { l -> monday - l.maxOf { it.day } >= 24 })
        val doneBefore = if (s.questWeek == monday) s.questsDone else emptySet()
        val newly = ids.mapNotNull { Quests.byId[it] }.filter { it.id !in doneBefore && it.progress(wf) >= it.need }
        if (s.questWeek != monday || s.quests.isEmpty() || newly.isNotEmpty()) {
            settings.update { it.copy(questWeek = monday, quests = ids, questsDone = doneBefore + newly.map { q -> q.id }, questsEver = it.questsEver + newly.size) }
            news(newly.map { q -> "quest:${q.id}" })
            val pets = dao.petList()
            newly.forEach { gold(Engine.bonus(it.gold, setOf(Perk.QUEST), pets)); seasonXp(Season.QUEST) }
        }
        runCatching { f = facts(); lay(Engine.finds(f, f.pets.map { it.species }.toSet())) }.onFailure { android.util.Log.e("Questling", "finds", it) }
        repeat(3) {
            f = facts()
            val fresh = Trophies.fresh(f)
            if (fresh.isEmpty()) return
            settings.update { st -> st.copy(trophies = st.trophies + fresh.associate { tr -> tr.id to t }) }
            val pets = dao.petList()
            gold(Engine.bonus(fresh.sumOf { it.tier.gold }, setOf(Perk.TROPHY), pets))
            seasonXp(fresh.sumOf { it.tier.xp })
            cheer(fresh.size * 5, null, "coinling")
            news(listOf("trophy:" + fresh.joinToString(",") { it.id }))
            lay(Engine.finds(facts(), dao.petList().map { it.species }.toSet()))
        }
    }

    /** Daily housekeeping: the night's growth, a new season, quests. */
    /** A quest that shows as done but wasn't paid (#3): settles it now. */
    suspend fun settle() = lock.withLock { afterAct() }

    suspend fun tick() = lock.withLock {
        val s = settings.now(); val t = clock()
        if (s.digested < t) {
            val (pets, grown) = Engine.digest(dao.petList())
            dao.updatePets(pets)
            settings.update { it.copy(digested = t) }
            news(grown.map { "stage:${it.first.id}:${it.second.ordinal}" })
        }
        seasonXp(0)
        afterAct()
    }

    /** Fetches the weather for the window when it is older than half an hour; guesses the town from the time zone once. */
    suspend fun refreshOutside(force: Boolean = false) = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching {
        var s = settings.now()
        if (s.lat.isNaN() && !s.guessed) {
            val tw = OutsideSource.guess(Lang.ru)
            settings.update { it.copy(guessed = true, town = tw?.name ?: it.town, lat = tw?.lat ?: it.lat, lon = tw?.lon ?: it.lon) }
            s = settings.now()
        }
        if (s.lat.isNaN()) return@runCatching
        val old = Outside.of(s.outside)
        if (!force && old != null && System.currentTimeMillis() - old.at < 30 * 60_000L) return@runCatching
        val o = OutsideSource.fetch(s.lat, s.lon)
        settings.update { it.copy(outside = o.json()) }
    } }
}

class QuestApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as QuestApp).container
