#!/usr/bin/env python3
"""Generates Arcane Quest textures, models, blockstates, recipes, loot tables, tags and worldgen.

Run from the repository root:  python3 tools/generate_resources.py
All textures are original pixel art drawn by this script (no Mojang assets are copied).
Translations (lang files) are written by hand and are not touched.
"""
import json
import random
from pathlib import Path

from PIL import Image

MODID = "arcanequest"
ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / MODID
DATA = ROOT / "data"

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------

MATERIALS = {
    "mithril": (140, 200, 235),
    "arcanite": (170, 90, 235),
    "adamantite": (200, 40, 55),
    "gold": (240, 200, 60),
    "bone": (225, 220, 200),
    "fire": (255, 130, 30),
    "frost": (120, 230, 255),
    "heal": (255, 120, 170),
    "lightning": (255, 240, 90),
    "dark": (90, 40, 140),
    "steel": (90, 90, 100),
}
WOOD = (130, 90, 50)
WOOD_DARK = (80, 52, 28)
OUTLINE = (25, 20, 30)
PAGE = (235, 230, 215)


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def shades(color):
    """Shades 1 (dark) to 5 (light)."""
    return {str(i): shade(color, f) for i, f in zip(range(1, 6), (0.45, 0.65, 0.82, 1.0, 1.2))}


def palette(primary, secondary=None):
    pal = {"k": OUTLINE, "s": WOOD, "S": WOOD_DARK, "w": PAGE, "g": MATERIALS["gold"]}
    pal.update(shades(primary))
    if secondary:
        for key, value in shades(secondary).items():
            pal["abcde"[int(key) - 1]] = value
    return pal


def from_ascii(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows[:16]):
        for x, ch in enumerate(row.ljust(16, ".")[:16]):
            if ch != ".":
                img.putpixel((x, y), pal[ch] + (255,))
    return img


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


# ---------------------------------------------------------------------------
# Item icons drawn from ASCII art (1..5 = primary shades, a..e = secondary shades)
# ---------------------------------------------------------------------------

INGOT = [
    "", "", "", "", "",
    ".......kkkkkk",
    ".....kk555554k",
    "...kk55444443k",
    "..k5544444433k",
    "..k4444444332k",
    "..k3333333322k",
    "..k22222222kk",
    "...kkkkkkkk",
]
RAW = [
    "", "", "",
    "......kkk",
    "....kk544k",
    "...k5544433k",
    "...k54443332k",
    "..k544433322k",
    "..k443333222k",
    "..k33332221k",
    "...k322211k",
    "....kk111kk",
    "......kkk",
]
GEM = [
    "", "",
    ".......kk",
    "......k55k",
    ".....k5544k",
    "....k554443k",
    "...k55444332k",
    "...k54443322k",
    "...k44433221k",
    "....k433221k",
    ".....k3221k",
    "......k21k",
    ".......kk",
]
HELMET = [
    "", "", "",
    "....kkkkkkkk",
    "...k55555544k",
    "...k54444433k",
    "...k43kkkk32k",
    "...k3k....k2k",
    "...kk......kk",
]
CHESTPLATE = [
    "",
    "..kkkk....kkkk",
    ".k5554kkkk4443k",
    ".k5444444444433k",
    ".kk44444444433kk",
    "..k44444444433k",
    "...k444444433k",
    "...k444444333k",
    "...k443333332k",
    "...k433333322k",
    "...k333333222k",
    "...k332222221k",
    "...kkkkkkkkkkk",
]
LEGGINGS = [
    "", "",
    "...kkkkkkkkkk",
    "...k55544443k",
    "...k54444333k",
    "...k443kk332k",
    "...k43k..k32k",
    "...k43k..k32k",
    "...k43k..k32k",
    "...k32k..k21k",
    "...k32k..k21k",
    "...kkkk..kkkk",
]
BOOTS = [
    "", "", "", "", "",
    "...kkkk..kkkk",
    "...k54k..k43k",
    "...k43k..k32k",
    "...k43k..k32k",
    "..k443k..k332k",
    ".k4433k..k3322k",
    ".kkkkkk..kkkkkk",
]
SKULL = [
    "",
    "....kkkkkkk",
    "...k5555554k",
    "..k555555444k",
    "..k554444443k",
    "..k4kkk4kkk3k",
    "..k4kak4kak3k",
    "..k4kkk4kkk3k",
    "..k444kkk4432k",
    "...k43k3k32k",
    "...k4k3k3k2k",
    "...kkkkkkkkk",
    "....ggggggg",
    "...gkgkgkgkg",
]
GRIMOIRE = [
    "",
    "..kkkkkkkkkkk",
    "..k5555555554k",
    "..k5gg5555554k",
    "..k5gg55dd554k",
    "..k5555deed54k",
    "..k5555deed54k",
    "..k55555dd554k",
    "..k5555555554k",
    "..k5555555554k",
    "..kwwwwwwwwwkk",
    "..kkkkkkkkkkk",
]


