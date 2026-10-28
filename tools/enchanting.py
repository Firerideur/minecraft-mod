"""Arcane enchanting: textures, models, recipes, loot and tags of the Arcane Enchanting Table and its bookshelves."""
import random

from PIL import Image

MODID = "arcanequest"

# name: (frame color, frame accent, book colors, glow color or None, recipe pattern, recipe key, fr, en)
SHELVES = {
    "reinforced_bookshelf": ((72, 72, 80), (98, 98, 108), [(120, 60, 50), (60, 80, 110), (70, 100, 60), (130, 110, 70)], None,
                             [" D ", "DSD", " D "], {"D": "minecraft:polished_deepslate"},
                             "Bibliothèque renforcée", "Reinforced Bookshelf"),
    "mithril_bookshelf": ((140, 165, 190), (205, 225, 240), [(60, 110, 180), (220, 230, 240), (90, 150, 200), (40, 70, 130)], None,
                          [" M ", "MSM", " M "], {"M": "mithril_ingot"},
                          "Bibliothèque de mithril", "Mithril Bookshelf"),
    "anchored_bookshelf": ((30, 20, 44), (70, 50, 100), [(40, 40, 50), (90, 80, 110), (60, 55, 70), (120, 110, 130)], None,
                           ["OCO", "CSC", "OCO"], {"O": "minecraft:obsidian", "C": "minecraft:chain"},
                           "Bibliothèque ancrée", "Anchored Bookshelf"),
    "infernal_bookshelf": ((72, 28, 34), (110, 45, 50), [(180, 40, 30), (230, 120, 40), (120, 20, 20), (200, 80, 30)], (255, 150, 50),
                           ["NBN", "BSB", "NBN"], {"N": "minecraft:nether_bricks", "B": "minecraft:blaze_rod"},
                           "Bibliothèque infernale", "Infernal Bookshelf"),
    "abyssal_bookshelf": ((50, 115, 105), (95, 170, 150), [(40, 90, 120), (80, 170, 160), (30, 60, 90), (150, 210, 200)], (120, 255, 230),
                          ["PCP", "CSC", "PCP"], {"P": "minecraft:prismarine_bricks", "C": "minecraft:prismarine_crystals"},
                          "Bibliothèque abyssale", "Abyssal Bookshelf"),
    "arcanite_bookshelf": ((100, 55, 150), (160, 110, 210), [(150, 70, 200), (90, 40, 140), (200, 140, 240), (60, 30, 100)], (230, 160, 255),
                           [" A ", "ASA", " A "], {"A": "arcanite"},
                           "Bibliothèque d'arcanite", "Arcanite Bookshelf"),
    "adamantite_bookshelf": ((110, 28, 40), (170, 60, 70), [(150, 30, 40), (220, 180, 80), (90, 20, 30), (200, 60, 60)], None,
                             [" A ", "ASA", " A "], {"A": "adamantite_ingot"},
                             "Bibliothèque d'adamantite", "Adamantite Bookshelf"),
    "draconic_bookshelf": ((200, 190, 145), (150, 95, 160), [(60, 20, 80), (170, 60, 190), (30, 10, 40), (220, 120, 230)], (255, 120, 255),
                           ["EDE", "DSD", "EDE"], {"E": "minecraft:end_stone_bricks", "D": "minecraft:dragon_breath"},
                           "Bibliothèque draconique", "Draconic Bookshelf"),
}


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def shelf_textures(name, frame, accent, books, glow, rng):
    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = y in (0, 15) or y in (7, 8) or x in (0, 15)
            if edge:
                c = accent if (y in (0, 7) and x not in (0, 15)) else frame
                side.putpixel((x, y), shade(c, 0.9 + 0.2 * rng.random()) + (255,))
    for row_top in (1, 9):
        x = 1
        while x < 15:
            width = 1 + rng.randrange(2)
            height = 5 + rng.randrange(2)
            color = books[rng.randrange(len(books))]
            for bx in range(x, min(15, x + width)):
                for by in range(row_top + 6 - height, row_top + 6):
                    light = 1.15 if bx == x else 0.85 if bx == x + width - 1 else 1.0
                    side.putpixel((bx, by), shade(color, light * (1.0 - 0.04 * (by - row_top))) + (255,))
                # A band on the spine
                side.putpixel((bx, row_top + 6 - height + 1), shade(color, 1.4) + (255,))
            if glow and rng.random() < 0.3:
                side.putpixel((x, row_top + 3), glow + (255,))
            x += width
        # Dark gap above the books
        for gx in range(1, 15):
            if side.getpixel((gx, row_top))[3] == 0:
                side.putpixel((gx, row_top), shade(frame, 0.35) + (255,))
    for y in range(16):
        for x in range(16):
            if side.getpixel((x, y))[3] == 0:
                side.putpixel((x, y), shade(frame, 0.3) + (255,))
    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            border = x in (0, 15) or y in (0, 15)
            base = accent if border else frame
            plank = 0.85 if y % 4 == 0 else 1.0
            top.putpixel((x, y), shade(base, plank * (0.9 + 0.2 * rng.random())) + (255,))
    return side, top


