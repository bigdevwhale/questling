"""Generates creature sprite sheets and tavern art with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py                 # everything missing
    python tools/gen_art.py slime mimic     # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, warm cozy tavern palette (honey wood, amber firelight, deep teal and crimson accents), "
    "cute and round, big shiny eyes. Original character design, not from any existing game."
)
# objects are plain things: the generator likes to give everything a cute face, so it is told twice not to
OBJ_STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, warm cozy tavern palette (honey wood, amber firelight, deep teal and crimson accents). "
    "These are plain inanimate objects: NO faces, NO eyes, NO mouths, nothing alive about them. No letters, numbers, logos or text anywhere."
)
GRID = (
    "Layout: 3 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. "
    "One creature per cell, centered, with clear margin; nothing touches the cell borders. No text, no letters, no grid lines, "
    "no shadows on the background, no other objects except those listed."
)

# id: (look, signature move) – the order is the species order in domain/Pets.kt
PETS = {
    "questling": ("a tiny round adventurer creature: a soft cream-coloured blob with a green hood, a little wooden sword and a tiny backpack", "opening a small wooden treasure chest with golden light spilling out"),
    "slime": ("a glossy jelly slime, translucent mint green with a darker core and a big shine", "bouncing high and splitting off a tiny twin slime"),
    "mimic": ("a small wooden treasure-chest mimic with iron bands, a long pink tongue and stubby little legs", "spitting out a shower of gold coins"),
    "goblin": ("a little green goblin merchant with big ears, a patched brown cloak and a huge backpack of wares", "proudly holding up a glowing red potion for sale with a sly grin"),
    "shroom": ("a plump mushroom critter with a violet cap with pale spots, a cream stem body and tiny feet", "releasing a puff of sparkling spores"),
    "ghost": ("a small blocky retro ghost, pale blue with a wavy hem and square black eyes", "phasing halfway through a wooden floor, peeking out"),
    "meeple": ("a wooden board-game meeple come alive, warm orange painted wood with a simple friendly face", "planting a tiny flag on a small hexagon landscape tile"),
    "dicey": ("a living twenty-sided die, ruby red with engraved dots on the faces (no numbers), tiny arms and legs", "tumbling through the air with a golden critical-hit sparkle burst"),
    "joker": ("a playing-card sprite: a standing card body with red diamond and black spade motifs, a jester hat with bells, tiny arms and legs", "fanning a hand of five cards like a stage magician"),
    "robo": ("a small round robot with a teal metal body, one antenna with a glowing tip and a screen face with pixel eyes", "scanning with a blue light beam from its antenna"),
    "dragon": ("a small chubby dragon with emerald scales, a cream belly and stubby wings", "sitting on a small pile of gold, puffing a tiny flame"),
    "frogknight": ("a little frog knight in a small iron helmet with a red plume, a round wooden shield and a needle-thin sword", "doing a forward dodge roll"),
    "necrocat": ("a black cat with a glowing pale-green skeleton pattern on its fur and a tiny green-flame lantern", "conjuring a small friendly floating skull wrapped in green flame"),
    "starjelly": ("a cosmic jellyfish: a deep blue translucent bell with stars and a nebula inside, trailing glowing tentacles", "floating beside a tiny ringed planet"),
    "raccoon": ("a raccoon racer with aviator goggles and a long red scarf", "zooming in a tiny go-kart with speed lines"),
    "capybara": ("a calm round capybara wearing a straw hat", "watering a small sprout in a tiny garden plot with a watering can"),
    "axolotl": ("a pink axolotl with frilly gills and a happy smile", "fitting a glowing jigsaw puzzle piece into place"),
    "fox": ("an orange fox explorer with a leather satchel and a brass compass", "holding up an old treasure map"),
    "owl": ("a wise grey owl wearing a tiny bicorne general's hat", "moving a wooden pawn on a small chessboard"),
    "hamster": ("a chubby golden hamster with round cheeks", "running in a tiny wheel that sparks little gold coins"),
    "penguin": ("a penguin with a little sports headband and a whistle", "kicking a small football"),
    "bunny": ("a lilac bunny wearing big headphones", "dancing on a small glowing dance pad with musical notes around"),
    "bear": ("a big cuddly brown bear with a cozy knitted scarf", "carrying a big board game box and smiling"),
    "cat": ("an orange tabby cat", "holding a game controller in both paws, tongue out in focus"),
    "corgi": ("a corgi with a tiny red bandana", "carrying a game cartridge in its mouth, tail wagging"),
    "tortoise": ("an old tortoise with a tiny stone castle with towers on its shell", "marching slowly with little banners flying from the castle"),
    "crab": ("a red crab with tiny boxing gloves on its claws", "in a fighting stance throwing a punch with a star-shaped impact burst"),
    "bat": ("a little purple bat wearing a gamer headset with a microphone", "hanging upside down holding a glowing handheld console"),
    "golem": ("a small golem built from mossy stone cubes, blocky voxel look, glowing amber eyes", "swinging a little pickaxe with cube chips flying"),
    "fairy": ("a tiny fairy with butterfly wings and a glowing storybook", "reading the storybook with glowing pages and small floating hearts"),
    "skelly": ("a cute little skeleton with a round wooden shield and a tiny rusty sword", "popping back together, bones snapping into place with sparkles"),
    "octopus": ("a teal octopus with big friendly eyes", "holding four different game controllers in four tentacles at once"),
    "bee": ("a fuzzy bumblebee builder wearing a little yellow hard hat", "carrying a hexagon honeycomb tile into place"),
    "phoenix": ("a phoenix chick with gold, orange and crimson flame feathers", "rising from glowing embers with wings spread wide"),
    "unicorn": ("a unicorn foal with a pastel rainbow mane and a golden horn", "standing proudly on top of a golden trophy cup, horn glowing"),
    "coinling": ("a living shiny gold coin with a star emblem on its face area, tiny arms and legs", "spinning on its edge with sparkles"),
}

EGG_HINTS = [
    "cream with a green hood band (adventurer)", "glossy translucent mint jelly (slime)", "wooden planks with iron bands (mimic)",
    "green with a patched brown band (goblin)", "violet with pale spots (mushroom)", "pale blue with a wavy band (ghost)",
    "warm orange painted wood (meeple)", "ruby red faceted like a gem (die)", "white with red diamond and black spade marks (card)",
    "teal metal with rivets and a tiny antenna (robot)", "emerald scales (dragon)", "green with a small iron helmet band (frog knight)",
    "black with pale green bone marks (necro cat)", "deep blue with stars and nebula (cosmic jelly)", "grey with a black mask stripe (raccoon)",
    "sandy brown with a straw band (capybara)", "pink with frilly dots (axolotl)", "orange with a white tip (fox)",
    "grey feathered with a dark band (owl)", "golden fluffy (hamster)", "black and white (penguin)", "lilac with a headphones band (bunny)",
    "brown with a knitted band (bear)", "orange tabby stripes (cat)", "orange and white with a red band (corgi)",
    "green hexagon shell pattern (tortoise)", "red with bumps (crab)", "dark purple with small wings (bat)",
    "mossy stone cubes, blocky (golem)", "pastel with butterfly-wing shimmer (fairy)", "bone white with cracks (skeleton)",
    "teal with suckers (octopus)", "yellow and black stripes (bee)", "flame gold and crimson, glowing (phoenix)",
    "pearly white with rainbow swirl, glowing (unicorn)", "shiny gold with a star, glowing (coin)",
]


def pet_prompt(desc, signature):
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet for a cozy gaming-diary app pet, a creature that lives in an adventurers' tavern: {desc}.
{STYLE}
{GRID}
Cells, left to right, top to bottom:
1) baby: tiny, round, stubby limbs, very cute
2) teen: bigger, a bit more confident
3) adult: grown, content, standing proud
4) adult {signature}
5) adult cheering, jumping with joy, paws (or arms) up in triumph
6) adult asleep, curled up, peaceful face
The same character and palette in every cell, facing three-quarters right."""


