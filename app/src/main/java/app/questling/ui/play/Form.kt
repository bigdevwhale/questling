package app.questling.ui.play

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.questling.data.Friend
import app.questling.data.Game
import app.questling.data.Media
import app.questling.data.Play
import app.questling.data.Seat
import app.questling.domain.Kinds
import app.questling.domain.tr
import app.questling.ui.Data
import app.questling.ui.components.Avatar
import app.questling.ui.components.Chip
import app.questling.ui.components.Field
import app.questling.ui.components.Icon
import app.questling.ui.components.Icons
import app.questling.ui.components.MoodRow
import app.questling.ui.components.friendColor
import app.questling.ui.theme.C
import app.questling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** What the player says about a session, while it is being filled in. */
class PlayDraft(p: Play? = null, seats: List<Seat> = emptyList(), scores: Map<Long, Int> = emptyMap()) {
    var result by mutableIntStateOf(p?.result ?: Play.NONE)
    var moment by mutableStateOf(p?.moment ?: "")
    var mood by mutableIntStateOf(p?.mood ?: 0)
    var note by mutableStateOf(p?.note ?: "")
    var shots by mutableStateOf(p?.shotList ?: emptyList())
    /** Scores as typed, by friend id (0 = the player). */
    var scores by mutableStateOf(when {
        seats.isNotEmpty() -> seats.filter { it.score != Int.MIN_VALUE }.associate { it.friendId to it.score.toString() }
        scores.isNotEmpty() -> scores.mapValues { it.value.toString() }
        p != null && p.hasScore -> mapOf(0L to p.score.toString())
        else -> emptyMap() })
    /** Who won among the friends, when the player lost. */
    var winners by mutableStateOf(seats.filter { it.won && it.friendId != 0L }.map { it.friendId }.toSet())
    /** Screenshots found on the phone, still to be copied in: the ticked ones. */
    var picked by mutableStateOf(emptySet<Uri>())

    init {
        // a score sheet kept during the game says who won
        if (p == null && scores.size >= 2) { val top = scores.maxOf { it.value }; val best = scores.filter { it.value == top }.keys
            result = when { best.size > 1 && 0L in best -> Play.DRAW; 0L in best -> Play.WIN; else -> Play.LOSS }
            winners = best - 0L }
    }

    val myScore get() = scores[0L]?.trim()?.toIntOrNull() ?: Int.MIN_VALUE

    /** Everybody's seats: only when there is company and something to say about who won or scored. */
    fun seats(friends: List<Long>): List<Seat> {
        if (friends.isEmpty()) return emptyList()
        val sc = (listOf(0L) + friends).associateWith { scores[it]?.trim()?.toIntOrNull() }
        val versus = result == Play.WIN || result == Play.LOSS || result == Play.DRAW
        if (!versus && sc.values.all { it == null }) return emptyList()
        val order = sc.entries.filter { it.value != null }.sortedByDescending { it.value }.map { it.key }
        return (listOf(0L) + friends).map { id ->
            val won = when (result) { Play.WIN -> id == 0L; Play.LOSS -> id in winners; Play.DRAW -> false; Play.COOP_WIN -> true; else -> false }
            Seat(playId = 0, friendId = id, score = sc[id] ?: Int.MIN_VALUE, place = order.indexOf(id) + 1, won = won)
        }
    }
}

/** The fields of a session that do not depend on time: how it ended, the scores, the moment, the mood, a note, pictures. */
@Composable
fun PlayFields(dr: PlayDraft, g: Game?, friends: List<Long>, d: Data, found: List<Uri> = emptyList()) {
    val company = friends.isNotEmpty()
    val table = g != null && Kinds.table(g.kind)
    if (company || table || dr.result != Play.NONE) {
        Label(tr("Чем закончилось", "How it ended"))
        val opts = buildList {
            add(Play.WIN to C.ok); add(Play.LOSS to C.bad); add(Play.DRAW to C.gold)
            if (company) { add(Play.COOP_WIN to C.ok); add(Play.COOP_LOSS to C.bad) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            opts.forEach { (r, col) -> Chip(Kinds.result(r), dr.result == r, color = col) { dr.result = if (dr.result == r) Play.NONE else r } }
        }
        if (company && dr.result == Play.LOSS) {
            Text(tr("Кто победил?", "Who won?"), style = T.small, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                friends.mapNotNull { d.friendById[it] }.forEach { f -> Chip(f.name, f.id in dr.winners, color = friendColor(f)) { dr.winners = if (f.id in dr.winners) dr.winners - f.id else dr.winners + f.id } }
            }
        }
    }
    if (company || table || dr.scores.isNotEmpty()) {
        Label(tr("Очки", "Score"))
        (listOf<Friend?>(null) + friends.mapNotNull { d.friendById[it] }).forEach { f ->
            val id = f?.id ?: 0L
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(f, 30.dp, d.s.player); Spacer(Modifier.width(10.dp))
                Text(f?.name ?: d.s.player.ifBlank { tr("Я", "Me") }, style = T.body, modifier = Modifier.weight(1f), maxLines = 1)
                Field(dr.scores[id].orEmpty(), { v -> dr.scores = if (v.isBlank()) dr.scores - id else dr.scores + (id to v.filter { it.isDigit() || it == '-' }.take(7)) }, "–", Modifier.width(96.dp), number = true, center = true)
            }
        }
    }
    Label(tr("Важный момент", "A moment"))
    val moments = listOf(Play.M_BOSS, Play.M_CHAPTER, Play.M_FINISH, Play.M_HUNDRED)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        moments.forEach { m -> Chip(Kinds.moment(m), dr.moment == m, color = if (m == Play.M_FINISH || m == Play.M_HUNDRED) C.gold else C.acc) { dr.moment = if (dr.moment == m) "" else m } }
    }
    Label(tr("Как игралось", "How it felt"))
    MoodRow(dr.mood) { dr.mood = it }
    Label(tr("Заметка", "A note"))
    Field(dr.note, { dr.note = it }, tr("Что произошло, на чём остановились, что запомнилось", "What happened, where you stopped, what stuck"), Modifier.fillMaxWidth(), single = false)
    Label(tr("Скриншоты и фото", "Screenshots and photos"))
    ShotsEditor(dr, found)
}

