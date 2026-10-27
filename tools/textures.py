"""Arcane Quest textures, drawn in a sleek "PvP pack" style:
32x32 dark tools with black handles and a glowing colored edge, smooth gradient ingots and gems,
gradient armor icons with a white highlight, framed ores and soft gradient blocks.

Everything is generated from code: no texture from Minecraft or any resource pack is copied.
"""
import math
import random
from pathlib import Path

from PIL import Image

BLACK = (0, 0, 0)
DARK = (18, 18, 20)
HANDLE = (34, 34, 38)
HANDLE_LIGHT = (62, 62, 68)
WHITE = (255, 255, 255)
GOLD = (255, 200, 60)

MATERIALS = {
    "mithril": (110, 200, 255),
    "arcanite": (190, 80, 255),
    "adamantite": (235, 30, 70),
    "gold": GOLD,
    "bone": (230, 225, 205),
    "fire": (255, 120, 20),
    "frost": (110, 230, 255),
    "heal": (255, 110, 170),
    "lightning": (255, 235, 70),
    "dark": (80, 30, 130),
}


# ---------------------------------------------------------------------------
# Color and geometry helpers
# ---------------------------------------------------------------------------

def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(color, factor):
    return tuple(clamp(c * factor) for c in color)


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(clamp(x + (y - x) * t) for x, y in zip(a, b))


def mix_white(color, t):
    return lerp(color, WHITE, t)


def bezier(p0, p1, p2, steps=80):
    for i in range(steps + 1):
        t = i / steps
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t ** 2 * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t ** 2 * p2[1]
        yield t, x, y


def segment(p0, p1, steps=None):
    steps = steps or int(max(abs(p1[0] - p0[0]), abs(p1[1] - p0[1])) * 2) + 1
    for i in range(steps + 1):
        t = i / steps
        yield t, p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t


def inside(px, py, vertices):
    result = False
    n = len(vertices)
    for i in range(n):
        x0, y0 = vertices[i]
        x1, y1 = vertices[(i + 1) % n]
        if (y0 > py) != (y1 > py) and px < (x1 - x0) * (py - y0) / (y1 - y0) + x0:
            result = not result
    return result


def smooth_noise(seed, size=16, cells=4):
    """Bilinear value noise in [0, 1]."""
    rng = random.Random(seed)
    grid = [[rng.random() for _ in range(cells + 1)] for _ in range(cells + 1)]
    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            gx, gy = x / size * cells, y / size * cells
            ix, iy = int(gx), int(gy)
            fx, fy = gx - ix, gy - iy
            fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
            top = grid[iy][ix] * (1 - fx) + grid[iy][ix + 1] * fx
            bottom = grid[iy + 1][ix] * (1 - fx) + grid[iy + 1][ix + 1] * fx
            out[y][x] = top * (1 - fy) + bottom * fy
    return out


