#!/usr/bin/env python3
"""Generate the mod icon.

Built on the same architecture as Warband's logo script: a graded sky, a glowing
celestial body, one very large hand-plotted letter with a gradient fill and a double
glow, big mobs standing in front of it, and a jagged horizon.

Tamekind is the passive-AI counterpart and inverts the mood: a midday blue sky instead
of midnight, a ray-casting sun instead of a cold moon, blossom pink into crimson
instead of gold, sunlit pasture instead of a black void, and livestock instead of
hostiles. Green is deliberately unused, since that is Hearthfolk's emerald identity.

The one real departure is that Tamekind's animals are shown in full colour where
Warband silhouettes its mobs. Silhouetting works there because a zombie, a skeleton and
a creeper have distinctive limbed outlines and a face that punches through in white. A
cow, a sheep and a chicken reduced to flat black are three indistinguishable lumps that
also merge into the ground, because livestock are short and wide rather than tall. Both
modes are supported; see the "silhouette" preset key.

    python scripts/make_icon.py --fetch-mobs           # shipped 128px icon
    python scripts/make_icon.py --fetch-mobs --size 512 --out /tmp/preview.png
    python scripts/make_icon.py --fetch-mobs --preset warband   # the sibling palette
    python scripts/make_icon.py --mobs-dir D:/Downloads         # local renders instead

Mobs come from real Minecraft renders, silhouetted on alpha or used as-is. Warband keeps
those PNGs loose in a downloads folder, which means its logo cannot actually be
regenerated from a fresh clone. Two things fix that here: --fetch-mobs pulls the renders
it needs into a gitignored cache, so the committed icon is reproducible; and if no render
is available at all, shapes are drawn procedurally from rectangles so the script still
runs standalone, though in a colour scene that fallback is visibly degraded.

The renders themselves are not committed. They are third-party assets, and a build
input does not belong in the tree when one flag can fetch it.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import math
import random
import sys
import urllib.parse
import urllib.request

try:
    from PIL import Image, ImageDraw, ImageFilter
except ImportError:
    sys.exit("Pillow is required: pip install Pillow")

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT_OUT = ROOT / "src" / "main" / "resources" / "assets" / "tamekind" / "icon.png"

NATIVE = 512  # everything is composed at this size, then reduced to the target

# Letters are plotted by hand rather than set in a font. A font glyph carries its own
# optical corrections and hinting, which fight the flat blocky look, and the polygon is
# also what lets the gradient and the glow line up exactly.
LETTERS: dict[str, list[tuple[int, int]]] = {
    "T": [(148, 100), (364, 100), (364, 166), (286, 166),
          (286, 326), (226, 326), (226, 166), (148, 166)],
    "W": [(131, 110), (176, 110), (216, 240), (236, 160),
          (276, 160), (296, 240), (336, 110), (381, 110),
          (326, 310), (276, 310), (256, 230), (236, 310), (186, 310)],
}

PRESETS = {
    # Daytime pasture. A moon and stars belong to the mod about things that hunt you at
    # night; the passive-AI counterpart is a midday field. The sun sits directly behind
    # the letter, which gives the bloom a cause in the scene rather than making the
    # glyph luminous for no reason.
    "tamekind": {
        "letter": "T",
        "sky_top": (86, 152, 214),
        "sky_bottom": (194, 212, 170),
        "stars": 0,
        "star_max_y": 0,
        "orb_glow": (255, 238, 168, 180),
        "orb_glow_box": (96, 40, 340),      # x, y, diameter
        "orb": (255, 251, 228),
        "orb_box": (182, 126, 156),
        "orb_detail": (255, 251, 228),      # a sun has no craters
        "orb_craters": False,
        "sun_rays": {"count": 12, "inner": 0.46, "outer": 3.2,
                     "spread": 0.05, "colour": (255, 240, 182, 48)},
        # Blossom pink into deep crimson. Green is reserved for Hearthfolk, whose whole
        # identity is emerald, and gold belongs to Warband, so all three stay
        # distinguishable at thumbnail size.
        "letter_top": (255, 166, 186),
        "letter_bottom": (162, 28, 56),
        "letter_shadow": (66, 10, 24),
        # Warm bloom rather than a halo of the fill: a dark letter blurred over a bright
        # sky just smears into mud.
        "letter_glow": (255, 246, 206),
        "fog": (232, 238, 212),
        # Full colour, not silhouettes: see render_mob for why.
        "silhouette": False,
        "ground": (104, 158, 74),            # sunlit pasture
        "ground_fore": (78, 128, 56),        # a darker strip so the field has depth
        "contact_shadow": (28, 54, 22, 120),
        # Cow centre stage, overlapping the letter's stem the way Warband's creeper
        # overlaps its W. The chicken was there first and was too small to hold the middle:
        # it merged into the stem and the ground as one dark lump.
        #
        # The centre mob is a three-quarter view, not front-on like Warband's creeper.
        # That is a limit of the source, not a preference: every vanilla cow render on the
        # wiki is three-quarter, the inventory icon included, and the only front-facing
        # image is an opaque screenshot that cannot be silhouetted. A hand-drawn front view
        # was tried and read as a tractor. Drop a transparent front render in as cow.png
        # and this picks it up with no code change.
        "mobs": ("chicken", "cow", "sheep"),
        "mob_heights": (150, 195, 180),
        "mob_x": (18, "centre", 352),
        "clip": 34,
        "flip": "left",     # the side-on renders face left, so face the left one inward
    },
    "warband": {
        "letter": "W",
        "sky_top": (14, 20, 42),
        "sky_bottom": (3, 5, 12),
        "stars": 90,
        "star_max_y": 320,
        "orb_glow": (240, 240, 210, 160),
        "orb_glow_box": (86, 30, 340),
        "orb": (248, 246, 230),
        "orb_box": (172, 116, 168),
        "orb_detail": (218, 215, 195),
        "orb_craters": True,
        "sun_rays": None,           # a moon does not cast visible spokes
        "letter_top": (255, 240, 150),
        "letter_bottom": (240, 160, 50),
        "letter_shadow": (90, 50, 0),
        "letter_glow": None,                # bloom comes from the gold fill itself
        "fog": (180, 190, 210),
        "silhouette": True,
        "ground": (0, 0, 0),
        "contact_shadow": (0, 0, 0, 0),
        "mobs": ("zombie", "creeper", "skeleton"),
        "mob_heights": (250, 270, 250),
        "mob_x": (20, "centre", 350),
        "clip": 60,
        "flip": "right",
    },
}

# Minecraft mobs are boxes, so a procedural silhouette is a rectangle list. Fractions of
# the sprite's own bounding box: x0, y0, x1, y1, y running down from the top.
SPRITES: dict[str, list[tuple[float, float, float, float]]] = {
    "cow": [
        (0.22, 0.20, 0.92, 0.58),   # body
        (0.00, 0.28, 0.26, 0.62),   # head and snout
        (0.04, 0.19, 0.10, 0.30),   # horn
        (0.18, 0.19, 0.24, 0.30),   # horn
        (0.24, 0.30, 0.31, 0.39),   # ear
        (0.26, 0.58, 0.38, 1.00),   # legs
        (0.44, 0.58, 0.56, 1.00),
        (0.66, 0.58, 0.78, 1.00),
        (0.82, 0.58, 0.94, 1.00),
        (0.90, 0.24, 0.96, 0.50),   # tail
    ],
    "sheep": [
        (0.18, 0.12, 0.90, 0.62),   # wool, deliberately taller than a cow's body
        (0.00, 0.30, 0.24, 0.60),   # head
        (0.22, 0.62, 0.34, 1.00),   # legs
        (0.40, 0.62, 0.52, 1.00),
        (0.62, 0.62, 0.74, 1.00),
        (0.78, 0.62, 0.90, 1.00),
    ],
    "chicken": [
        (0.24, 0.34, 0.84, 0.74),   # body
        (0.08, 0.12, 0.40, 0.40),   # head
        (0.16, 0.03, 0.28, 0.14),   # comb
        (0.00, 0.22, 0.10, 0.32),   # beak
        (0.82, 0.22, 0.99, 0.52),   # tail
        (0.36, 0.74, 0.46, 1.00),   # legs
        (0.60, 0.74, 0.70, 1.00),
    ],
    "creeper": [
        (0.18, 0.00, 0.82, 0.34),
        (0.26, 0.34, 0.74, 0.78),
        (0.26, 0.78, 0.46, 1.00),
        (0.54, 0.78, 0.74, 1.00),
    ],
    "zombie": [
        (0.28, 0.00, 0.72, 0.24),
        (0.30, 0.24, 0.70, 0.66),
        (0.06, 0.26, 0.30, 0.40),
        (0.70, 0.26, 0.94, 0.40),
        (0.32, 0.66, 0.46, 1.00),
        (0.54, 0.66, 0.68, 1.00),
    ],
    "skeleton": [
        (0.30, 0.00, 0.70, 0.24),
        (0.38, 0.24, 0.62, 0.64),
        (0.10, 0.28, 0.38, 0.38),
        (0.62, 0.28, 0.90, 0.38),
        (0.36, 0.64, 0.46, 1.00),
        (0.54, 0.64, 0.64, 1.00),
    ],
}

# Mobs whose face should punch through as a light shape, the way Warband's creeper does.
FACE_HOLES: dict[str, list[tuple[float, float, float, float]]] = {
    "creeper": [(0.32, 0.08, 0.44, 0.20), (0.56, 0.08, 0.68, 0.20),
                (0.42, 0.20, 0.58, 0.30)],
}


# Wiki file titles for --fetch-mobs. minecraft.wiki returns 403 to scripted requests,
# so the Fandom mirror is used; both serve the same official renders.
WIKI_TITLES = {
    "cow": "Cow.png",
    "sheep": "Sheep.png",
    "chicken": "Chicken.png",
    "zombie": "Zombie.png",
    "skeleton": "Skeleton.png",
    "creeper": "Creeper.png",
}
MOB_CACHE = ROOT / "scripts" / ".mob-cache"
WIKI_API = "https://minecraft.fandom.com/api.php"
UA = {"User-Agent": "tamekind-icon-build (Pillow)"}


def fetch_mobs(names: tuple[str, ...], cache: pathlib.Path) -> pathlib.Path:
    """Download the mob renders this preset needs into a gitignored cache."""
    cache.mkdir(parents=True, exist_ok=True)
    for name in names:
        target = cache / f"{name}.png"
        if target.is_file():
            continue
        title = WIKI_TITLES.get(name)
        if title is None:
            print(f"  {name}: no wiki title mapped, will draw procedurally")
            continue
        query = urllib.parse.urlencode({
            "action": "query", "titles": f"File:{title}",
            "prop": "imageinfo", "iiprop": "url", "format": "json"})
        try:
            with urllib.request.urlopen(
                    urllib.request.Request(f"{WIKI_API}?{query}", headers=UA), timeout=30) as r:
                pages = json.load(r)["query"]["pages"]
            url = next(p["imageinfo"][0]["url"] for p in pages.values() if p.get("imageinfo"))
            with urllib.request.urlopen(
                    urllib.request.Request(url, headers=UA), timeout=30) as r:
                data = r.read()
        except Exception as exc:                      # noqa: BLE001 - any failure falls back
            print(f"  {name}: fetch failed ({exc}), will draw procedurally")
            continue
        # The mirror may serve WebP under a .png title. Normalise so the cache is real PNG.
        import io
        Image.open(io.BytesIO(data)).convert("RGBA").save(target, "PNG")
        print(f"  {name}: cached from {title}")
    return cache


def vertical_gradient(size: int, top: tuple, bottom: tuple) -> Image.Image:
    column = Image.new("RGB", (1, size))
    px = column.load()
    for y in range(size):
        t = y / max(1, size - 1)
        px[0, y] = tuple(round(a + (b - a) * t) for a, b in zip(top, bottom))
    return column.resize((size, size), Image.NEAREST)


# Side-on quadrupeds are wider than tall, where a wiki render is portrait. Heights are
# tuned for the renders, so the fallback shrinks these to keep the same footprint rather
# than ballooning across the frame.
WIDE_SPRITES = {"cow": 1.45, "sheep": 1.45}


def procedural_silhouette(name: str, height: int) -> Image.Image:
    """Blocky mob silhouette drawn from rectangles, with a light face detail."""
    rects = SPRITES[name]
    aspect = WIDE_SPRITES.get(name)
    if aspect:
        height = round(height * 0.62)
    width = round(height * (aspect or 0.85))
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for x0, y0, x1, y1 in rects:
        draw.rectangle([round(x0 * width), round(y0 * height),
                        round(x1 * width), round(y1 * height)], fill=(0, 0, 0, 255))
    for x0, y0, x1, y1 in FACE_HOLES.get(name, []):
        draw.rectangle([round(x0 * width), round(y0 * height),
                        round(x1 * width), round(y1 * height)], fill=(255, 255, 255, 255))
    return img


def render_mob(name: str, height: int, mobs_dir: pathlib.Path | None,
               silhouette: bool) -> Image.Image:
    """A real mob render when one is available, otherwise the procedural fallback.

    Silhouetting is Warband's language, and it works there because a zombie, a skeleton
    and a creeper have distinctive outlines with limbs, plus a face that punches through
    in white. Livestock have none of that: a cow, a sheep and a chicken reduced to flat
    black are three indistinguishable lumps. So Tamekind keeps them in full colour, which
    is also the honest choice for a mod about animals you are meant to look after.
    """
    if mobs_dir is not None:
        path = mobs_dir / f"{name}.png"
        if path.is_file():
            mob = Image.open(path).convert("RGBA")
            width = round(height * mob.width / mob.height)
            mob = mob.resize((width, height), Image.LANCZOS)
            if not silhouette:
                return mob
            out = Image.new("RGBA", mob.size, (0, 0, 0, 0))
            alpha = mob.getchannel("A").point(lambda a: 255 if a > 128 else 0)
            out.paste((0, 0, 0, 255), (0, 0), alpha)
            return out
    # Shape only, so this is a degraded fallback in a colour scene. --fetch-mobs avoids it.
    return procedural_silhouette(name, height)


def render(preset: dict, mobs_dir: pathlib.Path | None) -> Image.Image:
    s = NATIVE
    img = vertical_gradient(s, preset["sky_top"], preset["sky_bottom"]).convert("RGBA")
    draw = ImageDraw.Draw(img)

    # Stars, as blocky pixels rather than dots. Seeded so the icon is reproducible.
    random.seed(7)
    for _ in range(preset["stars"]):
        sx = random.randint(0, s - 1)
        sy = random.randint(0, preset["star_max_y"])
        v = random.randint(150, 240)
        w = 3 if random.randint(0, 9) < 2 else 2
        draw.rectangle([sx, sy, sx + w - 1, sy + w - 1], fill=(v, v, v, 255))

    # Celestial glow, heavily blurred so it reads as light rather than as a shape.
    gx, gy, gd = preset["orb_glow_box"]
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([gx, gy, gx + gd, gy + gd], fill=preset["orb_glow"])
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(45)))
    draw = ImageDraw.Draw(img)

    # Sun rays, drawn before the disc so they read as emanating from behind it. Each spoke
    # is a quad that widens outward, blurred just enough to lose its hard polygon edges
    # without turning into a smear.
    rays = preset.get("sun_rays")
    if rays:
        rx, ry, rd = preset["orb_box"]
        cx, cy = rx + rd / 2, ry + rd / 2
        r_in, r_out = rd * rays["inner"], rd * rays["outer"]
        layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        for i in range(rays["count"]):
            a = i * 2 * math.pi / rays["count"] + 0.13   # offset so none sits dead vertical
            h_in, h_out = 0.012, rays["spread"]
            ld.polygon([
                (cx + r_in * math.cos(a - h_in), cy + r_in * math.sin(a - h_in)),
                (cx + r_out * math.cos(a - h_out), cy + r_out * math.sin(a - h_out)),
                (cx + r_out * math.cos(a + h_out), cy + r_out * math.sin(a + h_out)),
                (cx + r_in * math.cos(a + h_in), cy + r_in * math.sin(a + h_in)),
            ], fill=rays["colour"])
        img = Image.alpha_composite(img, layer.filter(ImageFilter.GaussianBlur(6)))
        draw = ImageDraw.Draw(img)

    ox, oy, od = preset["orb_box"]
    draw.ellipse([ox, oy, ox + od, oy + od], fill=preset["orb"] + (255,))
    if preset.get("orb_craters"):
        for fx, fy, fd in ((0.20, 0.25, 0.16), (0.61, 0.49, 0.12),
                           (0.39, 0.69, 0.10), (0.13, 0.61, 0.07)):
            draw.ellipse([ox + od * fx, oy + od * fy,
                          ox + od * (fx + fd), oy + od * (fy + fd)],
                         fill=preset["orb_detail"] + (255,))

    # The letter: a solid polygon used as a mask for a vertical gradient, a hard offset
    # shadow, then a wide and a tight blur of the fill stacked underneath so the glyph
    # sits in its own light.
    pts = LETTERS[preset["letter"]]
    ys = [p[1] for p in pts]
    xs = [p[0] for p in pts]
    base = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(base).polygon(pts, fill=(255, 255, 255, 255))

    grad = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gd_draw = ImageDraw.Draw(grad)
    top, bot = min(ys), max(ys)
    for y in range(top, bot + 1):
        t = (y - top) / max(1, bot - top)
        gd_draw.line([(min(xs), y), (max(xs), y)],
                     fill=tuple(round(a + (b - a) * t) for a, b in
                                zip(preset["letter_top"], preset["letter_bottom"])) + (255,))
    filled = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    filled.paste(grad, (0, 0), base.getchannel("A"))

    shadow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon([(x + 8, y + 8) for x, y in pts],
                                   fill=preset["letter_shadow"] + (255,))

    # The halo source: the fill itself when the letter is lighter than the sky, or a
    # flat warm colour when it is darker and its own blur would only muddy things.
    halo = filled
    if preset.get("letter_glow"):
        halo = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        halo.paste(preset["letter_glow"] + (255,), (0, 0), base.getchannel("A"))

    img = Image.alpha_composite(img, halo.filter(ImageFilter.GaussianBlur(18)))
    img = Image.alpha_composite(img, shadow)
    img = Image.alpha_composite(img, halo.filter(ImageFilter.GaussianBlur(6)))
    img = Image.alpha_composite(img, filled)

    horizon = 440
    clip = preset["clip"]  # how far the mobs' feet sink below the horizon

    # Haze behind the mobs, so they are not washed out by the pass in front of them.
    back = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    bd = ImageDraw.Draw(back)
    bd.ellipse([-50, horizon - 120, 250, horizon + 60], fill=preset["fog"] + (44,))
    bd.ellipse([150, horizon - 140, 550, horizon + 80], fill=preset["fog"] + (38,))
    img = Image.alpha_composite(img, back.filter(ImageFilter.GaussianBlur(35)))

    silhouette = preset.get("silhouette", True)
    ground = preset["ground"]
    jagged = [(0, s), (0, horizon), (128, horizon - 6), (256, horizon + 6),
              (384, horizon - 6), (s, horizon + 2), (s, s)]

    left_name, mid_name, right_name = preset["mobs"]
    lh, mh, rh = preset["mob_heights"]
    lx, _, rx = preset["mob_x"]
    flip = preset.get("flip")

    def place(name: str, height: int) -> Image.Image:
        mob = render_mob(name, height, mobs_dir, silhouette)
        return mob

    left, mid, right = place(left_name, lh), place(mid_name, mh), place(right_name, rh)
    if flip == "left":
        left = left.transpose(Image.FLIP_LEFT_RIGHT)
    elif flip == "right":
        right = right.transpose(Image.FLIP_LEFT_RIGHT)
    placements = [(left, lx), (right, min(rx, s - right.width)),
                  (mid, (s - mid.width) // 2)]

    if silhouette:
        # Warband's order: outer mobs, then the ground cutting them off at the knee, then
        # haze, then the centre mob on top so it stays crisp.
        img.alpha_composite(left, (lx, horizon - left.height + clip))
        img.alpha_composite(right, (min(rx, s - right.width), horizon - right.height + clip))
        ImageDraw.Draw(img).polygon(jagged, fill=ground + (255,))
        front = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        fd = ImageDraw.Draw(front)
        fd.ellipse([-100, horizon - 60, 200, horizon + 120], fill=preset["fog"] + (28,))
        fd.ellipse([300, horizon - 80, 650, horizon + 140], fill=preset["fog"] + (22,))
        img = Image.alpha_composite(img, front.filter(ImageFilter.GaussianBlur(25)))
        img.alpha_composite(mid, ((s - mid.width) // 2, horizon - mid.height + clip))
        return img.convert("RGB")

    # Colour scene: the grass goes down first and every animal stands on top of it, since
    # the whole point is that they are visible. Each gets a contact shadow so it is planted
    # in the field rather than pasted over it.
    ImageDraw.Draw(img).polygon(jagged, fill=ground + (255,))
    if preset.get("ground_fore"):
        ImageDraw.Draw(img).polygon(
            [(0, s), (0, horizon + 44), (256, horizon + 60), (s, horizon + 40), (s, s)],
            fill=preset["ground_fore"] + (255,))

    for mob, x in placements:
        feet = horizon - mob.height + clip + mob.height
        shade = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        ImageDraw.Draw(shade).ellipse(
            [x + mob.width * 0.08, feet - mob.height * 0.07,
             x + mob.width * 0.92, feet + mob.height * 0.05],
            fill=preset["contact_shadow"])
        img = Image.alpha_composite(img, shade.filter(ImageFilter.GaussianBlur(7)))
        img.alpha_composite(mob, (x, horizon - mob.height + clip))

    return img.convert("RGB")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--preset", default="tamekind", choices=sorted(PRESETS))
    ap.add_argument("--size", type=int, default=128, help="output edge in px")
    ap.add_argument("--out", type=pathlib.Path, default=None)
    ap.add_argument("--mobs-dir", type=pathlib.Path, default=None,
                    help="directory of <mob>.png renders to silhouette")
    ap.add_argument("--fetch-mobs", action="store_true",
                    help="download the renders this preset needs into scripts/.mob-cache")
    args = ap.parse_args()

    preset = PRESETS[args.preset]
    mobs_dir = args.mobs_dir
    if args.fetch_mobs:
        print(f"fetching mob renders for preset {args.preset}:")
        mobs_dir = fetch_mobs(preset["mobs"], MOB_CACHE)
    elif mobs_dir is None and MOB_CACHE.is_dir():
        mobs_dir = MOB_CACHE          # reuse a previous fetch without asking again

    out = args.out or DEFAULT_OUT
    out.parent.mkdir(parents=True, exist_ok=True)
    img = render(preset, mobs_dir)
    if args.size != NATIVE:
        img = img.resize((args.size, args.size), Image.LANCZOS)
    img.save(out, "PNG", optimize=True)
    print(f"wrote {out} ({args.size}x{args.size}, preset {args.preset})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
