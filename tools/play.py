"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

    python tools/play.py

Takes docs/screenshots/<lang>/<n>-<name>.png (1080x2400 captures of the demo build, tools/capture.sh) and writes the
512 px icon, the 1024x500 feature graphics, the 1600x900 launch posts and six 1080x1920 captioned screenshots per
language. The phone's status bar is painted over with a clean one. Play wants screenshots no longer than twice their
width, so each screen runs off the bottom edge under its caption. Captions are in Lora, the app's serif, in the
fire-lit colours of the dark theme; the banner is the tavern itself with a few creatures cheering on the rug.
Needs `pip install pillow`.
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
RES = ROOT / "app/src/main/res"
ART = RES / "drawable-nodpi"
SERIF = RES / "font/lora_medium.ttf"
SANS = RES / "font/inter_semibold.ttf"
ROUND = RES / "font/nunito_bold.ttf"

# The dark palette (ui/theme/Theme.kt): the tavern at night, cream ink, ember, coin gold.
BG = (0x1A, 0x14, 0x12)
CARD = (0x32, 0x2A, 0x24)
INK = (0xF5, 0xEB, 0xDD)
MUTED = (0xB0, 0xA0, 0x90)
EMBER = (0xF0, 0x80, 0x60)
EMBER_DEEP = (0xB5, 0x40, 0x2F)
GOLD = (0xF0, 0xC0, 0x60)
ICON_BG = (0x7A, 0x2A, 0x20)  # values/colors.xml icon_bg

BAR = 96  # the status bar of the 13 Pro captures, painted over

NAME = "Questling"
LINES = {
    "en": ("A diary of everything you play,", "with creatures that grow on it"),
    "ru": ("Дневник всего, во что вы играете,", "и питомцы, которые от этого растут"),
}

# The order they appear in the store and the caption: a line in cream and one in gold.
PAGES = [
    ("1-home", ("A tavern that grows", "with every game you play"), ("Трактир, который растёт", "с каждой вашей игрой")),
    ("2-journal", ("Every session, on a screen", "or at the table"), ("Каждая сессия –", "на экране или за столом")),
    ("4-pets", ("36 creatures, found by", "the way you play"), ("36 питомцев приходят за то,", "как именно вы играете")),
    ("3-trophies", ("Nearly 300 trophies", "and a season every quarter"), ("Почти 300 трофеев", "и новый сезон каждый квартал")),
    ("5-games", ("Video games, board games,", "cards and RPGs in one place"), ("Видеоигры, настолки,", "карты и ролёвки – вместе")),
    ("6-game", ("Hours, a rating and", "where you stopped"), ("Часы, оценка и то,", "на чём вы остановились")),
]

# Creatures on the banner's rug, all in frame 4 (cheering): species, x as a fraction of the width, height of the height.
CROWD = [("dicey", .55, .26), ("corgi", .66, .33), ("questling", .79, .44), ("slime", .92, .28)]


def font(size, path=SERIF):
    return ImageFont.truetype(str(path), size)


def fit(text, size, width, path=SERIF):
    while size > 20 and font(size, path).getlength(text) > width:
        size -= 2
    return font(size, path)


def gradient(W, H, top, bottom):
    g = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(g)
    for y in range(H):
        t = y / max(1, H - 1)
        d.line([(0, y), (W, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return g.convert("RGBA")


def glow(g, cx, cy, r, colour, alpha=150):
    """A soft round light, like the hearth's on the tavern wall."""
    mask = Image.new("L", g.size, 0)
    ImageDraw.Draw(mask).ellipse((cx - r, cy - r, cx + r, cy + r), fill=alpha)
    g.paste(Image.new("RGBA", g.size, colour + (255,)), (0, 0), mask.filter(ImageFilter.GaussianBlur(r / 2.5)))


def art(name, height):
    im = Image.open(ART / f"{name}.webp").convert("RGBA")
    k = height / im.height
    return im.resize((round(im.width * k), round(im.height * k)), Image.LANCZOS)


def clean_bar(im):
    """Paints the status bar over with the row just below it, then a calm one: 9:41, signal, wifi, a full battery."""
    im = im.convert("RGB")
    row = im.crop((0, BAR + 2, im.width, BAR + 3)).resize((im.width, BAR + 2))
    im.paste(row, (0, 0))
    d = ImageDraw.Draw(im)
    d.text((56, 58), "9:41", font=font(40, SANS), fill=INK, anchor="lm")
    x = im.width - 56
    d.rounded_rectangle((x - 62, 42, x - 6, 74), 7, outline=INK, width=3)
    d.rounded_rectangle((x - 56, 48, x - 12, 68), 3, fill=INK)
    d.rectangle((x - 4, 52, x, 64), fill=INK)
    cx, cy = x - 110, 72
    for r in (34, 23, 12):
        d.arc((cx - r, cy - r, cx + r, cy + r), 225, 315, fill=INK, width=5)
    d.ellipse((cx - 4, cy - 6, cx + 4, cy + 2), fill=INK)
    for i in range(4):
        bx = x - 196 + i * 13
        d.rectangle((bx, 72 - 8 - i * 8, bx + 8, 72), fill=INK)
    return im


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=48):
    """A screen on the board: rounded corners, a deep shadow and a thin honey-wood edge."""
    sh = Image.new("RGBA", (card.width + 160, card.height + 160), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((80, 100, 80 + card.width, 100 + card.height), r, fill=(0, 0, 0, 150))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(30)), (x - 80, y - 80))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 1, y - 1, x + card.width, y + card.height), r, outline=(0x6A, 0x4A, 0x32), width=3)