class Canvas:
    """Pixel canvas that remembers which pixels are filled, so it can be outlined."""

    def __init__(self, size=32):
        self.size = size
        self.pixels = {}

    def put(self, x, y, color):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.size and 0 <= y < self.size:
            self.pixels[(x, y)] = color

    def brush(self, x, y, radius, color):
        r = int(math.ceil(radius))
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= radius * radius + 0.25:
                    self.put(x + dx, y + dy, color)

    def stroke(self, points, radius, color_fn):
        for t, x, y in points:
            self.brush(x, y, radius(t) if callable(radius) else radius, color_fn(t))

    def fill_polygon(self, vertices, color_fn):
        xs = [v[0] for v in vertices]
        ys = [v[1] for v in vertices]
        for y in range(int(min(ys)), int(max(ys)) + 1):
            for x in range(int(min(xs)), int(max(xs)) + 1):
                if inside(x + 0.5, y + 0.5, vertices):
                    self.put(x, y, color_fn(x, y))

    def image(self, outline=BLACK):
        img = Image.new("RGBA", (self.size, self.size), (0, 0, 0, 0))
        for (x, y), color in self.pixels.items():
            img.putpixel((x, y), color + (255,))
        if outline is not None:
            for x in range(self.size):
                for y in range(self.size):
                    if (x, y) in self.pixels:
                        continue
                    if any((x + dx, y + dy) in self.pixels for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                        img.putpixel((x, y), outline + (255,))
        return img


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


# ---------------------------------------------------------------------------
# Tools and weapons (32x32)
# ---------------------------------------------------------------------------

def handle(canvas, start, end, color=HANDLE, light=HANDLE_LIGHT):
    """Two pixel wide dark handle with a lighter center line."""
    for t, x, y in segment(start, end):
        canvas.put(x, y, color)
        canvas.put(x + 1, y, shade(color, 0.7))
    for t, x, y in segment(start, end):
        if 0.15 < t < 0.85:
            canvas.put(x, y, lerp(color, light, 0.6))


def pommel(canvas, accent):
    for x, y in ((2, 29), (3, 30), (2, 30)):
        canvas.put(x, y, accent)
    canvas.put(2, 29, mix_white(accent, 0.4))


def sword(accent, core_dark=DARK, guard=DARK, grip=HANDLE):
    c = Canvas()
    handle(c, (4, 28), (9, 23), grip)
    pommel(c, accent)
    # Blade: dark core fading to the accent color at the tip, bright edge on one side.
    base, tip = (11, 21), (28, 4)
    for t, x, y in segment(base, tip):
        c.put(x, y, lerp(core_dark, shade(accent, 0.55), t ** 1.5))
        c.put(x - 1, y, lerp(core_dark, shade(accent, 0.4), t ** 1.5))
        c.put(x, y + 1, lerp(shade(accent, 0.45), shade(accent, 1.15), t))
    c.put(28, 3, mix_white(accent, 0.6))
    c.put(29, 3, mix_white(accent, 0.3))
    # Cross guard
    for t, x, y in segment((6, 18), (14, 26)):
        c.put(x, y, guard)
        c.put(x + 1, y, shade(guard, 0.6) if guard != DARK else (40, 40, 44))
    for x, y in ((6, 18), (14, 26)):
        c.put(x, y, accent)
    return c.image()


def pickaxe(accent):
    c = Canvas()
    handle(c, (3, 28), (19, 12))
    pommel(c, accent)
    p0, p1, p2 = (7, 8), (25, 2), (26, 25)
    c.stroke(bezier(p0, p1, p2), lambda t: 0.6 + 1.1 * math.sin(math.pi * t),
             lambda t: lerp(DARK, accent, abs(t - 0.5) * 2.2))
    # Bright outer edge
    for t, x, y in bezier((8, 6), (26, 0), (28, 24)):
        if 0.05 < t < 0.95:
            c.put(x, y, lerp(shade(accent, 0.6), mix_white(accent, 0.25), abs(t - 0.5) * 2))
    return c.image()


def axe(accent):
    c = Canvas()
    handle(c, (3, 28), (23, 8))
    pommel(c, accent)
    # Blade attached along the top of the handle, widening toward its edge (upper-left).
    nx, ny = -math.sqrt(0.5), -math.sqrt(0.5)
    p, q = (18, 13), (22, 9)
    head = [p, q, (18.5, 2.0), (11.0, 9.5)]

    def color(x, y):
        t = ((x + 0.5 - p[0]) * nx + (y + 0.5 - p[1]) * ny) / 7.5
        col = lerp(DARK, accent, t * 1.3)
        return mix_white(col, 0.35) if t > 0.85 else col

    c.fill_polygon(head, color)
    return c.image()


def shovel(accent):
    c = Canvas()
    handle(c, (3, 28), (18, 13))
    pommel(c, accent)
    cx, cy = 23, 8
    for y in range(32):
        for x in range(32):
            u = ((x - cx) + (y - cy)) / math.sqrt(2)
            v = ((x - cx) - (y - cy)) / math.sqrt(2)
            if (u / 4.2) ** 2 + (v / 6.2) ** 2 <= 1:
                d = math.hypot(x - 18, y - 13)
                c.put(x, y, lerp(shade(accent, 0.3), mix_white(accent, 0.15), (d - 3) / 9))
    c.put(23, 4, mix_white(accent, 0.7))
    c.put(24, 4, mix_white(accent, 0.4))
    return c.image()


def hoe(accent):
    c = Canvas()
    handle(c, (3, 28), (20, 11))
    pommel(c, accent)
    c.stroke(segment((21, 10), (24, 6)), 0.8, lambda t: DARK)
    c.stroke(segment((24, 6), (12, 4)), lambda t: 1.6 - 0.6 * t, lambda t: lerp(DARK, accent, t * 1.4))
    c.put(13, 4, mix_white(accent, 0.5))
    return c.image()


def hammer(accent):
    c = Canvas()
    handle(c, (3, 28), (17, 14))
    pommel(c, GOLD)
    head = [(12, 11), (20, 3), (29, 11), (21, 19)]
    c.fill_polygon(head, lambda x, y: lerp(mix_white(accent, 0.2), shade(accent, 0.45), (x + y - 18) / 24))
    for t, x, y in segment((16, 7), (25, 16)):
        c.put(x, y, GOLD)
    c.put(20, 4, mix_white(accent, 0.7))
    return c.image()


def staff(orb, shaft=HANDLE, collar=GOLD):
    c = Canvas()
    handle(c, (3, 28), (20, 11), shaft, lerp(shaft, WHITE, 0.25))
    for x, y in ((19, 11), (20, 12), (21, 11), (20, 10), (21, 12)):
        c.put(x, y, collar)
    cx, cy, r = 24.0, 7.5, 4.3
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                glow = 1 - math.hypot(x - (cx - 1.3), y - (cy - 1.3)) / (r * 1.6)
                c.put(x, y, mix_white(shade(orb, 0.55 + 0.6 * glow), max(0.0, glow - 0.45)))
    c.put(22, 5, WHITE)
    c.put(23, 5, mix_white(orb, 0.6))
    return c.image()


# ---------------------------------------------------------------------------
# Materials (32x32)
# ---------------------------------------------------------------------------

def ingot(accent):
    c = Canvas()
    a, b, cc, d = (5, 15), (16, 9), (27, 14), (16, 20)
    h = 6
    c.fill_polygon([a, d, (d[0], d[1] + h), (a[0], a[1] + h)],
                   lambda x, y: lerp(shade(accent, 0.95), shade(accent, 0.75), (y - 15) / 11))
    c.fill_polygon([d, cc, (cc[0], cc[1] + h), (d[0], d[1] + h)],
                   lambda x, y: lerp(shade(accent, 0.7), shade(accent, 0.5), (x - 16) / 11))
    c.fill_polygon([a, b, cc, d], lambda x, y: lerp(mix_white(accent, 0.45), mix_white(accent, 0.1), (x - 5) / 22))
    for t, x, y in segment(a, d):
        c.put(x, y, mix_white(accent, 0.6))
    for t, x, y in segment(d, cc):
        c.put(x, y, mix_white(accent, 0.35))
    return c.image(outline=shade(accent, 0.25))


def raw_chunk(accent, seed):
    c = Canvas()
    rng = random.Random(seed)
    blobs = [(12, 17, 6.5), (19, 13, 6.0), (19, 20, 5.0)]
    for y in range(32):
        for x in range(32):
            if any(math.hypot(x - bx, y - by) <= r for bx, by, r in blobs):
                f = 1.15 - (y - 7) / 22 + rng.uniform(-0.08, 0.08)
                c.put(x, y, shade(accent, max(0.45, f)))
    for x, y in ((11, 12), (12, 12), (18, 8), (19, 8)):
        c.put(x, y, mix_white(accent, 0.6))
    return c.image(outline=shade(accent, 0.25))


def gem(accent, inner=None):
    c = Canvas()
    cx, cy = 16, 16

    def octagon(rx, ry):
        return [(cx + rx * math.cos(math.radians(a)), cy + ry * math.sin(math.radians(a)))
                for a in range(22, 382, 45)]

    outer = octagon(9.5, 12.5)
    c.fill_polygon(outer, lambda x, y: lerp(mix_white(accent, 0.15), shade(accent, 0.45), (y - 4) / 24))
    c.fill_polygon(octagon(5.5, 8), lambda x, y: lerp(mix_white(inner or accent, 0.45), inner or accent, (y - 8) / 16))
    for t, x, y in segment((12, 7), (9, 13)):
        c.put(x, y, mix_white(accent, 0.8))
    c.put(13, 9, WHITE)
    return c.image(outline=shade(accent, 0.25))


# ---------------------------------------------------------------------------
# 16x16 icons from masks: '#' gradient fill, 'k' outline, 'i' dark inside, 'h' highlight
# ---------------------------------------------------------------------------

HELMET = [
    "", "", "",
    "...kkkkkkkkkk",
    "..kh#########k",
    "..k##########k",
    "..k###kkkk###k",
    "..k##kiiiik##k",
    "..k##kiiiik##k",
    "..kkkk....kkkk",
]
CHESTPLATE = [
    "",
    "..kkkk....kkkk",
    ".kh###kkkk####k",
    ".k############k",
    ".k############k",
    ".kkk########kkk",
    "...k########k",
    "...k########k",
    "...k########k",
    "...k########k",
    "...k########k",
    "...kkkkkkkkkk",
]
LEGGINGS = [
    "",
    "..kkkkkkkkkkkk",
    "..kh#########k",
    "..k##########k",
    "..k####kk####k",
    "..k###k..k###k",
    "..k###k..k###k",
    "..k###k..k###k",
    "..k###k..k###k",
    "..k###k..k###k",
    "..k###k..k###k",
    "..kkkkk..kkkkk",
]
BOOTS = [
    "", "", "", "", "",
    "..kkkk....kkkk",
    "..kh#k....kh#k",
    "..k##k....k##k",
    "..k##kkk..k##kkk",
    "..k####k..k####k",
    "..kkkkkk..kkkkkk",
]
SKULL = [
    "",
    "....kkkkkkkk",
    "...kh#######k",
    "..k##########k",
    "..k##########k",
    "..k#iii##iii#k",
    "..k#iii##iii#k",
    "..k####ii####k",
    "...k########k",
    "...k#i#i#i#ik",
    "....kkkkkkkk",
    "....gggggggg",
    "...g.g.g.g.g",
]
GRIMOIRE = [
    "",
    "..kkkkkkkkkkkk",
    "..kh#########k",
    "..k#gg#######k",
    "..k#gg##aa###k",
    "..k####aaaa##k",
    "..k####aaaa##k",
    "..k#####aa###k",
    "..k##########k",
    "..k##########k",
    "..kwwwwwwwwwwk",
    "..kkkkkkkkkkkk",
]


def mask_icon(rows, accent, extra=None):
    extra = extra or {}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    filled = [i for i, row in enumerate(rows) if row.strip(".")]
    top, bottom = (filled[0], filled[-1]) if filled else (0, 15)
    for y, row in enumerate(rows[:16]):
        for x, ch in enumerate(row.ljust(16, ".")[:16]):
            t = (y - top) / max(1, bottom - top)
            if ch == "#":
                color = lerp(mix_white(accent, 0.2), shade(accent, 0.6), t)
            elif ch == "h":
                color = WHITE
            elif ch == "k":
                color = BLACK
            elif ch == "i":
                color = shade(accent, 0.18)
            elif ch == "w":
                color = (235, 230, 215)
            elif ch == "g":
                color = GOLD
            elif ch in extra:
                color = lerp(mix_white(extra[ch], 0.4), extra[ch], t)
            else:
                continue
            img.putpixel((x, y), color + (255,))
    return img


# ---------------------------------------------------------------------------
# Blocks (16x16)
# ---------------------------------------------------------------------------

STONE = (126, 126, 128)
DEEPSLATE = (68, 68, 76)


def base_texture(color, seed, streaks=False):
    noise = smooth_noise(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            f = 0.88 + 0.22 * noise[y][x]
            if streaks:
                f += 0.05 * math.sin(y * 1.7)
            img.putpixel((x, y), shade(color, f) + (255,))
    return img


def ore(base, accent):
    """Framed ore: light outer ring, colored ring, dark shadow ring, stone in the middle."""
    img = base.copy()
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            light = 1.12 - 0.02 * (x + y)  # light from the top-left
            if ring == 0:
                color = shade(mix_white(accent, 0.45), light)
            elif ring == 1:
                color = shade(accent, light)
            elif ring == 2:
                color = shade(accent, 0.55 * light)
            elif ring == 3:
                color = shade(img.getpixel((x, y))[:3], 0.55)
            else:
                continue
            img.putpixel((x, y), color + (255,))
    return img


def metal_block(accent, seed):
    noise = smooth_noise(seed, cells=3)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            f = 0.85 + 0.2 * noise[y][x] + 0.06 * math.cos(y * math.pi / 2) - 0.008 * (x + y)
            color = mix_white(accent, 0.25) if (y % 4 == 0 and 0 < x < 15) else accent
            img.putpixel((x, y), shade(color, f) + (255,))
    return img


def crystal_block(accent, seed):
    noise = smooth_noise(seed, cells=3)
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(mix_white(accent, 0.15 * noise[y][x]), 0.7 + 0.45 * noise[y][x]) + (255,))
    for _ in range(6):
        img.putpixel((rng.randrange(16), rng.randrange(16)), mix_white(accent, 0.8) + (255,))
    return img


def raw_block(accent, seed):
    noise = smooth_noise(seed, cells=8)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(accent, 0.55 + 0.55 * noise[y][x]) + (255,))
    return img


