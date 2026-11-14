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

    /** Game stretches between [from] and [to]; [known] packages count as games whatever the phone thinks. */
    fun scan(ctx: Context, from: Long, to: Long, known: Set<String>, ignored: Set<String>): List<Found> = runCatching {
        if (!allowed(ctx)) return emptyList()
        val usm = ctx.getSystemService(UsageStatsManager::class.java)
        val ev = usm.queryEvents(from, to)
        val e = UsageEvents.Event()
        val open = HashMap<String, Long>()
        val spans = ArrayList<Triple<String, Long, Long>>()
        val labels = HashMap<String, String?>()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            val pkg = e.packageName ?: continue
            if (pkg in ignored) continue
            labels.getOrPut(pkg) { isGame(ctx, pkg, known) } ?: continue
            when (e.eventType) {
                RESUMED -> open.putIfAbsent(pkg, e.timeStamp)
                PAUSED, STOPPED -> open.remove(pkg)?.let { st -> if (e.timeStamp > st) spans += Triple(pkg, st, e.timeStamp) }
            }
        }
        val out = ArrayList<Found>()
        spans.groupBy { it.first }.forEach { (pkg, l) ->
            var st = -1L; var en = -1L; var played = 0L
            fun flush() { if (st >= 0 && played / 60_000 >= MIN) out += Found(pkg, labels[pkg] ?: pkg, st, (played / 60_000).toInt()) }
            l.sortedBy { it.second }.forEach { (_, a, b) ->
                if (st < 0 || a - en > GAP) { flush(); st = a; played = 0 }
                played += b - a; en = b
            }
            flush()
        }
        out.sortedBy { it.start }
    }.getOrDefault(emptyList())
}
