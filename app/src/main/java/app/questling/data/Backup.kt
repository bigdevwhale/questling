package app.questling.data

import androidx.room.withTransaction
import app.questling.AppContainer
import org.json.JSONArray
import org.json.JSONObject

/** The whole journal as one JSON file: every table and the settings. Import replaces everything. Pictures are not in it. */
object Backup {
    private fun arr(l: List<JSONObject>) = JSONArray().apply { l.forEach { put(it) } }
    private fun JSONObject.objs(k: String): List<JSONObject> = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
    private fun o(vararg kv: Pair<String, Any?>) = JSONObject().apply { kv.forEach { (k, v) -> put(k, v) } }

    suspend fun export(c: AppContainer): String {
        val d = c.dao
        return JSONObject().apply {
            put("app", "questling"); put("version", 1); put("at", System.currentTimeMillis())
            put("settings", c.settings.now().json())
            put("games", arr(d.gameList().map { o("id" to it.id, "title" to it.title, "kind" to it.kind, "platform" to it.platform, "genres" to it.genres, "cover" to it.cover, "status" to it.status,
                "added" to it.added, "finished" to it.finished, "rating" to it.rating, "note" to it.note, "pkg" to it.pkg, "year" to it.year, "maker" to it.maker, "best" to it.best) }))
            put("plays", arr(d.playList().map { o("id" to it.id, "gameId" to it.gameId, "day" to it.day, "start" to it.start, "minutes" to it.minutes, "platform" to it.platform, "result" to it.result,
                "score" to it.score, "place" to it.place, "friends" to it.friends, "mood" to it.mood, "note" to it.note, "shots" to it.shots, "breaks" to it.breaks, "auto" to it.auto,
                "moment" to it.moment, "tools" to it.tools) }))
            put("seats", arr(d.seatList().map { o("playId" to it.playId, "friendId" to it.friendId, "score" to it.score, "place" to it.place, "won" to it.won) }))
            put("friends", arr(d.friendList().map { o("id" to it.id, "name" to it.name, "color" to it.color, "created" to it.created, "archived" to it.archived, "note" to it.note) }))
            put("pets", arr(d.petList().map { o("id" to it.id, "species" to it.species, "name" to it.name, "xp" to it.xp, "growth" to it.growth, "found" to it.found, "hatched" to it.hatched,
                "slot" to it.slot, "shiny" to it.shiny, "belly" to it.belly, "str" to it.str, "wit" to it.wit, "dex" to it.dex, "cha" to it.cha, "wil" to it.wil, "lck" to it.lck, "genres" to it.genres) }))
        }.toString(1)
    }

    /** Reads a backup; false when the file isn't one. */
    suspend fun import(c: AppContainer, text: String): Boolean {
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (j.optString("app") != "questling") return false
        val d = c.dao
        c.db.withTransaction {
            d.wipeGames(); d.wipePlays(); d.wipeSeats(); d.wipeFriends(); d.wipePets()
            j.objs("games").forEach { d.insert(Game(it.getLong("id"), it.getString("title"), it.optString("kind", Game.VIDEO), it.optString("platform"), it.optString("genres"), it.optString("cover"),
                it.optString("status", Game.PLAYING), it.optLong("added"), it.optLong("finished"), it.optInt("rating"), it.optString("note"), it.optString("pkg"), it.optInt("year"), it.optString("maker"), it.optInt("best"))) }
            j.objs("plays").forEach { d.insert(Play(it.getLong("id"), it.getLong("gameId"), it.getLong("day"), it.optLong("start"), it.getInt("minutes"), it.optString("platform"), it.optInt("result"),
                it.optInt("score", Int.MIN_VALUE), it.optInt("place"), it.optString("friends"), it.optInt("mood"), it.optString("note"), it.optString("shots"), it.optInt("breaks"), it.optBoolean("auto"),
                it.optString("moment"), it.optInt("tools"))) }
            d.insertSeats(j.objs("seats").map { Seat(0, it.getLong("playId"), it.getLong("friendId"), it.optInt("score", Int.MIN_VALUE), it.optInt("place"), it.optBoolean("won")) })
            j.objs("friends").forEach { d.insert(Friend(it.getLong("id"), it.getString("name"), it.optInt("color"), it.optLong("created"), it.optBoolean("archived"), it.optString("note"))) }
            j.objs("pets").forEach { d.insert(Pet(it.getLong("id"), it.getString("species"), it.getString("name"), it.optInt("xp"), it.optInt("growth"), it.optLong("found"), it.optLong("hatched"),
                it.optInt("slot", -1), it.optBoolean("shiny"), it.optInt("belly"), it.optInt("str"), it.optInt("wit"), it.optInt("dex"), it.optInt("cha"), it.optInt("wil"), it.optInt("lck"), it.optString("genres"))) }
        }
        j.optJSONObject("settings")?.let { s -> val st = Settings.of(s); c.settings.update { st.copy(onboarded = true, live = Live(), found = emptyList()) } }
        return true
    }
}
