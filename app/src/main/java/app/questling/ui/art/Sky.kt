package app.questling.ui.art

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.questling.data.Outside
import app.questling.data.Settings
import app.questling.data.Sun
import app.questling.data.Weather
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * What the tavern window shows: [day] 0 night .. 1 full day, [glow] 0..1 how much dawn or dusk colours it,
 * and the weather (CLEAR when unknown).
 */
data class Sky(val day: Float, val glow: Float, val weather: Weather) {
    val night get() = day < .25f

    companion object {
        /** The sky now in the user's town: the real weather and the sun by its rise and set there. */
        fun of(s: Settings, now: Long = System.currentTimeMillis()): Sky {
            val o = Outside.of(s.outside)?.takeIf { now - it.at < 6 * 3600_000L }
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val (rise, set) = when {
                o != null && o.sunrise in today..(today + 86400_000) -> o.sunrise to o.sunset
                !s.lat.isNaN() -> Sun.riseSet(s.lat, s.lon, now)
                else -> (today + 7 * 3600_000L) to (today + 19 * 3600_000L)
            }
            val h = 3600_000f
            // light rises over the hour around sunrise and falls over the hour around sunset
            val day = min(((now - rise) / h + .5f).coerceIn(0f, 1f), ((set - now) / h + .5f).coerceIn(0f, 1f))
            val glow = max(1f - abs(now - rise) / (1.2f * h), 1f - abs(now - set) / (1.2f * h)).coerceIn(0f, 1f)
            return Sky(day, glow, o?.weather ?: Weather.CLEAR)
        }
    }
}

/** The arched window of the tavern picture, as parts of its width and height (tools/cut.py prints them). */
private const val WX0 = .0833f; private const val WX1 = .2014f; private const val WY0 = .194f; private const val WY1 = .5456f

private fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(c: Int, s: Int) = (c shr s) and 0xFF
    fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t.coerceIn(0f, 1f)).toInt()
    return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

/**
 * Paints the view out of the arched window of the tavern picture drawn in [r]: the real sky over the village
 * down the road, lit windows at night. [t] 0..1 loops for rain, snow and twinkling.
 */