# ---------------------------------------------------------------------------
# Armor worn on the player (64x32, humanoid layout)
# ---------------------------------------------------------------------------

def fill_face(img, x0, y0, w, h, accent):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1)
            t = (y - y0) / max(1, h - 1)
            color = shade(accent, 0.45) if edge else lerp(mix_white(accent, 0.2), shade(accent, 0.65), t)
            img.putpixel((x, y), color + (255,))
    if w > 3 and h > 3:
        img.putpixel((x0 + 1, y0 + 1), WHITE + (255,))


def fill_box(img, u, v, w, h, d, accent, rows=None):
    layout = [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h),
              (u + d + w, v + d, d, h), (u + d + w + d, v + d, w, h)]
    for i, (x, y, fw, fh) in enumerate(layout):
        if rows and i >= 2:
            y, fh = y + rows[0], rows[1]
        fill_face(img, x, y, fw, fh, accent)


def armor_layers(accent):
    layer1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill_box(layer1, 0, 0, 8, 8, 8, accent)              # helmet
    for x in range(10, 14):                               # face opening
        for y in range(12, 16):
            layer1.putpixel((x, y), (0, 0, 0, 0))
    fill_box(layer1, 16, 16, 8, 12, 4, accent)            # chestplate
    fill_box(layer1, 40, 16, 4, 12, 4, accent)            # arms
    fill_box(layer1, 0, 16, 4, 12, 4, accent, rows=(7, 5))  # boots
    layer2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill_box(layer2, 16, 16, 8, 12, 4, shade(accent, 0.85), rows=(7, 5))  # belt
    fill_box(layer2, 0, 16, 4, 12, 4, accent, rows=(0, 10))               # legs
    return layer1, layer2