def ascii_icon(name, rows, primary, secondary=None):
    save(from_ascii(rows, palette(primary, secondary)), ASSETS / "textures" / "item" / f"{name}.png")


# ---------------------------------------------------------------------------
# Tools and staffs drawn from shapes, then auto-outlined and shaded
# ---------------------------------------------------------------------------

def line(x0, y0, x1, y1):
    points = []
    steps = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(steps + 1):
        t = i / steps if steps else 0
        points.append((round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t)))
    return points


def thick(points):
    out = set(points)
    for x, y in points:
        out.add((x + 1, y))
    return out


def polygon(vertices):
    xs = [v[0] for v in vertices]
    ys = [v[1] for v in vertices]
    out = set()
    for y in range(min(ys), max(ys) + 1):
        for x in range(min(xs), max(xs) + 1):
            if inside(x + 0.5, y + 0.5, vertices):
                out.add((x, y))
    return out


def inside(px, py, vertices):
    result = False
    n = len(vertices)
    for i in range(n):
        x0, y0 = vertices[i]
        x1, y1 = vertices[(i + 1) % n]
        if (y0 > py) != (y1 > py) and px < (x1 - x0) * (py - y0) / (y1 - y0) + x0:
            result = not result
    return result


def disc(cx, cy, r):
    return {(x, y) for x in range(16) for y in range(16) if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 0.5}


