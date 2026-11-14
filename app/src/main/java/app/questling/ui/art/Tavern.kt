package app.questling.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.questling.BuildConfig
import app.questling.data.Pet
import app.questling.data.Weather
import app.questling.domain.Shop
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The tavern: the generated room, the window onto the real sky over the village, the things bought
 * and the creatures – the leader on the rug, the two others by the walls. The hearth flickers.
 */
@Composable
fun TavernScene(team: List<Pet>, items: Set<String>, pose: Pose, modifier: Modifier = Modifier, preview: String? = null, sky: Sky? = null, onPet: ((Pet) -> Unit)? = null) {
    val ctx = LocalContext.current
    val tt by rememberInfiniteTransition(label = "sky").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val t = if (BuildConfig.SHOTS) .3f else tt
    val bg = Art.get(ctx, "tavern_bg")
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.5f)) {
        val wDp = maxWidth; val hDp = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val r = android.graphics.RectF(0f, 0f, size.width, size.height)
            val sk = sky ?: Sky(1f, 0f, Weather.CLEAR)
            // the sky goes first, under the picture: the window glass is cut out of it
            drawIntoCanvas { drawSky(it.nativeCanvas, r, sk, t) }
            if (bg != null) drawImage(bg, IntOffset.Zero, IntSize(bg.width, bg.height), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Medium)
            else {
                // until the picture is there: plank walls, a floor, a rug and a hearth
                drawRect(Brush.verticalGradient(listOf(Color(0xFF7A5238), Color(0xFF5A3A28))), size = size.copy(height = size.height * .68f))
                for (k in 1..7) drawRect(Color(0x22000000), Offset(size.width * k / 8f, 0f), Size(size.width * .004f, size.height * .68f))
                drawRect(Color(0xFF4A2E1E), Offset(0f, size.height * .68f), size.copy(height = size.height * .32f))
                drawRect(Color(0xFF3A2216), Offset(0f, size.height * .68f), Size(size.width, size.height * .012f))
                drawOval(Color(0xFF9A3428), Offset(size.width * .3f, size.height * .8f), Size(size.width * .4f, size.height * .14f))
                drawOval(Color(0xFFD0A040), Offset(size.width * .34f, size.height * .82f), Size(size.width * .32f, size.height * .1f), style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * .004f))
                drawRect(Color(0xFF6A6A70), Offset(size.width * .76f, size.height * .36f), Size(size.width * .17f, size.height * .34f))
                drawRect(Color(0xFF1A1210), Offset(size.width * .79f, size.height * .48f), Size(size.width * .11f, size.height * .22f))
                drawOval(Color(0xFFF08030), Offset(size.width * .805f, size.height * (.6f - .01f * sin(t * 40 * PI).toFloat())), Size(size.width * .08f, size.height * .1f))
                drawOval(Color(0xFFFFD060), Offset(size.width * .825f, size.height * .64f), Size(size.width * .04f, size.height * .06f))
                // the window frame over the sky
                drawRect(Color(0xFF3A2216), Offset(size.width * .06f, size.height * .2f), Size(size.width * .15f, size.height * .4f), style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * .012f))
                drawRect(Color(0xFF3A2216), Offset(size.width * .132f, size.height * .2f), Size(size.width * .006f, size.height * .4f))
            }
            val shown = Shop.all.filter { it.id in items || it.id == preview }.sortedBy { if (it.wall) 0f else it.y }
            for (it in shown) {
                val img = Art.item(ctx, it.id)
                val w = size.width * it.w
                if (img == null) {
                    // a stand-in box while the picture is not drawn yet
                    val h = w * (if (it.wall) 1.1f else 1.2f)
                    val x = size.width * it.x - w / 2; val y = if (it.wall) size.height * it.y - h / 2 else size.height * it.y - h
                    drawRoundRect(Color(0xFF8A6A4A), Offset(x, y), Size(w, h), androidx.compose.ui.geometry.CornerRadius(w * .12f))
                    drawRoundRect(Color(0xFFC09A6A), Offset(x + w * .12f, y + h * .12f), Size(w * .76f, h * .5f), androidx.compose.ui.geometry.CornerRadius(w * .08f))
                    continue
                }
                val h = w * img.height / img.width
                val x = size.width * it.x - w / 2; val y = if (it.wall) size.height * it.y - h / 2 else size.height * it.y - h
                drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(w.roundToInt(), h.roundToInt()),
                    alpha = if (it.id == preview && it.id !in items) .85f else 1f, filterQuality = FilterQuality.Medium)
            }
            val flick = if (BuildConfig.SHOTS) .5f else (.5f + .5f * sin(t * 2 * PI * 23).toFloat() * sin(t * 2 * PI * 7).toFloat())
            drawIntoCanvas { drawNight(it.nativeCanvas, r, sk, flick) }
        }
        val tap = remember { MutableInteractionSource() }
        team.filter { it.slot in 1..2 }.forEach { p ->
            key(p.id) {
                val s = hDp * .27f
                Box(Modifier.offset(x = wDp * (if (p.slot == 1) .18f else .62f), y = hDp * .97f - s)
                    .then(if (onPet != null) Modifier.clickable(tap, null) { onPet(p) } else Modifier)) { PetView(p, s, if (pose == Pose.WORK) Pose.IDLE else pose) }
            }
        }
        team.firstOrNull { it.slot == 0 }?.let { lead -> key(lead.id) { val s = hDp * .38f
            Box(Modifier.offset(x = (wDp - s) / 2, y = hDp * .99f - s).then(if (onPet != null) Modifier.clickable(tap, null) { onPet(lead) } else Modifier)) { PetView(lead, s, pose) } } }
    }
}