def embers(g, box, n, seed):
    """Sparks rising from the hearth: small warm dots, some blurred."""
    rnd = random.Random(seed)
    layer = Image.new("RGBA", g.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x0, y0, x1, y1 = box
    for _ in range(n):
        x, y, r = rnd.uniform(x0, x1), rnd.uniform(y0, y1), rnd.uniform(1.5, 4.5)
        c = rnd.choice((GOLD, EMBER, (255, 214, 150)))
        d.ellipse((x - r, y - r, x + r, y + r), fill=c + (rnd.randint(90, 220),))
    g.alpha_composite(layer.filter(ImageFilter.GaussianBlur(.8)))


def coin_rule(g, cx, y):
    """The gold rule under a caption, with the app's coin in the middle."""
    d = ImageDraw.Draw(g)
    d.line((cx - 170, y, cx - 36, y), fill=GOLD, width=3)
    d.line((cx + 36, y, cx + 170, y), fill=GOLD, width=3)
    coin = art("prop_coin", 50)
    g.alpha_composite(coin, (cx - coin.width // 2, y - coin.height // 2))


def screenshot(src, caption):
    W, H = 1080, 1920
    g = gradient(W, H, (0x3A, 0x26, 0x1C), BG)
    glow(g, W // 2, 120, 340, (0x8A, 0x3A, 0x22), 150)
    embers(g, (40, 20, W - 40, 320), 26, caption[0])
    d = ImageDraw.Draw(g)
    f = min((fit(c, 78, W - 130) for c in caption), key=lambda x: x.size)
    d.text((W / 2, 104), caption[0], font=f, fill=INK, anchor="mm")
    d.text((W / 2, 196), caption[1], font=f, fill=GOLD, anchor="mm")
    coin_rule(g, W // 2, 278)
    im = clean_bar(Image.open(src))
    w = 880
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 340
    # only the top corners show: the screen runs on past the bottom edge
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def icon_square(size):
    """The adaptive icon flattened: the d20 over the ember-red ground, as tools/icon.py builds docs/logo.png."""
    g = Image.new("RGBA", (size, size), ICON_BG + (255,))
    fg = Image.open(ART / "ic_launcher_fg.webp").convert("RGBA")
    big = fg.resize((size * 432 // 330, size * 432 // 330), Image.LANCZOS)
    g.alpha_composite(big, ((size - big.width) // 2, (size - big.height) // 2))
    return g


def icon():
    """Play wants a full square and rounds the corners itself."""
    icon_square(512).convert("RGB").save(OUT / "icon-512.png", optimize=True)


def logo(size):
    """The icon as a launcher shows it: a rounded square."""
    sq = icon_square(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size - 1, size - 1), size * .22, fill=255)
    sq.putalpha(mask)
    return sq


def night_sky(W, H, seed=7):
    """What the tavern window shows at night: deep blue, stars and a moon."""
    g = gradient(W, H, (0x10, 0x16, 0x34), (0x2A, 0x2A, 0x58))
    d = ImageDraw.Draw(g)
    rnd = random.Random(seed)
    for _ in range(W * H // 900):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        r = rnd.choice((1, 1, 1.5, 2))
        d.ellipse((x - r, y - r, x + r, y + r), fill=(255, 250, 230, rnd.randint(120, 255)))
    return g


def tavern(W, H, zoom=1.0):
    """The tavern room scaled to cover W x H (pinned to the right, so the hearth stays in), the window glass
    filled with the night, the creatures cheering on the rug."""
    bg = Image.open(ART / "tavern_bg.webp").convert("RGBA")
    k = max(H / bg.height, W / bg.width) * zoom
    bg = bg.resize((round(bg.width * k), round(bg.height * k)), Image.LANCZOS)
    x0, y0 = W - bg.width, (H - bg.height) // 2 + round(H * .04)
    g = Image.new("RGBA", (W, H), BG + (255,))
    g.alpha_composite(night_sky(bg.width, bg.height), (x0, y0))
    g.alpha_composite(bg, (x0, y0))
    glow(g, int(x0 + bg.width * .86), int(y0 + bg.height * .62), int(bg.height * .28), (255, 150, 60), 90)
    floor = H * .95  # on the rug, near the bottom edge
    for name, fx, fh in sorted(CROWD, key=lambda c: c[2]):
        p = art(f"pet_{name}_4", int(H * fh))
        px, py = int(W * fx - p.width / 2), int(floor - p.height)
        sh = Image.new("RGBA", g.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).ellipse((px + p.width * .18, py + p.height * .86, px + p.width * .82, py + p.height * 1.04), fill=(30, 10, 0, 110))
        g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(6)))
        g.alpha_composite(p, (px, py))
    return g


def shade(g, until, strength=235):
    """A dark veil from the left edge, solid for the first half and fading out smoothly by `until`, for the text."""
    mask = Image.new("L", g.size, 0)
    d = ImageDraw.Draw(mask)
    for x in range(until):
        t = max(0., (x / until - .5) / .5)
        d.line([(x, 0), (x, g.height)], fill=round(strength * (1 - t * t * (3 - 2 * t))))
    g.paste(Image.new("RGBA", g.size, BG + (255,)), (0, 0), mask)


def feature(lang):
    W, H = 1024, 500
    g = tavern(W, H)
    shade(g, 720)
    embers(g, (40, 30, 600, 470), 30, lang)
    g.alpha_composite(logo(104), (56, 58))
    d = ImageDraw.Draw(g)
    d.text((182, 112), NAME, font=fit(NAME, 92, 360), fill=INK, anchor="lm")
    lines = LINES[lang]
    f = min((fit(l, 40, 480) for l in lines), key=lambda x: x.size)
    d.line((60, 214, 220, 214), fill=GOLD, width=5)
    for i, l in enumerate(lines):
        d.text((58, 244 + i * 56), l, font=f, fill=INK if i == 0 else GOLD)
    props = [art(n, 64) for n in ("prop_d20", "prop_pad", "prop_meeple", "prop_cards")]
    x = 60
    for p in props:
        g.alpha_composite(p, (x, 380))
        x += p.width + 18
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def post(lang):
    """The launch post: the banner's look at 1600x900, with a «now on Google Play» plaque."""
    W, H = 1600, 900
    g = tavern(W, H)
    shade(g, 1150)
    embers(g, (60, 60, 900, 860), 50, lang + "post")
    g.alpha_composite(logo(160), (96, 96))
    d = ImageDraw.Draw(g)
    d.text((290, 176), NAME, font=fit(NAME, 150, 600), fill=INK, anchor="lm")
    d.line((100, 340, 380, 340), fill=GOLD, width=7)
    lines = LINES[lang]
    f = min((fit(l, 64, 760) for l in lines), key=lambda x: x.size)
    for i, l in enumerate(lines):
        d.text((96, 380 + i * 86), l, font=f, fill=INK if i == 0 else GOLD)
    plaque = "Now on Google Play" if lang == "en" else "Теперь в Google Play"
    pf = font(46, ROUND)
    tw = int(pf.getlength(plaque))
    px, py, ph = 100, 680, 96
    d.rounded_rectangle((px, py + 8, px + tw + 96, py + ph + 8), 48, fill=EMBER_DEEP)
    d.rounded_rectangle((px, py, px + tw + 96, py + ph), 48, fill=EMBER)
    d.text((px + 48, py + ph // 2), plaque, font=pf, fill=(0x2A, 0x14, 0x10), anchor="lm")
    g.convert("RGB").save(OUT / f"post-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        post(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.png", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