def render_layers(layers):
    """layers: list of (pixel set, base color, glossy). Later layers draw on top."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    filled = set()
    for pixels, color, glossy in layers:
        for x, y in pixels:
            if 0 <= x < 16 and 0 <= y < 16:
                # light comes from the top-left
                factor = 1.15 - 0.03 * ((y - x) + 8) if glossy else 1.0 - 0.02 * y
                img.putpixel((x, y), shade(color, max(0.55, factor)) + (255,))
                filled.add((x, y))
    for x in range(16):
        for y in range(16):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), OUTLINE + (255,))
    return img


HANDLE_SHORT = line(1, 14, 7, 8)
HANDLE_LONG = line(1, 14, 10, 5)


def tool_icon(name, kind, color):
    handle = (set(HANDLE_SHORT if kind in ("sword",) else HANDLE_LONG), WOOD, False)
    if kind == "sword":
        blade = (thick(line(5, 9, 13, 1)), color, True)
        guard = (set(line(3, 8, 7, 12)), shade(color, 0.7), True)
        layers = [handle, blade, guard]
    elif kind == "pickaxe":
        head = thick(line(3, 3, 8, 1)) | thick(line(8, 1, 12, 4)) | set(line(12, 4, 13, 9))
        layers = [handle, (head, color, True)]
    elif kind == "axe":
        head = polygon([(8, 1), (11, 0), (15, 3), (15, 7), (12, 8), (8, 4)])
        layers = [handle, (head, color, True)]
    elif kind == "shovel":
        head = polygon([(8, 5), (9, 2), (12, 0), (15, 1), (14, 4), (11, 7)])
        layers = [(set(line(1, 14, 9, 6)), WOOD, False), (head, color, True)]
    elif kind == "hoe":
        head = thick(line(7, 2, 12, 2)) | set(line(12, 3, 12, 5))
        layers = [handle, (head, color, True)]
    elif kind == "hammer":
        head = polygon([(6, 4), (11, -1), (16, 4), (11, 9)])
        layers = [handle, (head, color, True), (set(line(9, 2, 13, 6)), MATERIALS["gold"], True)]
    else:
        raise ValueError(kind)
    save(render_layers(layers), ASSETS / "textures" / "item" / f"{name}.png")


def staff_icon(name, orb_color, shaft=WOOD, trim=MATERIALS["gold"]):
    layers = [
        (set(line(1, 14, 10, 5)), shaft, False),
        (set(line(9, 4, 11, 6)), trim, True),
        (disc(12.5, 2.5, 2.3), orb_color, True),
    ]
    img = render_layers(layers)
    img.putpixel((12, 2), (255, 255, 255, 255))  # sparkle
    save(img, ASSETS / "textures" / "item" / f"{name}.png")


def blade_icon(name):
    layers = [
        (set(HANDLE_SHORT), shade(MATERIALS["dark"], 0.6), False),
        (thick(line(5, 9, 13, 1)), (40, 40, 48), True),
        (set(line(6, 8, 13, 1)), (160, 160, 175), True),
        (set(line(3, 8, 7, 12)), MATERIALS["gold"], True),
    ]
    save(render_layers(layers), ASSETS / "textures" / "item" / f"{name}.png")


# ---------------------------------------------------------------------------
# Block textures
# ---------------------------------------------------------------------------

def noise_base(seed, base, variation, streaks=False):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        row_shift = rng.uniform(-0.06, 0.06) if streaks else 0
        for x in range(16):
            f = 1.0 + rng.uniform(-variation, variation) + row_shift
            img.putpixel((x, y), shade(base, f) + (255,))
    return img


STONE = (125, 125, 125)
DEEPSLATE = (75, 75, 82)


def ore_texture(name, base_img, color, seed):
    img = base_img.copy()
    rng = random.Random(seed)
    clusters = [(3, 3), (10, 4), (5, 10), (11, 11), (8, 7)]
    for cx, cy in clusters:
        cells = {(cx, cy), (cx + 1, cy), (cx, cy + 1)}
        if rng.random() < 0.7:
            cells.add((cx + 1, cy + 1))
        if rng.random() < 0.5:
            cells.add((cx - 1, cy))
        for x, y in cells:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if (nx, ny) not in cells and 0 <= nx < 16 and 0 <= ny < 16:
                    img.putpixel((nx, ny), shade(img.getpixel((nx, ny))[:3], 0.7) + (255,))
        for x, y in cells:
            light = 1.25 if (x, y) == (cx, cy) else rng.uniform(0.75, 1.05)
            img.putpixel((x, y), shade(color, light) + (255,))
    save(img, ASSETS / "textures" / "block" / f"{name}.png")


def metal_block(name, color):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = shade(color, 0.55)
            elif x == 1 or y == 1:
                c = shade(color, 1.25)
            elif x == 14 or y == 14:
                c = shade(color, 0.75)
            else:
                c = shade(color, 1.0 - 0.012 * (x + y))
            img.putpixel((x, y), c + (255,))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), shade(color, 0.6) + (255,))
    save(img, ASSETS / "textures" / "block" / f"{name}.png")


def raw_block(name, color, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade(color, rng.choice((0.55, 0.7, 0.85, 1.0, 1.1))) + (255,))
    save(img, ASSETS / "textures" / "block" / f"{name}.png")


def crystal_block(name, color):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            facet = ((x // 4) + (y // 4)) % 3
            f = (0.75, 0.95, 1.2)[facet] - 0.01 * y
            if x % 4 == 0 or y % 4 == 0:
                f *= 0.8
            img.putpixel((x, y), shade(color, f) + (255,))
    save(img, ASSETS / "textures" / "block" / f"{name}.png")


# ---------------------------------------------------------------------------
# Armor model textures (64x32, humanoid UV layout)
# ---------------------------------------------------------------------------

def fill_face(img, x0, y0, w, h, color, rng):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1)
            f = 0.6 if edge else rng.uniform(0.9, 1.08) - 0.015 * (y - y0)
            img.putpixel((x, y), shade(color, f) + (255,))


def fill_box(img, u, v, w, h, d, color, rng, faces=("top", "bottom", "right", "front", "left", "back"), rows=None):
    """Fills the faces of a box unwrapped Minecraft style at (u, v) with size w*h*d."""
    layout = {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }
    for face in faces:
        x, y, fw, fh = layout[face]
        if rows and face not in ("top", "bottom"):
            y, fh = y + rows[0], rows[1]
        fill_face(img, x, y, fw, fh, color, rng)


def armor_textures(name, color, seed):
    rng = random.Random(seed)
    layer1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill_box(layer1, 0, 0, 8, 8, 8, color, rng)                     # helmet
    for x in range(10, 14):                                          # face opening
        for y in range(12, 16):
            layer1.putpixel((x, y), (0, 0, 0, 0))
    fill_box(layer1, 16, 16, 8, 12, 4, color, rng)                  # chestplate
    fill_box(layer1, 40, 16, 4, 12, 4, color, rng)                  # arms
    fill_box(layer1, 0, 16, 4, 12, 4, color, rng, rows=(7, 5))      # boots
    save(layer1, ASSETS / "textures" / "models" / "armor" / f"{name}_layer_1.png")

    layer2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill_box(layer2, 16, 16, 8, 12, 4, shade(color, 0.85), rng, rows=(7, 5))  # belt
    fill_box(layer2, 0, 16, 4, 12, 4, color, rng, rows=(0, 10))              # legs
    save(layer2, ASSETS / "textures" / "models" / "armor" / f"{name}_layer_2.png")


# ---------------------------------------------------------------------------
# JSON helpers
# ---------------------------------------------------------------------------

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def rl(name):
    return name if ":" in name else f"{MODID}:{name}"


def item_model(name, parent="item/generated"):
    write_json(ASSETS / "models" / "item" / f"{name}.json",
               {"parent": f"minecraft:{parent}", "textures": {"layer0": f"{MODID}:item/{name}"}})


def block_assets(name):
    write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    write_json(ASSETS / "models" / "block" / f"{name}.json",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{name}"}})
    write_json(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"{MODID}:block/{name}"})


def loot_self(name):
    write_json(DATA / MODID / "loot_table" / "blocks" / f"{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                   "entries": [{"type": "minecraft:item", "name": rl(name)}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/{name}",
    })


def loot_ore(name, drop, count=None):
    functions = []
    if count:
        functions.append({"function": "minecraft:set_count", "add": False,
                          "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}})
    functions += [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                  {"function": "minecraft:explosion_decay"}]
    silk = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    write_json(DATA / MODID / "loot_table" / "blocks" / f"{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": rl(name), "conditions": [silk]},
            {"type": "minecraft:item", "name": rl(drop), "functions": functions},
        ]}]}],
        "random_sequence": f"{MODID}:blocks/{name}",
    })


def loot_entity(name, entries):
    pools = []
    for item, low, high in entries:
        entry = {"type": "minecraft:item", "name": rl(item)}
        if (low, high) != (1, 1):
            entry["functions"] = [{"function": "minecraft:set_count",
                                   "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
        pools.append({"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry]})
    write_json(DATA / MODID / "loot_table" / "entities" / f"{name}.json",
               {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{MODID}:entities/{name}"})


def ingredient(value):
    return {"tag": value[1:]} if value.startswith("#") else {"item": rl(value)}


def shaped(name, result, pattern, key, count=1, category="equipment"):
    write_json(DATA / MODID / "recipe" / f"{name}.json", {
        "type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
        "key": {k: ingredient(v) for k, v in key.items()},
        "result": {"id": rl(result), "count": count},
    })


def shapeless(name, result, ingredients, count=1):
    write_json(DATA / MODID / "recipe" / f"{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [ingredient(i) for i in ingredients],
        "result": {"id": rl(result), "count": count},
    })


def smelt(name, result, inputs, xp):
    for kind, time in (("smelting", 200), ("blasting", 100)):
        write_json(DATA / MODID / "recipe" / f"{name}_from_{kind}.json", {
            "type": f"minecraft:{kind}", "category": "misc",
            "ingredient": [ingredient(i) for i in inputs],
            "result": {"id": rl(result)}, "experience": xp, "cookingtime": time,
        })


def tag(registry, name, values, namespace="minecraft"):
    write_json(DATA / namespace / "tags" / registry / f"{name}.json",
               {"replace": False, "values": [rl(v) for v in values]})


def ore_feature(name, size, count, min_y, max_y, stone_ore, deepslate_ore, shape="trapezoid", air_discard=0.0):
    targets = []
    if stone_ore:
        targets.append({"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                        "state": {"Name": rl(stone_ore)}})
    targets.append({"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                    "state": {"Name": rl(deepslate_ore)}})
    write_json(DATA / MODID / "worldgen" / "configured_feature" / f"{name}.json", {
        "type": "minecraft:ore",
        "config": {"size": size, "discard_chance_on_air_exposure": air_discard, "targets": targets},
    })
    write_json(DATA / MODID / "worldgen" / "placed_feature" / f"{name}.json", {
        "feature": rl(name),
        "placement": [
            {"type": "minecraft:count", "count": count},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": f"minecraft:{shape}",
                                                           "min_inclusive": {"absolute": min_y},
                                                           "max_inclusive": {"absolute": max_y}}},
            {"type": "minecraft:biome"},
        ],
    })
    write_json(DATA / MODID / "forge" / "biome_modifier" / f"add_{name}.json", {
        "type": "forge:add_features", "biomes": "#minecraft:is_overworld",
        "features": rl(name), "step": "underground_ores",
    })


# ---------------------------------------------------------------------------
# Content
# ---------------------------------------------------------------------------

METALS = {"mithril": MATERIALS["mithril"], "adamantite": MATERIALS["adamantite"]}
TOOLS = ("sword", "pickaxe", "axe", "shovel", "hoe")
ARMOR = {"helmet": HELMET, "chestplate": CHESTPLATE, "leggings": LEGGINGS, "boots": BOOTS}
STAFFS = {
    "fire_staff": MATERIALS["fire"],
    "frost_staff": MATERIALS["frost"],
    "healing_staff": MATERIALS["heal"],
    "arcane_staff": MATERIALS["arcanite"],
    "lightning_staff": MATERIALS["lightning"],
}


def generate_textures_and_models():
    stone = noise_base(1, STONE, 0.08)
    deepslate = noise_base(2, DEEPSLATE, 0.07, streaks=True)

    ore_texture("mithril_ore", stone, MATERIALS["mithril"], 10)
    ore_texture("deepslate_mithril_ore", deepslate, MATERIALS["mithril"], 11)
    ore_texture("arcanite_ore", stone, MATERIALS["arcanite"], 12)
    ore_texture("deepslate_arcanite_ore", deepslate, MATERIALS["arcanite"], 13)
    ore_texture("adamantite_ore", deepslate, MATERIALS["adamantite"], 14)
    metal_block("mithril_block", MATERIALS["mithril"])
    metal_block("adamantite_block", MATERIALS["adamantite"])
    raw_block("raw_mithril_block", MATERIALS["mithril"], 20)
    raw_block("raw_adamantite_block", MATERIALS["adamantite"], 21)
    crystal_block("arcanite_block", MATERIALS["arcanite"])
    for block in ("mithril_ore", "deepslate_mithril_ore", "mithril_block", "raw_mithril_block", "arcanite_ore",
                  "deepslate_arcanite_ore", "arcanite_block", "adamantite_ore", "adamantite_block", "raw_adamantite_block"):
        block_assets(block)

    for metal, color in METALS.items():
        ascii_icon(f"raw_{metal}", RAW, color)
        ascii_icon(f"{metal}_ingot", INGOT, color)
        item_model(f"raw_{metal}")
        item_model(f"{metal}_ingot")
        for tool in TOOLS:
            tool_icon(f"{metal}_{tool}", tool, color)
            item_model(f"{metal}_{tool}", "item/handheld")
    ascii_icon("arcanite", GEM, MATERIALS["arcanite"])
    item_model("arcanite")

    for seed, metal in enumerate(("mithril", "arcanite", "adamantite")):
        color = MATERIALS[metal]
        armor_textures(metal, color, 30 + seed)
        for piece, rows in ARMOR.items():
            ascii_icon(f"{metal}_{piece}", rows, color)
            item_model(f"{metal}_{piece}")

    for staff, orb in STAFFS.items():
        staff_icon(staff, orb)
        item_model(staff, "item/handheld")
    staff_icon("archmage_staff", MATERIALS["arcanite"], shaft=(45, 25, 60))
    item_model("archmage_staff", "item/handheld")

    ascii_icon("cursed_skull", SKULL, MATERIALS["bone"], MATERIALS["fire"])
    ascii_icon("forbidden_grimoire", GRIMOIRE, MATERIALS["dark"], MATERIALS["arcanite"])
    ascii_icon("golem_core", GEM, MATERIALS["adamantite"])
    for item in ("cursed_skull", "forbidden_grimoire", "golem_core"):
        item_model(item)

    blade_icon("kings_blade")
    item_model("kings_blade", "item/handheld")
    tool_icon("adamantite_hammer", "hammer", MATERIALS["adamantite"])
    item_model("adamantite_hammer", "item/handheld")

    for egg in ("skeleton_king", "corrupted_archmage", "adamantite_golem"):
        write_json(ASSETS / "models" / "item" / f"{egg}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def generate_loot():
    for block in ("mithril_block", "raw_mithril_block", "arcanite_block", "adamantite_block", "raw_adamantite_block"):
        loot_self(block)
    loot_ore("mithril_ore", "raw_mithril")
    loot_ore("deepslate_mithril_ore", "raw_mithril")
    loot_ore("arcanite_ore", "arcanite", (1, 2))
    loot_ore("deepslate_arcanite_ore", "arcanite", (1, 2))
    loot_ore("adamantite_ore", "raw_adamantite")

    loot_entity("skeleton_king", [("kings_blade", 1, 1), ("mithril_ingot", 4, 8), ("arcanite", 2, 4),
                                  ("minecraft:bone", 5, 10), ("minecraft:wither_skeleton_skull", 1, 1)])
    loot_entity("corrupted_archmage", [("archmage_staff", 1, 1), ("arcanite", 6, 10),
                                       ("minecraft:totem_of_undying", 1, 1), ("minecraft:emerald", 4, 10)])
    loot_entity("adamantite_golem", [("adamantite_hammer", 1, 1), ("adamantite_ingot", 4, 8),
                                     ("raw_adamantite", 2, 5), ("minecraft:iron_block", 2, 4)])


def generate_recipes():
    for metal, ingot, raw in (("mithril", "mithril_ingot", "raw_mithril"), ("adamantite", "adamantite_ingot", "raw_adamantite")):
        shaped(f"{metal}_block", f"{metal}_block", ["###", "###", "###"], {"#": ingot}, category="building")
        shapeless(f"{ingot}_from_block", ingot, [f"{metal}_block"], 9)
        shaped(f"raw_{metal}_block", f"raw_{metal}_block", ["###", "###", "###"], {"#": raw}, category="building")
        shapeless(f"raw_{metal}_from_block", raw, [f"raw_{metal}_block"], 9)
        ores = [raw] + ([f"{metal}_ore", f"deepslate_{metal}_ore"] if metal == "mithril" else [f"{metal}_ore"])
        smelt(ingot, ingot, ores, 1.0 if metal == "mithril" else 2.0)

        stick = "#c:rods/wooden"
        shaped(f"{metal}_sword", f"{metal}_sword", ["X", "X", "#"], {"X": ingot, "#": stick})
        shaped(f"{metal}_pickaxe", f"{metal}_pickaxe", ["XXX", " # ", " # "], {"X": ingot, "#": stick})
        shaped(f"{metal}_axe", f"{metal}_axe", ["XX", "X#", " #"], {"X": ingot, "#": stick})
        shaped(f"{metal}_shovel", f"{metal}_shovel", ["X", "#", "#"], {"X": ingot, "#": stick})
        shaped(f"{metal}_hoe", f"{metal}_hoe", ["XX", " #", " #"], {"X": ingot, "#": stick})

    for metal, material in (("mithril", "mithril_ingot"), ("arcanite", "arcanite"), ("adamantite", "adamantite_ingot")):
        shaped(f"{metal}_helmet", f"{metal}_helmet", ["XXX", "X X"], {"X": material})
        shaped(f"{metal}_chestplate", f"{metal}_chestplate", ["X X", "XXX", "XXX"], {"X": material})
        shaped(f"{metal}_leggings", f"{metal}_leggings", ["XXX", "X X", "X X"], {"X": material})
        shaped(f"{metal}_boots", f"{metal}_boots", ["X X", "X X"], {"X": material})

    shaped("arcanite_block", "arcanite_block", ["###", "###", "###"], {"#": "arcanite"}, category="building")
    shapeless("arcanite_from_block", "arcanite", ["arcanite_block"], 9)
    smelt("arcanite", "arcanite", ["arcanite_ore", "deepslate_arcanite_ore"], 1.0)

    staff_cores = {
        "fire_staff": "minecraft:blaze_powder",
        "frost_staff": "minecraft:packed_ice",
        "healing_staff": "minecraft:glistering_melon_slice",
        "arcane_staff": "minecraft:amethyst_shard",
        "lightning_staff": "mithril_ingot",
    }
    for staff, core in staff_cores.items():
        shaped(staff, staff, [" BC", " SB", "S  "], {"C": "arcanite", "B": core, "S": "#c:rods/wooden"})

    shaped("cursed_skull", "cursed_skull", ["BMB", "MCM", "BMB"],
           {"B": "minecraft:bone_block", "M": "mithril_ingot", "C": "arcanite"}, category="misc")
    shaped("forbidden_grimoire", "forbidden_grimoire", ["EAE", "ABA", "EAE"],
           {"E": "minecraft:ender_pearl", "A": "arcanite", "B": "minecraft:book"}, category="misc")
    shaped("golem_core", "golem_core", ["AIA", "IMI", "AIA"],
           {"A": "adamantite_ingot", "I": "minecraft:iron_block", "M": "mithril_block"}, category="misc")


def generate_tags():
    ores = ["mithril_ore", "deepslate_mithril_ore", "arcanite_ore", "deepslate_arcanite_ore", "adamantite_ore"]
    blocks = ores + ["mithril_block", "raw_mithril_block", "arcanite_block", "adamantite_block", "raw_adamantite_block"]
    tag("block", "mineable/pickaxe", blocks)
    tag("block", "needs_iron_tool", ["mithril_ore", "deepslate_mithril_ore", "arcanite_ore", "deepslate_arcanite_ore",
                                     "mithril_block", "raw_mithril_block", "arcanite_block"])
    tag("block", "needs_diamond_tool", ["adamantite_ore", "adamantite_block", "raw_adamantite_block"])
    tag("block", "ores", ores, namespace="c")
    tag("item", "ores", ores, namespace="c")

    tag("item", "swords", ["mithril_sword", "adamantite_sword", "kings_blade", "adamantite_hammer"])
    for tool in ("pickaxe", "axe", "shovel", "hoe"):
        tag("item", f"{tool}s", [f"mithril_{tool}", f"adamantite_{tool}"])
    for piece, tag_name in (("helmet", "head_armor"), ("chestplate", "chest_armor"), ("leggings", "leg_armor"), ("boots", "foot_armor")):
        tag("item", tag_name, [f"{metal}_{piece}" for metal in ("mithril", "arcanite", "adamantite")])
    tag("item", "enchantable/durability", list(STAFFS) + ["archmage_staff"])
    tag("item", "ingots", ["mithril_ingot", "adamantite_ingot"], namespace="c")
    tag("item", "gems", ["arcanite"], namespace="c")
    tag("entity_type", "bosses", ["skeleton_king", "corrupted_archmage", "adamantite_golem"], namespace="c")


def generate_worldgen():
    ore_feature("ore_mithril", 8, 8, -32, 56, "mithril_ore", "deepslate_mithril_ore")
    ore_feature("ore_arcanite", 5, 5, -64, 16, "arcanite_ore", "deepslate_arcanite_ore")
    ore_feature("ore_adamantite", 4, 3, -64, -24, None, "adamantite_ore", air_discard=0.5)


if __name__ == "__main__":
    generate_textures_and_models()
    generate_loot()
    generate_recipes()
    generate_tags()
    generate_worldgen()
    print("Resources generated in", ROOT)