# ---------------------------------------------------------------------------
# Everything
# ---------------------------------------------------------------------------

TOOL_DRAWERS = {"sword": sword, "pickaxe": pickaxe, "axe": axe, "shovel": shovel, "hoe": hoe}
ARMOR_MASKS = {"helmet": HELMET, "chestplate": CHESTPLATE, "leggings": LEGGINGS, "boots": BOOTS}
STAFF_ORBS = {
    "fire_staff": MATERIALS["fire"],
    "frost_staff": MATERIALS["frost"],
    "healing_staff": MATERIALS["heal"],
    "arcane_staff": MATERIALS["arcanite"],
    "lightning_staff": MATERIALS["lightning"],
}


def generate(assets: Path):
    item = assets / "textures" / "item"
    block = assets / "textures" / "block"
    armor = assets / "textures" / "models" / "armor"
    m = MATERIALS

    stone = base_texture(STONE, 1)
    deepslate = base_texture(DEEPSLATE, 2, streaks=True)
    save(ore(stone, m["mithril"]), block / "mithril_ore.png")
    save(ore(deepslate, m["mithril"]), block / "deepslate_mithril_ore.png")
    save(ore(stone, m["arcanite"]), block / "arcanite_ore.png")
    save(ore(deepslate, m["arcanite"]), block / "deepslate_arcanite_ore.png")
    save(ore(deepslate, m["adamantite"]), block / "adamantite_ore.png")
    save(metal_block(m["mithril"], 5), block / "mithril_block.png")
    save(metal_block(m["adamantite"], 6), block / "adamantite_block.png")
    save(raw_block(m["mithril"], 7), block / "raw_mithril_block.png")
    save(raw_block(m["adamantite"], 8), block / "raw_adamantite_block.png")
    save(crystal_block(m["arcanite"], 9), block / "arcanite_block.png")

    for seed, metal in enumerate(("mithril", "adamantite")):
        accent = m[metal]
        save(ingot(accent), item / f"{metal}_ingot.png")
        save(raw_chunk(accent, 40 + seed), item / f"raw_{metal}.png")
        for tool, draw in TOOL_DRAWERS.items():
            save(draw(accent), item / f"{metal}_{tool}.png")
    save(gem(m["arcanite"]), item / "arcanite.png")

    for metal in ("mithril", "arcanite", "adamantite"):
        layer1, layer2 = armor_layers(m[metal])
        save(layer1, armor / f"{metal}_layer_1.png")
        save(layer2, armor / f"{metal}_layer_2.png")
        for piece, mask in ARMOR_MASKS.items():
            save(mask_icon(mask, m[metal]), item / f"{metal}_{piece}.png")

    for name, orb in STAFF_ORBS.items():
        save(staff(orb), item / f"{name}.png")
    save(staff(m["arcanite"], shaft=(40, 20, 60)), item / "archmage_staff.png")

    save(sword(GOLD, core_dark=(30, 10, 45), guard=(60, 40, 10), grip=(40, 20, 60)), item / "kings_blade.png")
    save(hammer(m["adamantite"]), item / "adamantite_hammer.png")
    save(mask_icon(SKULL, m["bone"]), item / "cursed_skull.png")
    save(mask_icon(GRIMOIRE, m["dark"], {"a": m["arcanite"]}), item / "forbidden_grimoire.png")
    save(gem(m["adamantite"], inner=m["fire"]), item / "golem_core.png")
    generate_forging(assets)
    generate_ui(assets)


