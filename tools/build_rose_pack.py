#!/usr/bin/env python3
"""Builds a personal resource pack that gives Arcane Quest items the exact look of the Rose Revamp pack.

The Rose Revamp textures belong to their author, so they are NOT included in this repository.
This script reads YOUR copy of the pack and recolors its textures for the mod's items:

    python3 tools/build_rose_pack.py "Rose Revamp 1.15.zip" "ArcaneQuest x Rose Revamp.zip"

Put the resulting zip in .minecraft/resourcepacks and enable it (on top of Rose Revamp if you like).
Keep it for personal use: don't share it without the Rose Revamp author's permission.
"""
import colorsys
import io
import json
import sys
import zipfile

from PIL import Image

MODID = "arcanequest"
SOURCE = "assets/minecraft/textures/"
TARGET = f"assets/{MODID}/textures/"

# Target colors (RGB) for each material.
MITHRIL = (110, 200, 255)
ARCANITE = (190, 80, 255)
ADAMANTITE = (235, 30, 70)
FIRE = (255, 130, 20)
FROST = (110, 230, 255)
HEAL = (255, 110, 170)
LIGHTNING = (255, 235, 70)


def recolor(img, color, saturation_threshold=0.15, value_scale=1.0, gray_scale=1.0):
    """Gives every colored pixel the hue of `color`, keeping its saturation and brightness,
    so shading and highlights stay exactly as drawn. Black, grey and white pixels are kept."""
    target_hue, target_sat, _ = colorsys.rgb_to_hsv(*(c / 255 for c in color))
    out = img.convert("RGBA").copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s >= saturation_threshold:
                # Keep the drawn saturation, but pull very pale targets (light blue) toward their own saturation.
                s = min(1.0, s * (0.5 + 0.5 * target_sat / max(target_sat, 0.01)))
                nr, ng, nb = colorsys.hsv_to_rgb(target_hue, s, min(1.0, v * value_scale))
            else:
                nr, ng, nb = r / 255 * gray_scale, g / 255 * gray_scale, b / 255 * gray_scale
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def flip(img):
    return img.transpose(Image.Transpose.FLIP_LEFT_RIGHT)


# our texture path -> (pack texture, color, options)
TOOLS = ("sword", "pickaxe", "axe", "shovel", "hoe")
ARMOR = ("helmet", "chestplate", "leggings", "boots")
MAPPING = {}
for metal, color in (("mithril", MITHRIL), ("adamantite", ADAMANTITE)):
    for tool in TOOLS:
        MAPPING[f"item/{metal}_{tool}"] = (f"item/diamond_{tool}", color, {})
    MAPPING[f"item/{metal}_ingot"] = ("item/gold_ingot", color, {})
    MAPPING[f"item/raw_{metal}"] = ("item/gold_nugget", color, {"value_scale": 0.85})
    MAPPING[f"block/{metal}_block"] = ("block/diamond_block", color, {})
    MAPPING[f"block/raw_{metal}_block"] = ("block/gold_block", color, {"value_scale": 0.7})
for metal, color in (("mithril", MITHRIL), ("arcanite", ARCANITE), ("adamantite", ADAMANTITE)):
    for piece in ARMOR:
        MAPPING[f"item/{metal}_{piece}"] = (f"item/diamond_{piece}", color, {})
    MAPPING[f"models/armor/{metal}_layer_1"] = ("models/armor/diamond_layer_1", color, {})
    MAPPING[f"models/armor/{metal}_layer_2"] = ("models/armor/diamond_layer_2", color, {})

MAPPING.update({
    "item/arcanite": ("item/emerald", ARCANITE, {}),
    "block/arcanite_block": ("block/emerald_block", ARCANITE, {}),
    "block/mithril_ore": ("block/diamond_ore", MITHRIL, {}),
    "block/deepslate_mithril_ore": ("block/diamond_ore", MITHRIL, {"gray_scale": 0.55}),
    "block/arcanite_ore": ("block/diamond_ore", ARCANITE, {}),
    "block/deepslate_arcanite_ore": ("block/diamond_ore", ARCANITE, {"gray_scale": 0.55}),
    "block/adamantite_ore": ("block/diamond_ore", ADAMANTITE, {"gray_scale": 0.55}),
    "item/fire_staff": ("item/blaze_rod", FIRE, {}),
    "item/frost_staff": ("item/blaze_rod", FROST, {}),
    "item/healing_staff": ("item/blaze_rod", HEAL, {}),
    "item/arcane_staff": ("item/blaze_rod", ARCANITE, {}),
    "item/lightning_staff": ("item/blaze_rod", LIGHTNING, {}),
    "item/archmage_staff": ("item/blaze_rod", ARCANITE, {"value_scale": 0.65}),
    "item/kings_blade": ("item/golden_sword", (255, 200, 60), {}),
    "item/adamantite_hammer": ("item/golden_axe", ADAMANTITE, {"value_scale": 0.9}),
    "item/golem_core": ("item/diamond", ADAMANTITE, {}),
})


def build(source_zip, output_zip):
    with zipfile.ZipFile(source_zip) as src:
        names = set(src.namelist())
        written, missing = [], []
        with zipfile.ZipFile(output_zip, "w", zipfile.ZIP_DEFLATED) as out:
            out.writestr("pack.mcmeta", json.dumps({"pack": {
                "pack_format": 34,
                "description": "Arcane Quest x Rose Revamp (usage personnel)",
            }}, indent=2))
            for target, (source, color, options) in sorted(MAPPING.items()):
                path = SOURCE + source + ".png"
                if path not in names:
                    missing.append(source)
                    continue
                img = Image.open(io.BytesIO(src.read(path)))
                if img.height > img.width:  # animated strip: keep the first frame
                    img = img.crop((0, 0, img.width, img.width))
                buffer = io.BytesIO()
                recolor(img, color, **options).save(buffer, "PNG")
                out.writestr(TARGET + target + ".png", buffer.getvalue())
                written.append(target)
    print(f"{len(written)} textures written to {output_zip}")
    if missing:
        print("Not in the source pack (the mod's own textures stay):", ", ".join(sorted(set(missing))))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    build(sys.argv[1], sys.argv[2])
