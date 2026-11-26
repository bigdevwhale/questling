package app.questling.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process

/**
 * Phone games found by Android's usage access: which game apps were on screen and for how long.
 * Nothing leaves the phone; a found stretch becomes a session only when the player says so.
 */
object Usage {
    private const val GAP = 5 * 60_000L       // stretches closer than this are one session
    private const val MIN = 3                  // minutes: shorter ones are not worth asking about
    private const val LOOK = 6 * 3600_000L     // how far back a look reaches past where the last one stopped
    // UsageEvents.Event types by number: the names differ between Android 9 and 10
    private const val RESUMED = 1; private const val PAUSED = 2; private const val STOPPED = 23

    fun allowed(ctx: Context): Boolean = runCatching {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun isGame(ctx: Context, pkg: String, known: Set<String>): String? = runCatching {
        if (pkg == ctx.packageName) return null
        val ai = ctx.packageManager.getApplicationInfo(pkg, 0)
        val game = pkg in known || ai.category == ApplicationInfo.CATEGORY_GAME || (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        if (game) ctx.packageManager.getApplicationLabel(ai).toString() else null
    }.getOrNull()

    /** What one look found: finished stretches, and the time the next look should start from. */
    class Scan(val found: List<Found>, val upTo: Long)

    private class Span(val pkg: String, val a: Long, val b: Long, val live: Boolean)
    private class Group(val pkg: String, val start: Long, var played: Long = 0, var live: Boolean = false)

    /**
     * Game stretches that started at or after [since] and are over by [to]. A game still on screen at [to]
     * is left for the next look: [Scan.upTo] stops at its start, so it is found whole later instead of
     * being cut in two. A stretch never runs across [since]: what came after it is a new stretch, as the
     * part before was already offered. [known] packages count as games whatever the phone thinks.
     */
    fun scan(ctx: Context, since: Long, to: Long, known: Set<String>, ignored: Set<String>): Scan = runCatching {
        if (!allowed(ctx)) return Scan(emptyList(), since)
        val usm = ctx.getSystemService(UsageStatsManager::class.java)
        // look back past [since], so a stretch that began before it is not mistaken for a new one
        val ev = usm.queryEvents(since - LOOK, to)
        val e = UsageEvents.Event()
        val labels = HashMap<String, String?>()
        val events = ArrayList<Event>()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            val pkg = e.packageName ?: continue
            if (pkg in ignored || e.eventType !in setOf(RESUMED, PAUSED, STOPPED)) continue
            labels.getOrPut(pkg) { isGame(ctx, pkg, known) } ?: continue
            events += Event(pkg, e.eventType == RESUMED, e.timeStamp, e.className ?: "")
        }
        cut(events, since, to) { labels[it] ?: it }
    }.getOrDefault(Scan(emptyList(), since))

    /** A game's screen [cls] coming on ([on]) or leaving it, at [time]. */
    class Event(val pkg: String, val on: Boolean, val time: Long, val cls: String = "")

    /** Stretches out of a game's comings and goings, in time order; see [scan]. */
    fun cut(events: List<Event>, since: Long, to: Long, label: (String) -> String): Scan {
        // a game is on screen while any of its screens is: one screen gives way to another (an ad, a loader) and the
        // old one's late "stopped" comes after the new one is up – that is not the game ending (#4)
        val open = HashMap<String, Long>()
        val up = HashMap<String, MutableSet<String>>()
        val spans = ArrayList<Span>()
        events.forEach { e ->
            val screens = up.getOrPut(e.pkg) { HashSet() }
            // a screen whose leaving got lost (a reboot) long ago must not keep the game "on" for good
            if (e.on && open[e.pkg]?.let { e.time - it >= LOOK } == true) { screens.clear(); open.remove(e.pkg) }
            if (e.on) { screens += e.cls; open.putIfAbsent(e.pkg, e.time) }
            else if (screens.remove(e.cls) && screens.isEmpty())
                open.remove(e.pkg)?.let { st -> if (e.time > st) spans += Span(e.pkg, st, e.time, false) }
        }
        // still on screen: runs up to [to]; a resume whose pause got lost (a reboot) long ago is dropped
        open.forEach { (pkg, st) -> if (to - st < LOOK) spans += Span(pkg, st, to, true) }
        val groups = ArrayList<Group>()
        spans.groupBy { it.pkg }.forEach { (pkg, l) ->
            var g: Group? = null; var en = 0L
            l.sortedBy { it.a }.forEach { sp ->
                val cur = g?.takeIf { sp.a - en <= GAP && !(it.start < since && sp.a >= since) } ?: Group(pkg, sp.a).also { groups += it }
                cur.played += sp.b - sp.a; cur.live = cur.live || sp.live; en = maxOf(en, sp.b); g = cur
            }
        }
        val upTo = groups.filter { it.live }.minOfOrNull { it.start }?.coerceIn(since, to) ?: to
        val out = groups.filter { !it.live && it.start >= since && it.start < upTo && it.played / 60_000 >= MIN }
            .map { Found(it.pkg, label(it.pkg), it.start, (it.played / 60_000).toInt()) }
        return Scan(out.sortedBy { it.start }, upTo)
    }
}