# ---------------------------------------------------------------------------
# Forged weapons (32x32), same style as the tools
# ---------------------------------------------------------------------------

WEAPON_COLORS = {
    "iron": (225, 228, 235),
    "steel": (150, 168, 185),
    "diamond": (70, 230, 225),
    "mithril": MATERIALS["mithril"],
    "orichalcum": (255, 150, 45),
    "adamantite": MATERIALS["adamantite"],
    "arcanium": (235, 80, 215),
    "starsteel": (150, 160, 255),
}
LEATHER = (90, 55, 30)


def wrapped_grip(c, start, end, accent):
    """Grip wrapped in alternating dark and colored bands."""
    for i, (t, x, y) in enumerate(segment(start, end)):
        band = shade(accent, 0.45) if (int(x) + int(y)) % 3 == 0 else HANDLE
        c.put(x, y, band)
        c.put(x + 1, y, shade(band, 0.7))


def dagger(accent):
    c = Canvas()
    wrapped_grip(c, (7, 25), (11, 21), accent)
    pommel_at(c, (6, 26), accent)
    for t, x, y in segment((13, 19), (23, 9)):
        c.put(x, y, lerp(DARK, shade(accent, 0.6), t))
        c.put(x, y + 1, lerp(shade(accent, 0.5), mix_white(accent, 0.2), t))
    c.put(24, 8, mix_white(accent, 0.6))
    for t, x, y in segment((10, 18), (14, 22)):
        c.put(x, y, (40, 40, 44))
    return c.image()


