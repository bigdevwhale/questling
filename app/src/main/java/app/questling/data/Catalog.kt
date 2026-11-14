package app.questling.data

import app.questling.domain.Genres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A search result before it becomes a game. */
data class Draft(val kind: String, val title: String, val year: Int = 0, val maker: String = "", val platform: String = "", val genres: List<String> = emptyList(), val coverUrl: String? = null)

/**
 * Looking a game up, so it comes in with its cover, year, maker and genres. Free and keyless:
 * Wikidata for the facts, the Wikipedia article's top picture for the cover. Besides the weather this is
 * the only thing that goes online, and only when someone searches.
 */
object Catalog {
    private const val AGENT = "Questling/1.0 (Android app)"
    private const val WD = "https://www.wikidata.org/w/api.php?format=json&"

    private val video = setOf("Q7889", "Q865493", "Q21125433")
    private val board = setOf("Q131436", "Q142714", "Q1191150", "Q1153190", "Q788553", "Q1515156")
    private val rpg = setOf("Q1643932", "Q2981278")

    private fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", AGENT)
        c.connectTimeout = 8_000
        c.readTimeout = 10_000
        try { if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null } finally { c.disconnect() }
    } catch (_: Exception) { null }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Null when the network failed; an empty list when nothing matched. Any kind of game comes back, with its kind guessed. */
    suspend fun search(q: String, ru: Boolean): List<Draft>? = withContext(Dispatchers.IO) {
        val query = q.trim()
        if (query.length < 2) return@withContext emptyList()
        val lang = if (Regex("[а-яё]", RegexOption.IGNORE_CASE).containsMatchIn(query)) "ru" else if (ru) "ru" else "en"
        val s = get(WD + "action=wbsearchentities&search=${enc(query)}&language=$lang&uselang=$lang&type=item&limit=20") ?: return@withContext null
        val ids = JSONObject(s).optJSONArray("search")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }.orEmpty()
        if (ids.isEmpty()) return@withContext emptyList()
        val ents = get(WD + "action=wbgetentities&ids=${ids.joinToString("|")}&props=claims|labels|sitelinks&languages=ru|en|mul&sitefilter=enwiki|ruwiki")
            ?.let { JSONObject(it).optJSONObject("entities") } ?: return@withContext null

        class Raw(val kind: String, val label: String, val year: Int, val makers: List<String>, val genres: List<String>, val plats: List<String>, val en: String?, val ruw: String?, val img: String?)
        val raws = ArrayList<Raw>()
        for (id in ids) {
            val e = ents.optJSONObject(id) ?: continue
            val c = e.optJSONObject("claims") ?: continue
            val p31 = ids(c, "P31")
            val isVideo = p31.any { it in video } || (c.has("P400") && !c.has("P1872"))
            val isRpg = p31.any { it in rpg }
            val isBoard = p31.any { it in board } || c.has("P1872")
            val kind = when { isVideo -> Game.VIDEO; isRpg -> Game.RPG; isBoard -> Game.BOARD; else -> continue }
            val labels = e.optJSONObject("labels")
            val label = listOf(lang, "mul", "en", "ru").firstNotNullOfOrNull { labels?.optJSONObject(it)?.optString("value")?.ifBlank { null } } ?: continue
            val time = c.optJSONArray("P577")?.let { first(it)?.optString("time") }
            val makers = (ids(c, if (kind == Game.VIDEO) "P178" else "P287") + ids(c, "P123")).take(1)
            val links = e.optJSONObject("sitelinks")
            raws += Raw(kind, label, time?.let { Regex("""(\d{4})""").find(it)?.value?.toIntOrNull() } ?: 0, makers, ids(c, "P136").take(6), ids(c, "P400").take(3),
                links?.optJSONObject("enwiki")?.optString("title"), links?.optJSONObject("ruwiki")?.optString("title"),
                c.optJSONArray("P18")?.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optString("value"))
            if (raws.size >= 8) break
        }
        val more = raws.flatMap { it.makers + it.genres + it.plats }.distinct().take(50)
        val names = HashMap<String, String>()
        val en = HashMap<String, String>()
        if (more.isNotEmpty()) get(WD + "action=wbgetentities&ids=${more.joinToString("|")}&props=labels&languages=$lang|en|mul")
            ?.let { JSONObject(it).optJSONObject("entities") }?.let { es ->
                for (id in more) {
                    val l = es.optJSONObject(id)?.optJSONObject("labels") ?: continue
                    listOf(lang, "mul", "en").firstNotNullOfOrNull { l.optJSONObject(it)?.optString("value") }?.let { names[id] = it }
                    listOf("en", "mul", lang).firstNotNullOfOrNull { l.optJSONObject(it)?.optString("value") }?.let { en[id] = it }
                }
            }
        coroutineScope {
            raws.map { r ->
                async {
                    Draft(r.kind, r.label, r.year, r.makers.mapNotNull { names[it] }.firstOrNull() ?: "", platformOf(r.plats.mapNotNull { en[it] }),
                        Genres.guess(r.genres.mapNotNull { en[it] }.joinToString(" "), r.kind),
                        poster(r.en, "en") ?: poster(r.ruw, "ru") ?: r.img?.let { "https://commons.wikimedia.org/wiki/Special:FilePath/${enc(it.replace(' ', '_'))}?width=400" })
                }
            }.awaitAll()
        }
    }

    /** One of the app's platforms out of Wikidata's platform names; empty when none fits. */
    private fun platformOf(names: List<String>): String {
        val t = names.joinToString(" ").lowercase()
        return when {
            t.isBlank() -> ""
            "windows" in t || "linux" in t || "macos" in t || "dos" in t -> "PC"
            "playstation" in t -> "PlayStation"
            "xbox" in t -> "Xbox"
            "switch" in t -> "Switch"
            "android" in t -> "Android"
            "ios" in t -> "iOS"
            else -> "Retro"
        }
    }

    /** The top picture of a Wikipedia article, by its exact title. */
    fun poster(title: String?, lang: String): String? {
        title ?: return null
        val body = get("https://$lang.wikipedia.org/api/rest_v1/page/summary/${enc(title.replace(' ', '_')).replace("+", "%20")}") ?: return null
        return JSONObject(body).optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
    }

    private fun first(a: JSONArray): JSONObject? = a.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")

    private fun ids(c: JSONObject, p: String): List<String> {
        val a = c.optJSONArray(p) ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")?.optString("id")?.ifBlank { null } }
    }

    /** Downloads a picture's bytes, or null. */
    suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", AGENT)
            c.connectTimeout = 8_000
            c.readTimeout = 15_000
            try { if (c.responseCode in 200..299) c.inputStream.readBytes() else null } finally { c.disconnect() }
        } catch (_: Exception) { null }
    }
}
