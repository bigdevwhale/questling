package app.questling.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Pictures kept in the app's own folders: game covers and the screenshots and photos of sessions. */
object Media {
    private val cache = object : android.util.LruCache<String, ImageBitmap>(48) {}

    fun covers(ctx: Context): File = File(ctx.filesDir, "covers").apply { mkdirs() }
    fun shots(ctx: Context): File = File(ctx.filesDir, "photos").apply { mkdirs() }

    private fun decode(bytes: ByteArray, max: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return null
        var s = 1
        while (o.outWidth / (s * 2) >= max || o.outHeight / (s * 2) >= max) s *= 2
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s }) ?: return null
        val rot = runCatching { ExifInterface(bytes.inputStream()).rotationDegrees }.getOrDefault(0)
        return if (rot == 0) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
    }

    private fun save(dir: File, bytes: ByteArray, max: Int): String? {
        val bmp = decode(bytes, max) ?: return null
        val name = UUID.randomUUID().toString() + ".jpg"
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    /** Saves picture bytes as a cover, shrunk to a sane size and turned upright; the file's name. */
    fun saveCover(ctx: Context, bytes: ByteArray): String? = save(covers(ctx), bytes, 600)
    fun saveCover(ctx: Context, uri: Uri): String? = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()?.let { saveCover(ctx, it) }

    /** Copies a screenshot or a photo into the app's folder. */
    fun saveShot(ctx: Context, uri: Uri): String? = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()?.let { save(shots(ctx), it, 1600) }
    fun saveShot(ctx: Context, f: File): String? = runCatching { save(shots(ctx), f.readBytes(), 1600) }.getOrNull()

    fun delete(ctx: Context, names: List<String>) = names.forEach { runCatching { File(shots(ctx), it).delete() }; cache.remove("s:$it:320"); cache.remove("s:$it:1600") }
    fun deleteCover(ctx: Context, name: String) { if (name.isNotBlank()) { runCatching { File(covers(ctx), name).delete() }; cache.remove("c:$name") } }

    private fun load(f: File, max: Int): Bitmap? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
    }.getOrNull()

    fun cover(ctx: Context, name: String): ImageBitmap? {
        if (name.isBlank()) return null
        cache.get("c:$name")?.let { return it }
        return load(File(covers(ctx), name), 400)?.asImageBitmap()?.also { cache.put("c:$name", it) }
    }
    fun coverBitmap(ctx: Context, name: String): Bitmap? = if (name.isBlank()) null else load(File(covers(ctx), name), 400)

    fun shot(ctx: Context, name: String, max: Int = 320): ImageBitmap? {
        cache.get("s:$name:$max")?.let { return it }
        return load(File(shots(ctx), name), max)?.asImageBitmap()?.also { cache.put("s:$name:$max", it) }
    }
    fun shotBitmap(ctx: Context, name: String, max: Int): Bitmap? = load(File(shots(ctx), name), max)
    fun shotFile(ctx: Context, name: String) = File(shots(ctx), name)

    /** The permission that lets the app see the screenshots made during a session. */
    val readPermission: String get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    fun canRead(ctx: Context) = ctx.checkSelfPermission(readPermission) == PackageManager.PERMISSION_GRANTED

    /** Screenshots the phone made between [from] and [to] (ms), the newest first. Empty without the permission. */
    fun screenshots(ctx: Context, from: Long, to: Long): List<Uri> = runCatching {
        if (!canRead(ctx)) return emptyList()
        val out = ArrayList<Uri>()
        val base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED)
        ctx.contentResolver.query(base, cols, "${MediaStore.Images.Media.DATE_ADDED} BETWEEN ? AND ?", arrayOf((from / 1000 - 5).toString(), (to / 1000 + 5).toString()),
            "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
            while (c.moveToNext() && out.size < 60) {
                val path = (c.getString(1) ?: "") + "/" + (c.getString(2) ?: "")
                if (path.contains("screenshot", ignoreCase = true) || path.contains("скриншот", ignoreCase = true)) out += ContentUris.withAppendedId(base, c.getLong(0))
            }
        }
        out
    }.getOrDefault(emptyList())
}
