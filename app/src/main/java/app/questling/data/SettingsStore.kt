package app.questling.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * A session being played right now: kept here so it survives the app being closed.
 * [pausedMs] time spent on breaks, [pausedSince] a break running since (0 = playing).
 * [scores] the scoresheet as «friendId:score» (0 = the player).
 */
data class Live(
    val start: Long = 0,                // 0 = nothing running
    val gameId: Long = 0,
    val platform: String = "",
    val friends: List<Long> = emptyList(),
    val pausedMs: Long = 0,
    val pausedSince: Long = 0,
    val breaks: Int = 0,
    val scores: Map<Long, Int> = emptyMap(),
    val tools: Int = 0,
    val yawned: Boolean = false,        // the pet has asked for a break already
) {
    val on get() = start > 0
    fun played(now: Long): Long = (now - start - pausedMs - if (pausedSince > 0) now - pausedSince else 0).coerceAtLeast(0)
    fun json(): JSONObject = JSONObject().put("start", start).put("gameId", gameId).put("platform", platform).put("friends", JSONArray(friends))
        .put("pausedMs", pausedMs).put("pausedSince", pausedSince).put("breaks", breaks).put("tools", tools).put("yawned", yawned)
        .put("scores", JSONObject().apply { scores.forEach { (k, v) -> put(k.toString(), v) } })
    companion object {
        fun of(j: JSONObject?): Live = if (j == null) Live() else Live(j.optLong("start"), j.optLong("gameId"), j.optString("platform"),
            j.optJSONArray("friends")?.let { a -> (0 until a.length()).map { a.getLong(it) } } ?: emptyList(),
            j.optLong("pausedMs"), j.optLong("pausedSince"), j.optInt("breaks"),
            j.optJSONObject("scores")?.let { o -> o.keys().asSequence().associate { it.toLong() to o.getInt(it) } } ?: emptyMap(),
            j.optInt("tools"), j.optBoolean("yawned"))
    }
}

/** A stretch of a phone game found by the usage tracker, waiting to be confirmed. */
data class Found(val pkg: String, val label: String, val start: Long, val minutes: Int) {
    fun json(): JSONObject = JSONObject().put("pkg", pkg).put("label", label).put("start", start).put("minutes", minutes)
    companion object { fun of(j: JSONObject) = Found(j.optString("pkg"), j.optString("label"), j.optLong("start"), j.optInt("minutes")) }
}

/**
 * Everything that isn't a record or a creature: the player, gold, the tavern's things, quests, the season,
 * trophies and the session running now.
 */