def katana(accent):
    c = Canvas()
    wrapped_grip(c, (3, 28), (9, 22), accent)
    pommel_at(c, (2, 29), accent)
    # Slightly curved, thin blade
    for t, x, y in bezier((12, 19), (22, 12), (30, 1), steps=60):
        c.put(x, y, lerp(DARK, shade(accent, 0.55), t ** 1.3))
        c.put(x + 1, y + 1, lerp(shade(accent, 0.5), mix_white(accent, 0.3), t))
    c.put(30, 1, mix_white(accent, 0.7))
    # Round tsuba
    for x, y in ((9, 19), (10, 19), (11, 20), (11, 21), (12, 22), (9, 20), (10, 21), (11, 22)):
        c.put(x, y, GOLD if (x + y) % 2 else shade(GOLD, 0.6))
    return c.image()


def longsword(accent):
    c = Canvas()
    wrapped_grip(c, (2, 29), (8, 23), accent)
    pommel_at(c, (1, 30), accent)
    # Wide blade with a dark fuller in the middle
    for t, x, y in segment((11, 20), (29, 2)):
        c.put(x - 1, y, lerp(shade(accent, 0.35), shade(accent, 0.7), t))
        c.put(x, y, lerp(DARK, shade(accent, 0.45), t ** 1.5))
        c.put(x, y + 1, lerp(shade(accent, 0.55), mix_white(accent, 0.25), t))
        c.put(x + 1, y + 1, lerp(shade(accent, 0.4), accent, t))
    c.put(30, 1, mix_white(accent, 0.7))
    # Big cross guard
    for t, x, y in segment((5, 17), (14, 26)):
        c.put(x, y, shade(accent, 0.5) if 0.3 < t < 0.7 else DARK)
        c.put(x + 1, y, (40, 40, 44))
    for x, y in ((5, 17), (14, 26)):
        c.put(x, y, accent)
    return c.image()


def spear(accent):
    c = Canvas()
    handle(c, (2, 29), (21, 10))
    # Leaf shaped head
    for t, x, y in segment((21, 10), (30, 1)):
        width = 1.6 * math.sin(math.pi * min(1.0, t * 1.15))
        c.brush(x, y, max(0.5, width), lerp(shade(accent, 0.5), mix_white(accent, 0.15), t))
    for t, x, y in segment((22, 9), (29, 2)):
        c.put(x, y, mix_white(accent, 0.45))
    for x, y in ((19, 11), (20, 12), (21, 11), (20, 10)):
        c.put(x, y, GOLD)
    return c.image()


def scythe(accent):
    c = Canvas()
    handle(c, (6, 30), (20, 3))
    # Large curved blade sweeping to the left from the top of the shaft
    c.stroke(bezier((21, 4), (10, -2), (1, 11), steps=70),
             lambda t: 0.5 + 1.6 * math.sin(math.pi * min(1.0, 0.25 + t * 0.75)),
             lambda t: lerp(DARK, accent, t * 1.2))
    for t, x, y in bezier((19, 2), (9, -3), (1, 10), steps=70):
        if t > 0.2:
            c.put(x, y, lerp(shade(accent, 0.7), mix_white(accent, 0.3), t))
    pommel_at(c, (5, 31), accent)
    return c.image()


def pommel_at(c, pos, accent):
    x, y = pos
    c.put(x, y, accent)
    c.put(x + 1, y - 1, mix_white(accent, 0.4))


WEAPON_DRAWERS = {"dagger": dagger, "katana": katana, "longsword": longsword, "spear": spear, "scythe": scythe}


def template_icon(weapon_img):
    """Parchment blueprint with the weapon drawn in ink."""
    c = Canvas()
    for y in range(3, 29):
        for x in range(4, 28):
            edge = x in (4, 27) or y in (3, 28)
            c.put(x, y, (170, 140, 95) if edge else lerp((240, 225, 185), (215, 190, 140), (x + y - 7) / 48))
    img = c.image()
    ink = weapon_img.resize((22, 22), Image.NEAREST)
    for y in range(22):
        for x in range(22):
            if ink.getpixel((x, y))[3] > 0:
                img.putpixel((x + 5, y + 5), (40, 60, 120, 255))
    return img


def grip_icon():
    c = Canvas()
    for i, (t, x, y) in enumerate(segment((8, 24), (23, 9))):
        band = LEATHER if (int(x) + int(y)) % 3 else shade(LEATHER, 0.6)
        c.brush(x, y, 1.3, band)
    c.brush(7, 25, 1.6, GOLD)
    c.brush(24, 8, 1.6, GOLD)
    return c.image()


def shaft_icon():
    c = Canvas()
    for t, x, y in segment((3, 29), (29, 3)):
        c.put(x, y, lerp((120, 85, 50), (165, 120, 70), t))
        c.put(x + 1, y, (90, 60, 35))
    for t in (0.33, 0.66):
        x, y = 3 + 26 * t, 29 - 26 * t
        c.brush(x, y, 1.2, (150, 150, 155))
    return c.image()