def table_textures(rng):
    gold, purple, dark = (220, 175, 70), (90, 40, 140), (24, 14, 34)
    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            c = shade(purple, 0.8 + 0.2 * rng.random())
            if abs(r - 5.5) < 0.6 or abs(r - 2.2) < 0.5:
                c = gold
            if x in (0, 15) or y in (0, 15):
                c = shade(dark, 1.3)
            top.putpixel((x, y), c + (255,))
    for x, y in ((7, 2), (8, 2), (2, 7), (13, 8), (7, 13), (8, 13)):
        top.putpixel((x, y), (255, 220, 120, 255))
    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = shade(dark, 0.8 + 0.5 * rng.random())
            if y < 4:
                c = shade(purple, 0.7 + 0.2 * rng.random()) if y > 0 else gold
            elif (x + y) % 5 == 0 and 6 <= y <= 14 and 2 <= x <= 13:
                c = (190, 110, 255)
            side.putpixel((x, y), c + (255,))
    bottom = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            bottom.putpixel((x, y), shade(dark, 0.7 + 0.5 * rng.random()) + (255,))
    return top, side, bottom


def generate(assets, write_json, shaped, loot_self, langs):
    rng = random.Random(1234)
    blocks = assets / "textures" / "block"
    blocks.mkdir(parents=True, exist_ok=True)
    for name, (frame, accent, books, glow, pattern, key, fr, en) in SHELVES.items():
        side, top = shelf_textures(name, frame, accent, books, glow, rng)
        side.save(blocks / f"{name}.png")
        top.save(blocks / f"{name}_top.png")
        write_json(assets / "models" / "block" / f"{name}.json", {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": f"{MODID}:block/{name}_top", "side": f"{MODID}:block/{name}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
        write_json(assets / "models" / "item" / f"{name}.json", {"parent": f"{MODID}:block/{name}"})
        shaped(name, name, pattern, {**key, "S": "minecraft:bookshelf"}, category="building")
        loot_self(name)
        langs["fr_fr"][f"block.{MODID}.{name}"] = fr
        langs["en_us"][f"block.{MODID}.{name}"] = en

    top, side, bottom = table_textures(rng)
    for part, image in (("top", top), ("side", side), ("bottom", bottom)):
        image.save(blocks / f"arcane_enchanting_table_{part}.png")
    write_json(assets / "models" / "block" / "arcane_enchanting_table.json", {
        "parent": "minecraft:block/enchanting_table",
        "textures": {"particle": f"{MODID}:block/arcane_enchanting_table_bottom",
                     "bottom": f"{MODID}:block/arcane_enchanting_table_bottom",
                     "top": f"{MODID}:block/arcane_enchanting_table_top",
                     "side": f"{MODID}:block/arcane_enchanting_table_side"}})
    write_json(assets / "blockstates" / "arcane_enchanting_table.json",
               {"variants": {"": {"model": f"{MODID}:block/arcane_enchanting_table"}}})
    write_json(assets / "models" / "item" / "arcane_enchanting_table.json", {"parent": f"{MODID}:block/arcane_enchanting_table"})
    shaped("arcane_enchanting_table", "arcane_enchanting_table", [" A ", "ATA", "OOO"],
           {"A": "arcanite", "T": "minecraft:enchanting_table", "O": "minecraft:crying_obsidian"}, category="misc")
    loot_self("arcane_enchanting_table")

    texts = {
        "fr_fr": {
            "block.arcanequest.arcane_enchanting_table": "Table d'enchantement arcanique",
            "container.arcanequest.arcane_enchanting": "Enchantement arcanique",
            "screen.arcanequest.enchanting.power": "Puissance",
            "screen.arcanequest.enchanting.chaos": "Hasard",
            "screen.arcanequest.enchanting.arcana": "Arcane",
            "screen.arcanequest.enchanting.max_level": "Niveau max : %s",
            "screen.arcanequest.enchanting.requires": "Niveau %s requis",
            "screen.arcanequest.enchanting.consumes": "Consomme %s niveau(x) et autant de lapis",
            "screen.arcanequest.enchanting.cannot": "Pas assez de niveaux ou de lapis",
            "tooltip.arcanequest.shelf": "Puissance +%s (jusqu'à %s) · Hasard %s%% · Arcane %s%%",
        },
        "en_us": {
            "block.arcanequest.arcane_enchanting_table": "Arcane Enchanting Table",
            "container.arcanequest.arcane_enchanting": "Arcane Enchanting",
            "screen.arcanequest.enchanting.power": "Power",
            "screen.arcanequest.enchanting.chaos": "Chaos",
            "screen.arcanequest.enchanting.arcana": "Arcana",
            "screen.arcanequest.enchanting.max_level": "Max level: %s",
            "screen.arcanequest.enchanting.requires": "Level %s required",
            "screen.arcanequest.enchanting.consumes": "Uses %s level(s) and as much lapis",
            "screen.arcanequest.enchanting.cannot": "Not enough levels or lapis",
            "tooltip.arcanequest.shelf": "Power +%s (up to %s) · Chaos %s%% · Arcana %s%%",
        },
    }
    for code, values in texts.items():
        langs[code].update(values)
    return list(SHELVES)