def eggs_prompt(hints):
    return """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 18 creature eggs for a cozy gaming-diary app set in an adventurers' tavern.
""" + OBJ_STYLE + """
Layout: 6 columns x 3 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One egg per cell, centered, with clear margin; no text, no grid lines.
Each egg is the same size and egg-shaped, but with its own pattern and colours that hint at the creature inside, in this order (left to right, top to bottom):
""" + ", ".join(f"{i + 1} {h}" for i, h in enumerate(hints)) + "."


def banner(desc):
    return """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a wide banner scene for a cozy gaming-diary app, seen from the front, side-on like a 2D game background: """ + desc + """
""" + STYLE + """ Wide panorama, aspect ratio 3:1. No people, no creatures, no letters, numbers or text anywhere."""


OTHER = {
    "eggs1": eggs_prompt(EGG_HINTS[:18]),
    "eggs2": eggs_prompt(EGG_HINTS[18:]),
    "tavern": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy gaming-diary app: the inside of a small cozy fantasy tavern for adventurers and gamers, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Landscape 3:2.
The room: warm wooden plank walls with dark beams, a stone fireplace with a crackling fire on the RIGHT, a tall arched window on the LEFT wall whose glass is filled with flat solid pure magenta (#FF00FF), a wooden plank floor with a round crimson rug with a gold pattern in the middle, a couple of hanging lanterns giving warm light.
IMPORTANT: keep the room mostly EMPTY: no furniture, no tables, no creatures, no people, no text. The centre of the floor and the back wall between the window and the fireplace must be clear free space (furniture will be added later).
Only small details: a lantern on a hook, a few hanging herbs on a beam, a small mug on the fireplace mantel.""",
    "items": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 furniture objects for a cozy fantasy gamers' tavern, each a separate object, seen from the front (side-on like a 2D game room).
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a round wooden tavern table with a board game laid out on it (tiles, wooden pieces, cards, dice), 2 a retro arcade cabinet with a glowing screen showing abstract shapes, 3 a cozy armchair with a knitted blanket, 4 an old CRT television on a wooden stand with a game console and a controller,
5 a tall wooden shelf full of colourful board game boxes, 6 a wooden shelf with golden trophy cups, 7 a glowing retro jukebox, 8 a wooden notice board with pinned parchment notes (no readable text),
9 a pinball machine with lights, 10 a stack of wooden ale barrels, 11 a dartboard on a wooden stand, 12 a magic portal door in a stone arch with a swirling green forest inside,
13 a magic portal door in a metal sci-fi frame with swirling starry space inside, 14 a magic portal door in a crooked dark wooden frame with misty purple fog inside, 15 a big soft teal bean bag chair, 16 a hanging crimson banner with a crossed sword and gamepad emblem (no letters).""",
    "props": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 small objects for a cozy gaming-diary app set in an adventurers' tavern, each separate, front view.
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a game controller, 2 a red twenty-sided die (dots instead of numbers), 3 a wooden orange meeple game piece, 4 a single shiny gold coin with a star,
5 a small heap of gold coins, 6 a bronze trophy cup, 7 a silver trophy cup, 8 a gold trophy cup,
9 a platinum trophy cup with a pale blue shine, 10 a fan of five playing cards, 11 a brass hourglass, 12 a rolled quest scroll tied with a red ribbon,
13 a small wooden treasure chest, 14 a speckled creature egg in a nest of straw, 15 a round red health potion flask, 16 an old instant camera.""",
    "hall": banner("a trophy hall in an adventurers' guild: tall stone arches, wooden shelves and pedestals with golden, silver and bronze trophy cups, medals on ribbons, hanging banners, warm torchlight."),
    "journal": banner("an adventurers' map room: a big wooden table with an unrolled world map, small figurines and dice on it, a quill and ink, shelves of journals and scrolls, a round window with sunset sky, lanterns."),
    "shelf": banner("a cozy game library corner of a tavern: tall wooden shelves packed with colourful board game boxes and game cartridges and discs, an armchair, a reading lamp, a ladder, warm evening light."),
}


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    for attempt in range(2):
        r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                           input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
        p = image_of((r.stdout or "") + (r.stderr or ""))
        if p is not None:
            shutil.copy(p, dst)
            print("ok", name, p, flush=True)
            return
    print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = list(OTHER.items()) + [(k, pet_prompt(*v)) for k, v in PETS.items()]
    jobs = [j for j in jobs if not want or j[0] in want]
    with ThreadPoolExecutor(4) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