fun drawSky(c: Canvas, r: RectF, sky: Sky, t: Float) {
    val w = r.width(); val h = r.height()
    val pane = RectF(r.left + w * WX0 - 2, r.top + h * WY0 - 2, r.left + w * WX1 + 2, r.top + h * WY1 + 2)
    val x0 = pane.left; val y0 = pane.top; val pw = pane.width(); val ph = pane.height()
    val grey = sky.weather == Weather.OVERCAST || sky.weather == Weather.RAIN || sky.weather == Weather.STORM || sky.weather == Weather.FOG || sky.weather == Weather.SNOW
    var top = mix(0xFF0A0B26.toInt(), 0xFF4F8FE0.toInt(), sky.day); var bottom = mix(0xFF26205A.toInt(), 0xFFB5DAF5.toInt(), sky.day)
    if (sky.glow > 0) { top = mix(top, 0xFF5A4C9C.toInt(), sky.glow * .6f); bottom = mix(bottom, 0xFFF59A62.toInt(), sky.glow * .8f) }
    if (grey) { top = mix(top, mix(0xFF22263A.toInt(), 0xFF8C96A2.toInt(), sky.day), .75f); bottom = mix(bottom, mix(0xFF2E3242.toInt(), 0xFFB3BBC4.toInt(), sky.day), .75f) }
    val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    c.save()
    c.clipRect(pane)
    c.drawRect(pane, Paint().apply { shader = LinearGradient(0f, y0, 0f, y0 + ph, top, bottom, Shader.TileMode.CLAMP) })
    if (sky.night && !grey) {
        dot.color = 0xFFFFF6D8.toInt()
        for (k in 0 until 14) { val tw = .5f + .5f * sin((t * 2 * PI + k * 1.7).toFloat()); dot.alpha = (110 + 140 * tw).toInt()
            c.drawCircle(x0 + pw * ((k * 37) % 100) / 100f, y0 + ph * ((k * 53) % 60) / 100f + ph * .04f, w * .0016f, dot) }
        dot.color = 0xFFFFF1C2.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .66f, y0 + ph * .22f, pw * .13f, dot)
        dot.color = top; c.drawCircle(x0 + pw * .66f + pw * .06f, y0 + ph * .2f, pw * .11f, dot)
    }
    if (!sky.night && sky.weather == Weather.CLEAR) { dot.color = 0xFFFFE89A.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .32f, y0 + ph * .22f, pw * .13f, dot) }
    if (sky.weather == Weather.CLOUDY || sky.weather == Weather.CLEAR || grey && sky.weather != Weather.FOG) {
        dot.color = if (grey) mix(0xFF39404E.toInt(), 0xFFD5DAE0.toInt(), sky.day) else if (sky.night) 0xFF3A3F66.toInt() else 0xFFFFFFFF.toInt(); dot.alpha = 220
        for (k in 0 until (if (grey) 3 else 1)) { val cx = x0 + pw * ((.1f + t * .5f + k * .4f) % 1.3f - .15f); val cy = y0 + ph * (.3f + k * .1f)
            c.drawCircle(cx, cy, pw * .1f, dot); c.drawCircle(cx + pw * .12f, cy - pw * .05f, pw * .12f, dot); c.drawCircle(cx + pw * .24f, cy, pw * .09f, dot) }
    }
    // the village down the road: pines, roofs and a far hill, windows lit at dusk and night
    val dim = 1f - (1f - sky.day) * .6f
    val far = mix(0xFF14202A.toInt(), 0xFF6A8A8A.toInt(), dim); val near = mix(0xFF0E1A16.toInt(), 0xFF3F6A4A.toInt(), dim)
    val pf = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = far }
    c.drawPath(android.graphics.Path().apply { moveTo(x0, y0 + ph * .7f); cubicTo(x0 + pw * .3f, y0 + ph * .52f, x0 + pw * .6f, y0 + ph * .6f, x0 + pw, y0 + ph * .66f); lineTo(x0 + pw, y0 + ph); lineTo(x0, y0 + ph); close() }, pf)
    val roof = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mix(0xFF1A1420.toInt(), 0xFF8A5A4A.toInt(), dim) }
    fun house(cx: Float, top: Float, hw: Float) {
        val l = x0 + pw * (cx - hw / 2); val rr = x0 + pw * (cx + hw / 2); val tp = y0 + ph * top
        c.drawRect(l, tp, rr, y0 + ph, pf)
        c.drawPath(android.graphics.Path().apply { moveTo(l - pw * .03f, tp); lineTo(x0 + pw * cx, tp - ph * .08f); lineTo(rr + pw * .03f, tp); close() }, roof)
    }
    house(.5f, .7f, .24f); house(.84f, .75f, .2f)
    val pn = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = near }
    fun pine(cx: Float, base: Float, hh: Float) { for (k in 0 until 3) { val tp = y0 + ph * (base - hh + k * hh * .28f); val bw = pw * (.07f + k * .035f)
        c.drawPath(android.graphics.Path().apply { moveTo(x0 + pw * cx, tp); lineTo(x0 + pw * cx - bw, tp + ph * hh * .45f); lineTo(x0 + pw * cx + bw, tp + ph * hh * .45f); close() }, pn) } }
    pine(.14f, .95f, .4f); pine(.3f, .98f, .28f)
    c.drawPath(android.graphics.Path().apply { moveTo(x0, y0 + ph * .9f); cubicTo(x0 + pw * .3f, y0 + ph * .86f, x0 + pw * .7f, y0 + ph * .9f, x0 + pw, y0 + ph * .88f); lineTo(x0 + pw, y0 + ph); lineTo(x0, y0 + ph); close() }, pn)
    if (sky.day < .7f) {
        dot.color = 0xFFFFC870.toInt(); dot.alpha = ((1 - sky.day) * 255).toInt().coerceIn(0, 255)
        val lit = listOf(.44f to .74f, .54f to .74f, .49f to .8f, .8f to .79f, .87f to .79f)
        for ((k, p) in lit.withIndex()) { if ((k + (t * 3).toInt()) % 7 == 0 && k % 3 == 0) continue
            c.drawRect(x0 + pw * p.first, y0 + ph * p.second, x0 + pw * (p.first + .025f), y0 + ph * (p.second + .035f), dot) }
    }
    if (sky.weather == Weather.SNOW) {
        dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 230
        for (k in 0 until 16) { val fx2 = ((k * 0.173f) % 1f) + .03f * sin((t * 6 + k).toFloat()); val fy2 = ((t + k * .1f) % 1f)
            c.drawCircle(x0 + pw * fx2, y0 - ph * .05f + ph * 1.1f * fy2, w * .002f, dot) }
    }
    if (sky.weather == Weather.RAIN || sky.weather == Weather.STORM) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99C8D8EE.toInt(); strokeWidth = w * .0012f }
        for (k in 0 until 12) { val fx2 = ((k * 0.137f) % 1f); val fy2 = ((t * 3 + k * .23f) % 1f)
            val x = x0 + pw * fx2; val y = y0 - ph * .1f + ph * 1.2f * fy2; c.drawLine(x, y, x - pw * .04f, y + ph * .06f, line) }
        if (sky.weather == Weather.STORM && t % .5f < .025f) { dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 160; c.drawRect(pane, dot) }
    }
    if (sky.weather == Weather.FOG) { dot.color = 0xFFE6E9EE.toInt(); dot.alpha = 150; c.drawRect(pane, dot) }
    c.restore()
}

/** The tavern darkens at night (over the room and its things, under the creatures); the hearth and the lanterns keep warm pools. */
fun drawNight(c: Canvas, r: RectF, sky: Sky, flicker: Float) {
    val w = r.width(); val h = r.height()
    val dark = ((1 - sky.day) * .3f * 255).toInt()
    if (dark > 4) c.drawRect(r, Paint().apply { color = (dark shl 24) or 0x140A08 })
    fun glow(x: Float, y: Float, rad: Float, a: Float) {
        val al = (a * 255).toInt().coerceIn(0, 255); if (al <= 2) return
        c.drawCircle(r.left + w * x, r.top + h * y, w * rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(r.left + w * x, r.top + h * y, w * rad, (al shl 24) or 0xFFB050, 0x00FFB050, Shader.TileMode.CLAMP) })
    }
    // the hearth breathes
    glow(HEARTH_X, HEARTH_Y, .26f + flicker * .02f, .16f + (1 - sky.day) * .22f + flicker * .05f)
    glow(.295f, .31f, .13f, .1f + (1 - sky.day) * .18f)
    glow(.715f, .31f, .13f, .1f + (1 - sky.day) * .18f)
}

/** Where the fire burns in the tavern picture. */
const val HEARTH_X = .865f; const val HEARTH_Y = .57f