data class Settings(
    val onboarded: Boolean = false,
    val player: String = "",                     // a name for the cards
    val kinds: Set<String> = emptySet(),         // what the player plays: video, board…
    val platforms: List<String> = emptyList(),   // the player's own platforms, in their order
    val gold: Int = 0,
    val goldEver: Int = 0,
    val items: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val theme: String = "system",
    val inbox: List<String> = emptyList(),       // news to show once: «egg:slime», «hatch:12», «stage:12:2», «quest:x», «trophy:x», «level:7»
    val seen: Set<String> = emptySet(),
    val questWeek: Long = 0,
    val quests: List<String> = emptyList(),
    val questsDone: Set<String> = emptySet(),
    val questsEver: Int = 0,
    val trophies: Map<String, Long> = emptyMap(), // id → day earned
    val season: String = "",                     // «2026-4»: the quarter the season xp counts for
    val seasonXp: Int = 0,
    val seasonClaimed: Int = 0,                  // levels whose rewards were handed out
    val seasons: Map<String, Int> = emptyMap(),  // past seasons' levels
    val title: String = "",                      // a title the player wears (from trophies or seasons)
    val digested: Long = 0,                      // the last day the creatures' bellies turned into growth
    val goldDay: Long = 0, val goldToday: Int = 0,
    val live: Live = Live(),
    val breakAfter: Int = 90,                    // minutes before the pet yawns, 0 off
    val shotsAuto: Boolean = true,               // pick up screenshots made during a session
    val tracker: Boolean = false,                // phone games found by usage access
    val scanned: Long = 0,                       // usage scanned up to (ms)
    val found: List<Found> = emptyList(),
    val ignored: Set<String> = emptySet(),       // packages the player said are not games
    val tools: Map<String, Int> = emptyMap(),    // dice, first, timer, score: times used
    val town: String = "", val lat: Double = Double.NaN, val lon: Double = Double.NaN,
    val guessed: Boolean = false,
    val outside: String = "",
    val cardBg: Int = 0,
    val feasts: Set<String> = emptySet(),
    val yawnsHeeded: Int = 0,                    // breaks taken when the pet yawned
    val dice: String = "1d20",
    val turn: Int = 60,                          // turn timer seconds
) {
    fun json(): JSONObject = JSONObject().apply {
        put("onboarded", onboarded); put("player", player); put("kinds", JSONArray(kinds.toList())); put("platforms", JSONArray(platforms))
        put("gold", gold); put("goldEver", goldEver)
        put("items", JSONArray(items.toList())); put("hidden", JSONArray(hidden.toList())); put("theme", theme); put("inbox", JSONArray(inbox))
        put("seen", JSONArray(seen.toList())); put("questWeek", questWeek); put("quests", JSONArray(quests)); put("questsDone", JSONArray(questsDone.toList())); put("questsEver", questsEver)
        put("trophies", JSONObject().apply { trophies.forEach { (k, v) -> put(k, v) } })
        put("season", season); put("seasonXp", seasonXp); put("seasonClaimed", seasonClaimed)
        put("seasons", JSONObject().apply { seasons.forEach { (k, v) -> put(k, v) } }); put("title", title)
        put("digested", digested); put("goldDay", goldDay); put("goldToday", goldToday)
        put("live", live.json()); put("breakAfter", breakAfter); put("shotsAuto", shotsAuto); put("tracker", tracker); put("scanned", scanned)
        put("found", JSONArray().apply { found.forEach { put(it.json()) } }); put("ignored", JSONArray(ignored.toList()))
        put("tools", JSONObject().apply { tools.forEach { (k, v) -> put(k, v) } })
        put("town", town); if (!lat.isNaN()) { put("lat", lat); put("lon", lon) }; put("guessed", guessed); put("outside", outside)
        put("cardBg", cardBg); put("feasts", JSONArray(feasts.toList())); put("yawnsHeeded", yawnsHeeded); put("dice", dice); put("turn", turn)
    }

    companion object {
        fun of(j: JSONObject): Settings {
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            fun set(k: String) = list(k).toSet()
            fun ints(k: String) = j.optJSONObject(k)?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } } ?: emptyMap()
            return Settings(
                onboarded = j.optBoolean("onboarded"), player = j.optString("player"), kinds = set("kinds"), platforms = list("platforms"),
                gold = j.optInt("gold"), goldEver = j.optInt("goldEver"),
                items = set("items"), hidden = set("hidden"), theme = j.optString("theme", "system"), inbox = list("inbox"), seen = set("seen"),
                questWeek = j.optLong("questWeek"), quests = list("quests"), questsDone = set("questsDone"), questsEver = j.optInt("questsEver"),
                trophies = j.optJSONObject("trophies")?.let { o -> o.keys().asSequence().associateWith { o.getLong(it) } } ?: emptyMap(),
                season = j.optString("season"), seasonXp = j.optInt("seasonXp"), seasonClaimed = j.optInt("seasonClaimed"), seasons = ints("seasons"),
                title = j.optString("title"), digested = j.optLong("digested"), goldDay = j.optLong("goldDay"), goldToday = j.optInt("goldToday"),
                live = Live.of(j.optJSONObject("live")), breakAfter = j.optInt("breakAfter", 90), shotsAuto = j.optBoolean("shotsAuto", true),
                tracker = j.optBoolean("tracker"), scanned = j.optLong("scanned"),
                found = j.optJSONArray("found")?.let { a -> (0 until a.length()).map { Found.of(a.getJSONObject(it)) } } ?: emptyList(),
                ignored = set("ignored"), tools = ints("tools"),
                town = j.optString("town"), lat = j.optDouble("lat", Double.NaN), lon = j.optDouble("lon", Double.NaN), guessed = j.optBoolean("guessed"),
                outside = j.optString("outside"), cardBg = j.optInt("cardBg"), feasts = set("feasts"), yawnsHeeded = j.optInt("yawnsHeeded"),
                dice = j.optString("dice", "1d20"), turn = j.optInt("turn", 60))
        }
    }
}

private val Context.store by preferencesDataStore("questling")
private val KEY = stringPreferencesKey("settings")

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.store.data.map { p -> p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings() }
    suspend fun now(): Settings = settings.first()
    suspend fun update(f: (Settings) -> Settings) { ctx.store.edit { p ->
        val cur = p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings()
        p[KEY] = f(cur).json().toString() } }
}