def catalyst_icon():
    c = Canvas()
    cx, cy, r = 16.0, 16.0, 9.0
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                swirl = 0.5 + 0.5 * math.sin(math.atan2(y - cy, x - cx) * 3 + d * 0.6)
                col = lerp(MATERIALS["arcanite"], MATERIALS["fire"], swirl)
                glow = 1 - math.hypot(x - (cx - 3), y - (cy - 3)) / (r * 1.8)
                c.put(x, y, mix_white(shade(col, 0.55 + 0.6 * glow), max(0.0, glow - 0.5)))
    c.put(12, 11, WHITE)
    c.put(13, 11, WHITE)
    c.put(12, 12, mix_white(MATERIALS["arcanite"], 0.7))
    return c.image(outline=shade(MATERIALS["arcanite"], 0.2))


# ---------------------------------------------------------------------------
# Workstation blocks (16x16)
# ---------------------------------------------------------------------------

BRICK = (48, 44, 56)
TRIM = (200, 160, 60)


def dark_bricks(seed):
    noise = smooth_noise(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            mortar = y % 4 == 3 or (x + (4 if (y // 4) % 2 else 0)) % 8 == 7
            f = 0.55 if mortar else 0.9 + 0.25 * noise[y][x]
            img.putpixel((x, y), shade(BRICK, f) + (255,))
    return img


def forge_front(lit):
    img = dark_bricks(60)
    for y in range(6, 14):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (6, 13)
            if edge:
                color = TRIM
            elif lit:
                t = (y - 7) / 6
                color = lerp(mix_white(MATERIALS["fire"], 0.35), MATERIALS["arcanite"], t)
            else:
                color = (20, 18, 24)
            img.putpixel((x, y), color + (255,))
    for x in range(2, 14):
        img.putpixel((x, 2), TRIM + (255,))
    return img


def forge_side():
    img = dark_bricks(61)
    for x in range(16):
        img.putpixel((x, 0), shade(TRIM, 0.8) + (255,))
        img.putpixel((x, 15), shade(TRIM, 0.6) + (255,))
    return img


def forge_top():
    img = dark_bricks(62)
    for y in range(4, 12):
        for x in range(4, 12):
            ring = min(x - 4, y - 4, 11 - x, 11 - y)
            color = TRIM if ring == 0 else lerp((255, 120, 30), (120, 30, 160), math.hypot(x - 7.5, y - 7.5) / 4)
            img.putpixel((x, y), color + (255,))
    return img


def weapon_forge_top():
    noise = smooth_noise(70)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            f = 0.5 if edge else 0.85 + 0.25 * noise[y][x] - 0.01 * (x + y)
            img.putpixel((x, y), shade((150, 155, 165), f) + (255,))
    for t, x, y in segment((3, 12), (12, 3)):
        img.putpixel((int(x), int(y)), (70, 70, 80, 255))
    return img


def weapon_forge_side():
    noise = smooth_noise(71)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 3 or y > 12:
                color = shade((140, 145, 155), 0.75 + 0.2 * noise[y][x])
            else:
                plank = (y - 3) // 5
                color = shade((140, 95, 55), 0.75 + 0.25 * noise[y][x] - 0.1 * plank)
                if (y - 3) % 5 == 4:
                    color = shade(color, 0.6)
            img.putpixel((x, y), color + (255,))
    for y in (1, 14):
        for x in (2, 13):
            img.putpixel((x, y), (60, 60, 70, 255))
    return img


def weapon_forge_bottom():
    noise = smooth_noise(72)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade((120, 82, 48), 0.75 + 0.25 * noise[y][x]) + (255,))
    return img


def generate_forging(assets: Path):
    item = assets / "textures" / "item"
    block = assets / "textures" / "block"

    for alloy in ("steel", "orichalcum", "arcanium", "starsteel"):
        save(ingot(WEAPON_COLORS[alloy]), item / f"{alloy}_ingot.png")
    for material, color in WEAPON_COLORS.items():
        for weapon, draw in WEAPON_DRAWERS.items():
            save(draw(color), item / f"{material}_{weapon}.png")
    for weapon, draw in WEAPON_DRAWERS.items():
        save(template_icon(draw(WEAPON_COLORS["iron"])), item / f"{weapon}_template.png")
    save(grip_icon(), item / "weapon_grip.png")
    save(shaft_icon(), item / "weapon_shaft.png")
    save(catalyst_icon(), item / "fusion_catalyst.png")

    save(forge_front(False), block / "fusion_forge_front.png")
    save(forge_front(True), block / "fusion_forge_front_on.png")
    save(forge_side(), block / "fusion_forge_side.png")
    save(forge_top(), block / "fusion_forge_top.png")
    save(weapon_forge_top(), block / "weapon_forge_top.png")
    save(weapon_forge_side(), block / "weapon_forge_side.png")
    save(weapon_forge_bottom(), block / "weapon_forge_bottom.png")



# ---------------------------------------------------------------------------
# Dark fantasy UI kit (256x256 atlas): dark charcoal panels, thin gold trims
# ---------------------------------------------------------------------------

UI_GOLD = (201, 162, 90)
UI_GOLD_LIGHT = (240, 210, 140)
UI_GOLD_DARK = (110, 82, 40)
UI_BG = (22, 20, 18)


def ui_atlas():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    rng = random.Random(90)
    noise = smooth_noise(91, size=64, cells=8)

    # (0,0) 64x64 panel background tile: charcoal with soft noise and faint scratches
    for y in range(64):
        for x in range(64):
            f = 0.8 + 0.4 * noise[y][x]
            img.putpixel((x, y), shade(UI_BG, f) + (238,))
    for _ in range(14):
        x, y = rng.randrange(64), rng.randrange(64)
        for i in range(rng.randrange(4, 10)):
            px, py = (x + i) % 64, (y + i // 2) % 64
            img.putpixel((px, py), shade(UI_BG, 1.35) + (238,))

    # (64,0) 18x18 slot and (82,0) highlighted slot: engraved dark stone, thin border
    slot_noise = smooth_noise(92, size=18, cells=3)
    for ox, border in ((64, shade(UI_GOLD, 0.45)), (82, UI_GOLD_LIGHT)):
        for y in range(18):
            for x in range(18):
                if x in (0, 17) or y in (0, 17):
                    color = border
                elif x == 1 or y == 1:
                    color = (8, 8, 8)
                else:
                    color = shade((34, 31, 28), 0.85 + 0.3 * slot_noise[y][x])
                img.putpixel((ox + x, y), color + (255,))
        for i in range(3, 9):  # diagonal scratch like engraved stone
            img.putpixel((ox + i + 4, 12 - i // 2), shade((34, 31, 28), 1.3) + (255,))

    # (100,0) four 9x9 corner flourishes: TL, TR, BL, BR
    corner = [
        "ggggg....",
        "gLLLgg...",
        "gL..gLg..",
        "gL...gg..",
        "gggg..g..",
        ".g.g.....",
        ".gg......",
        "..g......",
        ".........",
    ]
    def put_corner(ox, oy, flip_x, flip_y):
        for y, row in enumerate(corner):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                tx = 8 - x if flip_x else x
                ty = 8 - y if flip_y else y
                img.putpixel((ox + tx, oy + ty), (UI_GOLD_LIGHT if ch == "L" else UI_GOLD) + (255,))
    put_corner(100, 0, False, False)
    put_corner(109, 0, True, False)
    put_corner(100, 9, False, True)
    put_corner(109, 9, True, True)

    # (118,0) 7x7 diamond ornament
    for y in range(7):
        for x in range(7):
            d = abs(x - 3) + abs(y - 3)
            if d <= 3:
                img.putpixel((118 + x, y), (UI_GOLD_LIGHT if d <= 1 else UI_GOLD) + (255,))

    # (128,0) 24x15 crafting arrow (gold)
    for y in range(15):
        for x in range(24):
            shaft = x < 15 and 5 <= y <= 9
            head = x >= 15 and abs(y - 7) <= (23 - x)
            if shaft or head:
                img.putpixel((128 + x, y), lerp(UI_GOLD_LIGHT, UI_GOLD_DARK, y / 14) + (255,))

    # (0,64) 40x40 medallion ring with thorny gold rim
    cx = cy = 19.5
    for y in range(40):
        for x in range(40):
            d = math.hypot(x - cx, y - cy)
            angle = math.atan2(y - cy, x - cx)
            spike = 0.8 * max(0.0, math.cos(angle * 12)) ** 6
            if 15.5 <= d <= 18.0 + spike:
                t = (d - 15.5) / 3
                img.putpixel((x, 64 + y), lerp(UI_GOLD_LIGHT, UI_GOLD_DARK, t) + (255,))
            elif 14.5 <= d < 15.5:
                img.putpixel((x, 64 + y), (10, 9, 8, 255))
            elif d < 14.5:
                img.putpixel((x, 64 + y), shade(UI_BG, 0.7 + 0.2 * (d / 14.5)) + (255,))

    # (0,104) selected tab 96x24: gold glow fading to the right, gold frame
    for y in range(24):
        for x in range(96):
            edge = y in (0, 23) or x == 0
            glow = max(0.0, 1 - x / 96) * (1 - abs(y - 11.5) / 12)
            if edge:
                color = UI_GOLD if x < 90 else UI_GOLD_DARK
            else:
                color = lerp(UI_BG, (120, 90, 40), glow * 0.8)
            img.putpixel((x, 104 + y), color + (235,))
    return img


def generate_ui(assets: Path):
    save(ui_atlas(), assets / "textures" / "gui" / "arcane_ui.png")