@Composable
fun Label(text: String) = Text(text.uppercase(), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

/** A small picture out of any content uri. */
@Composable
fun UriThumb(uri: Uri, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, uri) { value = withContext(Dispatchers.IO) { runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        var s = 1; while (o.outWidth / (s * 2) >= 240 && o.outHeight / (s * 2) >= 240) s *= 2
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s }) } }.getOrNull() } }
    Box(modifier.size(size).clip(RoundedCornerShape(10.dp)).background(C.card2)) { bmp?.let { Image(it.asImageBitmap(), null, Modifier.size(size), contentScale = ContentScale.Crop) } }
}

/** A saved shot as a square thumbnail. */
@Composable
fun ShotThumb(name: String, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val img by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, name) { value = withContext(Dispatchers.IO) { Media.shot(ctx, name) } }
    Box(modifier.size(size).clip(RoundedCornerShape(10.dp)).background(C.card2)) { img?.let { Image(it, null, Modifier.size(size), contentScale = ContentScale.Crop) } }
}

/**
 * The pictures of a session: the ones kept already (tap removes), screenshots the phone made during it
 * (tap ticks), and two ways to add more – the gallery and the phone's own camera.
 */
@Composable
fun ShotsEditor(dr: PlayDraft, found: List<Uri>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<File?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        scope.launch { val names = withContext(Dispatchers.IO) { uris.mapNotNull { Media.saveShot(ctx, it) } }; dr.shots = dr.shots + names } }
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = photo; if (ok && f != null) scope.launch { val n = withContext(Dispatchers.IO) { Media.saveShot(ctx, f).also { f.delete() } }; if (n != null) dr.shots = dr.shots + n } }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Add(Icons.IMAGE, tr("Галерея", "Gallery")) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        Add(Icons.CAMERA, tr("Камера", "Camera")) {
            val f = File(File(ctx.cacheDir, "share").apply { mkdirs() }, "photo_${System.currentTimeMillis()}.jpg"); photo = f
            runCatching { take.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)) } }
        dr.shots.forEach { n -> Box { ShotThumb(n, 72.dp)
            Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp).clip(CircleShape).background(Color(0xAA000000)).clickable { dr.shots = dr.shots - n }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, Color.White, 12.dp) } } }
        found.forEach { u -> val on = u in dr.picked
            Box(Modifier.clip(RoundedCornerShape(10.dp)).border(if (on) 3.dp else 0.dp, if (on) C.ok else Color.Transparent, RoundedCornerShape(10.dp)).clickable { dr.picked = if (on) dr.picked - u else dr.picked + u }) {
                UriThumb(u, 72.dp)
                if (on) Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp).clip(CircleShape).background(C.ok), contentAlignment = Alignment.Center) { Icon(Icons.CHECK, Color.White, 12.dp) } } }
    }
    if (found.isNotEmpty()) Text(tr("Скриншоты, сделанные во время сессии, уже отмечены – снимите галочку с лишних.", "Screenshots made during the session are ticked already – untick the ones you don't need."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun Add(icon: Icons, label: String, onClick: () -> Unit) {
    Column(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(C.card2).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, C.acc, 22.dp); Text(label, style = T.tiny, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Copies the ticked phone screenshots into the app and returns every shot of the draft. */
suspend fun PlayDraft.allShots(ctx: android.content.Context): List<String> =
    shots + withContext(Dispatchers.IO) { picked.mapNotNull { Media.saveShot(ctx, it) } }
