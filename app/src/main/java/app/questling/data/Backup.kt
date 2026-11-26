package app.questling.data

import androidx.room.withTransaction
import app.questling.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole journal as one file, to keep or to carry to another phone: a zip with `questling.json` (every table and
 * the settings), the games' covers and the sessions' screenshots and photos. A bare JSON file, as the first versions
 * saved (without pictures), is read too. Import replaces everything.
 */
object Backup {
    private const val JSON = "questling.json"
    private const val COVERS = "covers"
    private const val PHOTOS = "photos"
    fun fileName() = "questling-${LocalDate.now()}.zip"

    /** Writes the journal into [out] and closes it. Only the pictures the journal still points at go in. */
    suspend fun write(c: AppContainer, out: OutputStream) {
        val text = export(c)
        val covers = c.dao.gameList().map { it.cover }.filter { it.isNotBlank() }.toSet()
        val shots = c.dao.playList().flatMap { it.shotList }.toSet()
        withContext(Dispatchers.IO) {
            ZipOutputStream(out.buffered()).use { z ->
                z.putNextEntry(ZipEntry(JSON)); z.write(text.toByteArray()); z.closeEntry()
                fun put(dir: File, folder: String, names: Set<String>) = names.forEach { n ->
                    val f = File(dir, File(n).name); if (!f.isFile) return@forEach
                    z.putNextEntry(ZipEntry("$folder/${f.name}")); f.inputStream().use { it.copyTo(z) }; z.closeEntry() }
                put(Media.covers(c.app), COVERS, covers)
                put(Media.shots(c.app), PHOTOS, shots)
            }
        }
    }

    /**
     * Reads a journal from [input] – a zip or a bare JSON – and closes it; false when the file isn't one of ours,
     * and nothing changes then.
     */
    suspend fun read(c: AppContainer, input: InputStream): Boolean {
        val fresh = File(c.app.cacheDir, "restore")
        val text = withContext(Dispatchers.IO) { runCatching { input.use { unpack(it, fresh) } }.getOrNull() }
        val ok = text != null && import(c, text)
        withContext(Dispatchers.IO) {
            // a zip's pictures take the place of the old ones; a bare JSON brings none, and those here stay for it
            if (ok && fresh.exists()) for ((folder, dir) in listOf(COVERS to Media.covers(c.app), PHOTOS to Media.shots(c.app))) {
                dir.listFiles()?.forEach { it.delete() }
                File(fresh, folder).listFiles()?.forEach { it.copyTo(File(dir, it.name), overwrite = true) }
            }
            fresh.deleteRecursively()
        }
        return ok
    }

    /** The JSON out of [input]; a zip's pictures are put under [into] on the way. */
    private fun unpack(input: InputStream, into: File): String? {
        into.deleteRecursively()
        val src = BufferedInputStream(input)
        src.mark(4); val head = ByteArray(2); val n = src.read(head); src.reset()
        if (n < 2 || head[0] != 'P'.code.toByte() || head[1] != 'K'.code.toByte()) return src.readBytes().decodeToString()
        var text: String? = null
        into.mkdirs()   // a zip always stands for its pictures, even when it has none
        ZipInputStream(src).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val name = e.name.substringAfterLast('/')   // only the file's own name: nothing is written outside the folders
                val folder = e.name.substringBefore('/', "")
                when {
                    e.isDirectory || name.isBlank() || name == "." || name == ".." -> {}
                    e.name == JSON -> text = z.readBytes().decodeToString()
                    folder == COVERS || folder == PHOTOS -> File(into, folder).apply { mkdirs() }.let { d -> File(d, name).outputStream().use { z.copyTo(it) } }
                }
            }
        }
        return text
    }

    private fun arr(l: List<JSONObject>) = JSONArray().apply { l.forEach { put(it) } }
    private fun JSONObject.objs(k: String): List<JSONObject> = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
    private fun o(vararg kv: Pair<String, Any?>) = JSONObject().apply { kv.forEach { (k, v) -> put(k, v) } }

    suspend fun export(c: AppContainer): String {
        val d = c.dao
        return JSONObject().apply {
            put("app", "questling"); put("version", 1); put("at", System.currentTimeMillis())
            put("settings", c.settings.now().json())
            put("games", arr(d.gameList().map { o("id" to it.id, "title" to it.title, "kind" to it.kind, "platform" to it.platform, "genres" to it.genres, "cover" to it.cover, "status" to it.status,
                "added" to it.added, "finished" to it.finished, "rating" to it.rating, "note" to it.note, "pkg" to it.pkg, "year" to it.year, "maker" to it.maker, "best" to it.best, "earlier" to it.earlier) }))
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
                it.optString("status", Game.PLAYING), it.optLong("added"), it.optLong("finished"), it.optInt("rating"), it.optString("note"), it.optString("pkg"), it.optInt("year"), it.optString("maker"), it.optInt("best"), it.optInt("earlier"))) }
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
