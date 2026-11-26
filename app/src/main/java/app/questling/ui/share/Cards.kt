package app.questling.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.questling.R
import app.questling.data.Game
import app.questling.data.Media
import app.questling.data.Pet
import app.questling.data.Play
import app.questling.domain.Genres
import app.questling.domain.Kinds
import app.questling.domain.Pets
import app.questling.domain.Season
import app.questling.domain.Shop
import app.questling.domain.Span
import app.questling.domain.Stage
import app.questling.domain.Stat
import app.questling.domain.Stats
import app.questling.domain.Tier
import app.questling.domain.Trophies
import app.questling.domain.dur
import app.questling.domain.fmtDayShort
import app.questling.domain.fmtDayYear
import app.questling.domain.klass
import app.questling.domain.monthName
import app.questling.domain.speciality
import app.questling.domain.stage
import app.questling.domain.title
import app.questling.domain.tr
import app.questling.domain.weekStart
import app.questling.ui.Data
import app.questling.ui.art.Art
import app.questling.ui.art.Pose
import app.questling.ui.art.frameOf
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

enum class CardKind(val ru: String, val en: String) {
    WEEK("Неделя", "Week"), MONTH("Месяц", "Month"), YEAR("Год", "Year"), SESSION("Сессия", "Session"), GAME("Игра", "Game"),
    PET("Питомец", "Creature"), TAVERN("Трактир", "Tavern"), COLLECTION("Коллекция", "Collection"), TROPHIES("Трофеи", "Trophies");
    val label get() = tr(ru, en)
}

/** What to draw: a card kind and what it is about (a week's Monday, a session, a game, a creature, a year). */
data class ShareReq(val kind: CardKind, val id: Long = 0)

/** How a card looks: the format, the background, whether names of friends are hidden. */
data class Look(val story: Boolean = false, val bg: Int = 0, val hide: Boolean = false)

/** Backgrounds: two colours top to bottom and whether the text is light; -1 is the tavern picture. */
val BACKS = listOf(
    Triple(0xFF7A2A20.toInt(), 0xFF24100C.toInt(), true), Triple(0xFF1E4A4A.toInt(), 0xFF0C1A1C.toInt(), true), Triple(0xFF3A2F60.toInt(), 0xFF140E28.toInt(), true),
    Triple(0xFFF4EBDD.toInt(), 0xFFE3D0B0.toInt(), false), Triple(0xFFF2D27A.toInt(), 0xFFC98A3A.toInt(), false), Triple(-1, -1, true),
)

