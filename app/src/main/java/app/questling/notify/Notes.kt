package app.questling.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.questling.MainActivity
import app.questling.R
import app.questling.domain.Lang
import app.questling.domain.tr

/** Notifications: the session running (a stopwatch) and the pet's gentle «time for a break». */
object Notes {
    const val LIVE = 10; const val BREAK = 11
    private const val CH_LIVE = "live"; private const val CH_NUDGE = "nudge"

    fun allowed(ctx: Context) = android.os.Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_LIVE, tr("Идёт сессия", "Session running"), NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_NUDGE, tr("Напоминания о перерыве", "Break reminders"), NotificationManager.IMPORTANCE_DEFAULT))
    }
    private fun open(ctx: Context, what: String = "") = PendingIntent.getActivity(ctx, what.hashCode(), Intent(ctx, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("go", what), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun alarm(ctx: Context) = PendingIntent.getBroadcast(ctx, 7, Intent(ctx, BreakTime::class.java), PendingIntent.FLAG_IMMUTABLE)

    /** The session in the shade: a stopwatch since [start]; after [breakAfter] minutes the pet asks for a break (0 = never). */
    @android.annotation.SuppressLint("MissingPermission") // allowed() checks it
    fun live(ctx: Context, game: String, start: Long, breakAfter: Int) {
        if (!allowed(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_LIVE).setSmallIcon(R.drawable.ic_stat).setContentTitle(game)
            .setContentText(tr("Идёт сессия", "Session running")).setUsesChronometer(true).setWhen(start).setShowWhen(true)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open(ctx, "live")).build()
        NotificationManagerCompat.from(ctx).notify(LIVE, n)
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(alarm(ctx))
        if (breakAfter > 0) am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + breakAfter * 60_000L, alarm(ctx))
    }

    fun stopLive(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(LIVE); NotificationManagerCompat.from(ctx).cancel(BREAK)
        ctx.getSystemService(AlarmManager::class.java).cancel(alarm(ctx))
    }

    @android.annotation.SuppressLint("MissingPermission") // allowed() checks it
    fun nudge(ctx: Context, id: Int, title: String, text: String, what: String = "") {
        if (!allowed(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_NUDGE).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open(ctx, what)).setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

/** The session has been going long enough: the pet yawns. */
class BreakTime : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        Lang.ru = ctx.resources.configuration.locales[0].language == "ru"
        Notes.nudge(ctx, Notes.BREAK, tr("Питомец зевает", "Your pet is yawning"),
            tr("Вы играете уже давно. Разомнитесь, выпейте воды – игра подождёт.", "You have been playing for a while. Stretch, have some water – the game will wait."), "live")
    }
}
