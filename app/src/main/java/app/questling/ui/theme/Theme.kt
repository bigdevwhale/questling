package app.questling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.questling.R

/** Colour tokens: warm parchment and honey wood by day, a fire-lit tavern by night; ember red for action, coin gold, teal for things done. */
class Pal(
    val dark: Boolean, val bg: Color, val card: Color, val card2: Color, val ink: Color, val mute: Color, val line: Color, val track: Color,
    val acc: Color, val accInk: Color, val accSoft: Color, val ok: Color, val okSoft: Color, val gold: Color, val goldSoft: Color, val bad: Color,
    val heat: List<Color>, val nav: Color, val shadow: Color, val mana: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xF4EBDD), c(0xFFFAF0), c(0xEDE1CE), c(0x2B1F1A), c(0x857565), c(0xE2D5C0), c(0xE8DCC8),
    c(0xB5402F), c(0xFFFFFF), c(0xF6DDD4), c(0x2F8A7A), c(0xD8EDE6), c(0xC08A1A), c(0xF7E8C5), c(0xC4533A),
    listOf(c(0xE8DCC8), c(0xF2CDB0), c(0xE89A70), c(0xD0603E), c(0xA83A2A)), c(0xFFF8EC), Color(0x22503020), c(0xC08A1A))

val Dark = Pal(true, c(0x1A1412), c(0x26201C), c(0x322A24), c(0xF5EBDD), c(0xB0A090), c(0x40362E), c(0x382E28),
    c(0xF08060), c(0x2A1410), c(0x5A2A20), c(0x6CCBB5), c(0x1E3833), c(0xF0C060), c(0x3D3220), c(0xEE7558),
    listOf(c(0x382E28), c(0x6A3A2A), c(0xA04A32), c(0xD8663E), c(0xFF9A6A)), c(0x201A16), Color(0x66000000), c(0xF0C060))

/** The palette in use; switched by the theme setting before the tree is drawn. */
object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val card get() = p.card; val card2 get() = p.card2; val ink get() = p.ink; val mute get() = p.mute
    val line get() = p.line; val track get() = p.track; val acc get() = p.acc; val accInk get() = p.accInk; val accSoft get() = p.accSoft
    val ok get() = p.ok; val okSoft get() = p.okSoft; val gold get() = p.gold; val goldSoft get() = p.goldSoft; val bad get() = p.bad
    val heat get() = p.heat; val nav get() = p.nav; val shadow get() = p.shadow; val mana get() = p.mana
}

val Lora = FontFamily(Font(R.font.lora_regular, FontWeight.Normal), Font(R.font.lora_medium, FontWeight.Medium), Font(R.font.lora_medium, FontWeight.Bold))
val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_bold, FontWeight.ExtraBold))
val Inter = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_semibold, FontWeight.Bold))

object T {
    val title get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 27.sp, lineHeight = 32.sp, color = C.ink)
    val h2 get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 25.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val sec get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.ink)
    val numS get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.ink)
    val big get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
}

@Composable
fun QuestTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