class Painter(private val ctx: Context, private val d: Data, private val look: Look) {
    private val W = 1080; private val H = if (look.story) 1920 else 1350
    private val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.lora_medium) ?: Typeface.DEFAULT_BOLD
    private val sans: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD
    private val reg: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_regular) ?: Typeface.DEFAULT
    private val num: Typeface = ResourcesCompat.getFont(ctx, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD
    private val back = BACKS[look.bg.coerceIn(0, BACKS.lastIndex)]
    private val light = back.third
    private val ink = if (light) 0xFFFFFFFF.toInt() else 0xFF2B1F1A.toInt()
    private val sub = if (light) 0xC8FFFFFF.toInt() else 0xA02B1F1A.toInt()
    private val accent = if (light) 0xFFFFD36B.toInt() else 0xFFB5402F.toInt()
    private val panel = if (light) 0x24FFFFFF else 0x142B1F1A
    private val top = if (look.story) 300f else 200f
    private val bottom get() = H - 150f

    private fun p(color: Int = ink) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun base(kicker: String): Pair<Bitmap, Canvas> {
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        val bg = Art.get(ctx, "tavern_bg")?.asAndroidBitmap()
        if (back.first == -1 && bg != null) {
            val s = H.toFloat() / bg.height; val w = bg.width * s
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p(0xFF2A1A12.toInt()))
            c.drawBitmap(bg, Rect(0, 0, bg.width, bg.height), RectF((W - w) / 2, 0f, (W + w) / 2, H.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xA01A0E08.toInt(), 0xE61A0E08.toInt(), Shader.TileMode.CLAMP) })
        } else {
            val a = if (back.first == -1) 0xFF4A2E1E.toInt() else back.first; val z = if (back.first == -1) 0xFF1A100A.toInt() else back.second
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, W * .4f, H.toFloat(), a, z, Shader.TileMode.CLAMP) })
            // sparks from the hearth
            val spark = p(if (light) 0x55FFE0A0 else 0x33B5402F)
            for (k in 0 until 40) { val x = (k * 397 % 1000) / 1000f * W; val y = (k * 613 % 1000) / 1000f * H; c.drawCircle(x, y, 2f + (k % 3) * 1.5f, spark) }
            for ((x, y, r) in listOf(Triple(.92f, .08f, .42f), Triple(.05f, .62f, .36f), Triple(.8f, .95f, .3f)))
                c.drawCircle(W * x, H * y, W * r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W * x, H * y, W * r, if (light) 0x28FFFFFF else 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP) })
        }
        val hy = if (look.story) 130f else 70f
        c.drawRoundRect(RectF(60f, hy, 60f + 310f, hy + 64f), 32f, 32f, p(panel))
        Art.prop(ctx, "d20")?.asAndroidBitmap()?.let { img -> val s = 48f / max(img.width, img.height)
            c.drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(76f, hy + 8f, 76f + img.width * s, hy + 8f + img.height * s), Paint(Paint.FILTER_BITMAP_FLAG)) }
        c.t("Questling", 136f, hy + 45f, 34f, bold)
        c.t(kicker.uppercase(), W - 60f, hy + 44f, 30f, sans, sub, Paint.Align.RIGHT, maxW = W - 450f)
        return b to c
    }

    private fun Canvas.t(s: String, x: Float, y: Float, size: Float, face: Typeface = bold, color: Int = ink, align: Paint.Align = Paint.Align.LEFT, maxW: Float = when (align) { Paint.Align.CENTER -> W - 120f; Paint.Align.RIGHT -> x - 60f; else -> W - x - 60f }) {
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = face; this.color = color; textAlign = align }
        var str = s; if (pt.measureText(str) > maxW) { while (pt.measureText("$str…") > maxW && str.length > 2) str = str.dropLast(1); str += "…" }
        drawText(str, x, y, pt)
    }
    /** Text broken into lines of [maxW]; returns the y after the last line. */
    private fun Canvas.para(s: String, x: Float, y0: Float, size: Float, maxW: Float, lines: Int, face: Typeface = reg, color: Int = sub): Float {
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = face; this.color = color }
        var y = y0; var line = ""; var n = 0
        for (w in s.replace('\n', ' ').split(' ')) {
            if (pt.measureText("$line $w") > maxW && line.isNotEmpty()) { n++; drawText(if (n == lines) "$line…" else line, x, y, pt); y += size * 1.3f; line = w; if (n == lines) return y } else line = if (line.isEmpty()) w else "$line $w"
        }
        if (line.isNotEmpty()) { drawText(line, x, y, pt); y += size * 1.3f }
        return y
    }
    private fun Canvas.panel(x: Float, y: Float, w: Float, h: Float, r: Float = 36f, color: Int = panel) = drawRoundRect(RectF(x, y, x + w, y + h), r, r, p(color))
    private fun Canvas.facts(items: List<Pair<String, String>>, y: Float, h: Float = 170f) {
        val n = items.size; val gap = 18f; val w = (W - 120f - gap * (n - 1)) / n
        items.forEachIndexed { i, (v, l) -> val x = 60f + i * (w + gap); panel(x, y, w, h)
            t(v, x + w / 2, y + h * .52f, if (v.length > 8) 40f else 52f, num, ink, Paint.Align.CENTER, w - 24); t(l, x + w / 2, y + h * .8f, 27f, reg, sub, Paint.Align.CENTER, w - 24) }
    }
    private fun Canvas.bitmap(img: Bitmap, cx: Float, foot: Float, size: Float, shadow: Boolean = true) {
        val s = min(size / img.width, size / img.height); val w = img.width * s; val h = img.height * s
        if (shadow) drawOval(RectF(cx - w * .36f, foot - h * .04f, cx + w * .36f, foot + h * .04f), p(0x33000000))
        drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(cx - w / 2, foot - h, cx + w / 2, foot), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
    private fun Canvas.pet(p: Pet?, pose: Pose, cx: Float, foot: Float, size: Float) {
        p ?: return
        val st = p.stage()
        val img = (if (st == Stage.EGG) Art.egg(ctx, p.species) else Art.pet(ctx, p.species, frameOf(st, pose)))?.asAndroidBitmap()
        val scale = when (st) { Stage.EGG -> .7f; Stage.BABY -> .62f; Stage.TEEN -> .8f; else -> .96f }
        if (img == null) { val r = size * scale * .36f; drawOval(RectF(cx - r, foot - r * 1.7f, cx + r, foot), p(Pets.of(p.species).color.toInt())); return }
        bitmap(img, cx, foot, size * scale)
    }
    private fun Canvas.prop(id: String, cx: Float, foot: Float, size: Float) { Art.prop(ctx, id)?.asAndroidBitmap()?.let { bitmap(it, cx, foot, size, false) } }
    private fun Canvas.footer() = t(tr("Questling · трактир, где питомцы растут от ваших игр", "Questling · a tavern where creatures grow on the games you play"), W / 2f, H - 64f, 28f, reg, sub, Paint.Align.CENTER)
    private fun Canvas.bars(rows: List<Triple<String, Int, Int>>, y0: Float, maxRows: Int): Float {
        var y = y0; val max = (rows.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        rows.take(maxRows).forEach { (name, v, col) ->
            panel(60f, y, W - 120f, 92f, 28f)
            drawRoundRect(RectF(60f, y, 60f + (W - 120f) * v / max, y + 92f), 28f, 28f, p((col and 0xFFFFFF) or 0x66000000))
            t(name, 96f, y + 58, 38f, sans, ink, maxW = 600f)
            t(dur(v * 60), W - 96f, y + 58, 36f, num, accent, Paint.Align.RIGHT)
            y += 108
        }
        return y
    }
    private fun gameColor(g: Game?): Int = (g?.genreList?.firstOrNull()?.let { Genres.of(it)?.color } ?: Kinds.color(g?.kind ?: "")).toInt()

    /** A game's cover in [r], or a box in its colour with the title's letters. */
    private fun Canvas.cover(g: Game?, r: RectF, rad: Float = 28f) {
        save(); clipPath(Path().apply { addRoundRect(r, rad, rad, Path.Direction.CW) })
        val img = g?.cover?.let { Media.coverBitmap(ctx, it) }
        if (img != null) { val s = max(r.width() / img.width, r.height() / img.height); val w = img.width * s; val h = img.height * s
            drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(r.centerX() - w / 2, r.centerY() - h / 2, r.centerX() + w / 2, r.centerY() + h / 2), Paint(Paint.FILTER_BITMAP_FLAG)) }
        else { val col = gameColor(g)
            drawRect(r, Paint().apply { shader = LinearGradient(r.left, r.top, r.right, r.bottom, col, (col and 0xFFFFFF) or 0x99000000.toInt(), Shader.TileMode.CLAMP) })
            drawRect(r, p(0x33000000))
            val letters = g?.title.orEmpty().split(' ', '-', ':').filter { it.isNotBlank() }.take(3).joinToString("") { it.first().uppercase() }
            t(letters, r.centerX(), r.centerY() + r.width() * .12f, r.width() * .34f, bold, 0xFFFFFFFF.toInt(), Paint.Align.CENTER, r.width()) }
        restore()
    }

    /** The tavern in [r]: the room, the window, the things and the creatures. */
    private fun Canvas.tavern(r: RectF) {
        save(); clipPath(Path().apply { addRoundRect(r, 40f, 40f, Path.Direction.CW) })
        app.questling.ui.art.drawSky(this, r, app.questling.ui.art.Sky.of(d.s), .3f)
        val bg = Art.get(ctx, "tavern_bg")?.asAndroidBitmap()
        if (bg != null) drawBitmap(bg, Rect(0, 0, bg.width, bg.height), r, Paint(Paint.FILTER_BITMAP_FLAG))
        else { drawRect(r.left, r.top, r.right, r.top + r.height() * .68f, p(0xFF6A4630.toInt())); drawRect(r.left, r.top + r.height() * .68f, r.right, r.bottom, p(0xFF4A2E1E.toInt())) }
        val items = d.s.items - d.s.hidden
        for (it in Shop.all.filter { it.id in items }.sortedBy { if (it.wall) 0f else it.y }) {
            val img = Art.item(ctx, it.id)?.asAndroidBitmap() ?: continue
            val w = r.width() * it.w; val h = w * img.height / img.width
            val x = r.left + r.width() * it.x - w / 2; val y = r.top + if (it.wall) r.height() * it.y - h / 2 else r.height() * it.y - h
            drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(x, y, x + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val h = r.height(); val w = r.width()
        d.team.filter { it.slot in 1..2 }.forEach { pt -> val s = h * .27f; pet(pt, Pose.IDLE, r.left + w * (if (pt.slot == 1) .18f else .62f) + s / 2, r.top + h * .97f, s) }
        d.leader?.let { pet(it, Pose.CHEER, r.centerX(), r.top + h * .99f, h * .38f) }
        app.questling.ui.art.drawNight(this, r, app.questling.ui.art.Sky.of(d.s), .5f)
        restore()
    }

    fun render(r: ShareReq): Pair<Bitmap, String> = when (r.kind) {
        CardKind.WEEK -> { val m = weekStart(if (r.id > 0) r.id else d.today); span(Span(m, m + 6, ""), tr("Неделя в играх", "A week in games"), "${fmtDayShort(m)} – ${fmtDayShort(m + 6)}") }
        CardKind.MONTH -> { val m = LocalDate.ofEpochDay(if (r.id > 0) r.id else d.today).withDayOfMonth(1)
            span(Span(m.toEpochDay(), m.plusMonths(1).toEpochDay() - 1, ""), monthName(m.monthValue).replaceFirstChar { it.uppercase() }, "${m.year}") }
        CardKind.YEAR -> { val y = if (r.id > 0) r.id.toInt() else LocalDate.ofEpochDay(d.today).year
            span(Span(LocalDate.of(y, 1, 1).toEpochDay(), LocalDate.of(y, 12, 31).toEpochDay(), ""), tr("Год в играх", "The year in games"), "$y") }
        CardKind.SESSION -> session(r.id); CardKind.GAME -> game(r.id); CardKind.PET -> pet(r.id)
        CardKind.TAVERN -> tavernCard(); CardKind.COLLECTION -> collection(); CardKind.TROPHIES -> trophies()
    }

    private fun span(sp: Span, title: String, kicker: String): Pair<Bitmap, String> {
        val (b, c) = base(kicker)
        val st = Stats(d.plays, sp, d.gameById, d.today)
        var y = top + 60
        c.t(title, 60f, y + 30, 80f); y += 90
        c.facts(listOf(dur(st.minutes * 60) to tr("наиграно", "played"), "${st.count}" to tr("сессий", "sessions"), "${st.gamesPlayed}" to tr("игр", "games")), y, 160f)
        y += 200
        y = c.bars(st.byGame().map { Triple(it.key?.title ?: "–", it.minutes, gameColor(it.key)) }, y, if (look.story) 6 else 4)
        val top = st.byGenre().firstOrNull()
        if (top != null) { c.t(tr("Жанр: ${top.key.title}", "Genre: ${top.key.title}") + if (st.finished > 0) tr(" · пройдено игр: ${st.finished}", " · games finished: ${st.finished}") else "", 60f, y + 40, 36f, sans, sub); y += 70 }
        val room = bottom - y
        if (room > 130) c.pet(d.leader, Pose.CHEER, W / 2f, bottom + 30, min(room + 30, 440f))
        c.footer()
        return b to tr("Мои игры в Questling", "My games in Questling")
    }

    private fun session(id: Long): Pair<Bitmap, String> {
        val pl = d.plays.firstOrNull { it.id == id } ?: d.plays.firstOrNull(); val (b, c) = base(tr("сессия", "session")); if (pl == null) return b to ""
        val g = d.gameById[pl.gameId]
        var y = top + 30
        val cw = if (look.story) 420f else 300f; val ch = cw * 1.34f
        c.cover(g, RectF(60f, y, 60f + cw, y + ch))
        val x = 60f + cw + 40f
        c.t(g?.title ?: "–", x, y + 70, 60f, maxW = W - x - 60f)
        c.t(fmtDayYear(pl.day), x, y + 124, 34f, reg, sub)
        c.t(dur(pl.minutes * 60), x, y + 230, 84f, num, accent)
        val tags = listOfNotNull(Kinds.result(pl.result).takeIf { it.isNotBlank() }, Kinds.moment(pl.moment).takeIf { it.isNotBlank() }, pl.platform.takeIf { it.isNotBlank() }?.let { Kinds.platformTitle(it) })
        var ty = y + 300; tags.take(3).forEach { c.t(it, x, ty, 36f, sans); ty += 52 }
        y += ch + 50
        val friends = pl.friendIds.mapNotNull { d.friendById[it] }
        if (friends.isNotEmpty()) { c.t(if (look.hide) tr("Играли: ${friends.size + 1}", "Players: ${friends.size + 1}") else tr("С кем: ", "With: ") + friends.joinToString(", ") { it.name }, 60f, y + 30, 38f, sans); y += 70 }
        if (pl.note.isNotBlank()) y = c.para("«${pl.note}»", 60f, y + 40, 36f, W - 120f, if (look.story) 6 else 3) + 10
        // a strip of the session's pictures
        val shots = pl.shotList.take(3).mapNotNull { Media.shotBitmap(ctx, it, 600) }
        if (shots.isNotEmpty() && bottom - y > 240) { val gap = 16f; val sw = (W - 120f - gap * (shots.size - 1)) / shots.size; val sh = min(bottom - y - 20, sw * .62f)
            shots.forEachIndexed { i, img -> val r = RectF(60f + i * (sw + gap), y + 10, 60f + i * (sw + gap) + sw, y + 10 + sh)
                c.save(); c.clipPath(Path().apply { addRoundRect(r, 24f, 24f, Path.Direction.CW) })
                val s = max(r.width() / img.width, r.height() / img.height); val w = img.width * s; val h = img.height * s
                c.drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(r.centerX() - w / 2, r.centerY() - h / 2, r.centerX() + w / 2, r.centerY() + h / 2), Paint(Paint.FILTER_BITMAP_FLAG)); c.restore() } }
        else if (bottom - y > 240) c.pet(d.leader, Pose.CHEER, W / 2f, bottom, min(bottom - y, 400f))
        c.footer()
        return b to (g?.title ?: "Questling")
    }

    private fun game(id: Long): Pair<Bitmap, String> {
        val g = d.gameById[id] ?: d.recent.firstOrNull(); val (b, c) = base(when (g?.status) { Game.HUNDRED -> tr("пройдена на 100%", "100% done"); Game.FINISHED -> tr("пройдена", "finished"); else -> tr("игра", "game") })
        if (g == null) return b to ""
        val plays = d.byGame[g.id].orEmpty(); val minutes = d.minutes(g.id)
        val cw = if (look.story) 520f else 380f; val ch = cw * 1.34f
        var y = top + 20
        c.cover(g, RectF((W - cw) / 2, y, (W + cw) / 2, y + ch), 36f)
        y += ch + 90
        c.t(g.title, W / 2f, y, 72f, bold, ink, Paint.Align.CENTER)
        c.t(listOfNotNull(Kinds.title(g.kind), g.genreList.firstOrNull()?.let { Genres.of(it)?.title }, g.year.takeIf { it > 0 }?.toString()).joinToString(" · "), W / 2f, y + 56, 34f, reg, sub, Paint.Align.CENTER)
        y += 100
        c.facts(listOfNotNull(dur(minutes * 60) to tr("наиграно", "played"), "${plays.size}" to tr("сессий", "sessions"),
            if (g.rating > 0) "${g.rating}/10" to tr("моя оценка", "my rating") else "${plays.map { it.day }.toSet().size}" to tr("дней", "days")), y, 160f)
        if (g.finished > 0) c.t(tr("Титры – ${fmtDayYear(g.finished)}", "Credits – ${fmtDayYear(g.finished)}"), W / 2f, y + 220, 34f, sans, accent, Paint.Align.CENTER)
        c.footer()
        return b to g.title
    }

    private fun pet(id: Long): Pair<Bitmap, String> {
        val pt = d.pets.firstOrNull { it.id == id } ?: d.leader; val (b, c) = base(tr("мой питомец", "my creature")); if (pt == null) return b to ""
        val sp = Pets.of(pt.species); val col = sp.color.toInt()
        val cy = top + (if (look.story) 450 else 290)
        c.drawCircle(W / 2f, cy, if (look.story) 400f else 320f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W / 2f, cy, 420f, (col and 0xFFFFFF) or 0x88000000.toInt(), col and 0xFFFFFF, Shader.TileMode.CLAMP) })
        c.pet(pt, Pose.IDLE, W / 2f, cy + (if (look.story) 400 else 290), if (look.story) 820f else 560f)
        var y = cy + (if (look.story) 520 else 380)
        c.t(pt.name, W / 2f, y, 96f, bold, ink, Paint.Align.CENTER)
        c.t("${sp.title} · ${pt.stage().title()}" + (pt.klass()?.let { " · " + it.klass } ?: "") + (pt.speciality()?.let { " · " + it.title } ?: "") + if (pt.shiny) tr(" · редкий окрас", " · rare colours") else "", W / 2f, y + 64, 38f, reg, sub, Paint.Align.CENTER)
        y += 110
        val best = Stat.entries.zip(pt.stats).maxByOrNull { it.second }
        c.facts(listOf("${pt.xp}" to tr("опыта", "XP"), (best?.let { "${it.second}" } ?: "0") to (best?.first?.title?.lowercase() ?: ""), "${d.pets.count { it.hatched > 0 }}/${Pets.all.size}" to tr("в коллекции", "collected")), y, 150f)
        c.t(tr("В трактире: ", "In the tavern: ") + app.questling.ui.pets.perkShort(sp), W / 2f, y + 200, 30f, reg, sub, Paint.Align.CENTER)
        c.footer()
        return b to "${pt.name} – ${sp.title}"
    }

    private fun tavernCard(): Pair<Bitmap, String> {
        val (b, c) = base(tr("мой трактир", "my tavern"))
        val gh = (W - 120f) / 1.5f
        c.tavern(RectF(60f, top + 20, W - 60f, top + 20 + gh))
        var y = top + 70 + gh
        c.t(tr("Трактир «Последнее сохранение»", "The Last Save tavern"), 60f, y + 40, 58f, maxW = W - 120f)
        c.t(d.team.joinToString(" · ") { "${it.name} (${Pets.of(it.species).title.lowercase()})" }, 60f, y + 100, 32f, reg, sub)
        y += 140
        c.facts(listOf("${(d.s.items - d.s.hidden).size}/${Shop.all.size}" to tr("вещей", "things"), "${d.pets.count { it.hatched > 0 }}" to tr("питомцев", "creatures"),
            "${Season.level(d.s.seasonXp)}" to tr("уровень сезона", "season level")), y, 150f)
        c.footer()
        return b to tr("Мой трактир в Questling", "My tavern in Questling")
    }

    private fun collection(): Pair<Bitmap, String> {
        val (b, c) = base(tr("коллекция", "collection"))
        val cols = if (look.story) 5 else 6; val cell = (W - 120f) / cols
        var y = top + 20
        c.t(tr("${d.pets.count { it.hatched > 0 }} из ${Pets.all.size}", "${d.pets.count { it.hatched > 0 }} of ${Pets.all.size}"), 60f, y + 60, 76f); y += 100
        Pets.all.forEachIndexed { i, sp ->
            val x = 60f + (i % cols) * cell; val yy = y + (i / cols) * cell * .82f
            if (yy + cell * .8f > bottom) return@forEachIndexed
            val pt = d.pets.firstOrNull { it.species == sp.id }
            val img = when { pt == null -> null; pt.hatched == 0L -> Art.egg(ctx, sp.id); else -> Art.pet(ctx, sp.id, 2) }?.asAndroidBitmap()
            c.panel(x + 6, yy + 6, cell - 12, cell * .82f - 12, 28f)
            if (img != null) c.bitmap(img, x + cell / 2, yy + cell * .74f, cell * .66f, false)
            else if (pt != null && pt.hatched > 0) c.drawCircle(x + cell / 2, yy + cell * .41f, cell * .2f, p(sp.color.toInt()))
            else c.t("?", x + cell / 2, yy + cell * .5f, 64f, bold, sub, Paint.Align.CENTER)
        }
        c.footer()
        return b to tr("Мои питомцы в Questling", "My creatures in Questling")
    }

    private fun trophies(): Pair<Bitmap, String> {
        val (b, c) = base(tr("трофеи", "trophies"))
        val got = d.s.trophies
        var y = top + 40
        c.t(tr("${got.size} из ${Trophies.all.size}", "${got.size} of ${Trophies.all.size}"), 60f, y + 50, 84f); y += 100
        c.facts(Tier.entries.reversed().map { t -> "${Trophies.all.count { it.tier == t && it.id in got }}" to t.title }, y, 150f); y += 190
        val latest = got.entries.sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenByDescending { Trophies.byId[it.key]?.tier?.ordinal ?: 0 }).mapNotNull { Trophies.byId[it.key] }
        latest.take(if (look.story) 9 else 5).forEach { t ->
            c.panel(60f, y, W - 120f, 104f, 28f)
            c.drawCircle(116f, y + 52, 30f, p(t.tier.color.toInt())); c.prop(t.tier.prop, 116f, y + 84, 64f)
            c.t(t.title, 170f, y + 48, 38f, sans, ink, maxW = W - 290f)
            c.t(t.desc, 170f, y + 86, 27f, reg, sub, maxW = W - 290f)
            y += 118
        }
        c.footer()
        return b to tr("Мои трофеи в Questling", "My trophies in Questling")
    }
}

object Share {
    fun send(ctx: Context, bmp: Bitmap, title: String) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }; val f = File(dir, "questling.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, tr("Поделиться карточкой", "Share the card")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves the card to Pictures/Questling. */
    fun save(ctx: Context, bmp: Bitmap): Boolean = runCatching {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "questling-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Questling") }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; true }.getOrDefault(false)
}
