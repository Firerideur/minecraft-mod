#!/usr/bin/env python3
"""Generates Arcane Quest textures, models, blockstates, recipes, loot tables, tags and worldgen.

Run from the repository root:  python3 tools/generate_resources.py
Textures are original pixel art drawn by tools/textures.py (no Mojang or resource pack texture is copied).
Translations (lang files) are written by hand and are not touched.
"""
import json
from pathlib import Path

import boss_models
import dungeon_mobs
import textures

MODID = "arcanequest"
ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / MODID
DATA = ROOT / "data"

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

METALS = ("mithril", "adamantite")
TOOLS = ("sword", "pickaxe", "axe", "shovel", "hoe")
ARMOR = ("helmet", "chestplate", "leggings", "boots")
STAFFS = ("fire_staff", "frost_staff", "healing_staff", "arcane_staff", "lightning_staff")
BLOCKS = ("mithril_ore", "deepslate_mithril_ore", "mithril_block", "raw_mithril_block", "arcanite_ore",
          "deepslate_arcanite_ore", "arcanite_block", "adamantite_ore", "adamantite_block", "raw_adamantite_block")


def generate_textures_and_models():
    textures.generate(ASSETS)
    boss_models.generate(ASSETS)
    dungeon_mobs.generate(ASSETS)

    for block in BLOCKS:
        block_assets(block)
    for metal in METALS:
        item_model(f"raw_{metal}")
        item_model(f"{metal}_ingot")
        for tool in TOOLS:
            item_model(f"{metal}_{tool}", "item/handheld")
    item_model("arcanite")
    for metal in ("mithril", "arcanite", "adamantite"):
        for piece in ARMOR:
            item_model(f"{metal}_{piece}")
    for staff in STAFFS + ("archmage_staff",):
        item_model(staff, "item/handheld")
    for item in ("cursed_skull", "forbidden_grimoire", "golem_core"):
        item_model(item)
    for weapon in ("kings_blade", "adamantite_hammer"):
        item_model(weapon, "item/handheld")
    for egg in ("skeleton_king", "corrupted_archmage", "adamantite_golem", "goblin", "goblin_shaman", "steelfang_wolf",
                "ice_elf", "ant_soldier", "high_orc", "demon_knight", "troll", "minotaur", "gargoyle", "lich", "naga",
                "mimic", "treant", "basilisk", "giant_spider", "mantis", "beetle", "giant_wasp", "werewolf", "ratman",
                "lizardman", "ghoul", "imp", "succubus", "greater_demon"):
        write_json(ASSETS / "models" / "item" / f"{egg}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def generate_dimension():
    """The dungeon dimension behind the gates: an empty, dark void where dungeons are built."""
    write_json(DATA / MODID / "dimension_type" / "dungeon.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": True,
        "ambient_light": 0.12, "fixed_time": 18000, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "piglin_safe": False, "bed_works": False, "respawn_anchor_works": False, "has_raids": False,
        "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": "minecraft:the_end",
    })
    write_json(DATA / MODID / "dimension" / "dungeon.json", {
        "type": "arcanequest:dungeon",
        "generator": {"type": "minecraft:flat", "settings": {
            "biome": "minecraft:the_void", "layers": [], "lakes": False, "features": False, "structure_overrides": []}},
    })


def generate_entity_tags():
    """Undead monsters take extra damage from holy skills and Smite."""
    write_json(DATA / "minecraft" / "tags" / "entity_type" / "undead.json",
               {"replace": False, "values": ["arcanequest:lich", "arcanequest:ghoul"]})


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
    tag("block", "mineable/pickaxe", blocks + ["fusion_forge", "weapon_forge"])
    tag("block", "needs_iron_tool", ["mithril_ore", "deepslate_mithril_ore", "arcanite_ore", "deepslate_arcanite_ore",
                                     "mithril_block", "raw_mithril_block", "arcanite_block"])
    tag("block", "needs_diamond_tool", ["adamantite_ore", "adamantite_block", "raw_adamantite_block"])
    tag("block", "ores", ores, namespace="c")
    tag("item", "ores", ores, namespace="c")

    tag("item", "swords", ["mithril_sword", "adamantite_sword", "kings_blade", "adamantite_hammer"] + ALL_WEAPONS)
    for tool in ("pickaxe", "axe", "shovel", "hoe"):
        tag("item", f"{tool}s", [f"mithril_{tool}", f"adamantite_{tool}"] + (ALL_HAMMERS if tool == "pickaxe" else []))
    for piece, tag_name in (("helmet", "head_armor"), ("chestplate", "chest_armor"), ("leggings", "leg_armor"), ("boots", "foot_armor")):
        tag("item", tag_name, [f"{metal}_{piece}" for metal in ("mithril", "arcanite", "adamantite")])
    tag("item", "enchantable/durability", list(STAFFS) + ["archmage_staff"])
    tag("item", "ingots", ["mithril_ingot", "adamantite_ingot"], namespace="c")
    tag("item", "gems", ["arcanite"], namespace="c")
    tag("entity_type", "bosses", ["skeleton_king", "corrupted_archmage", "adamantite_golem"], namespace="c")


def generate_dungeon_seal():
    """The magic wall closing a boss arena: violet runes swirling on a dark background (animated, 8 frames)."""
    import math
    from PIL import Image
    frames = 8
    image = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        phase = f / frames * 2 * math.pi
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = math.hypot(dx, dy)
                a = math.atan2(dy, dx)
                swirl = 0.5 + 0.5 * math.sin(a * 3 + r * 0.9 - phase)
                ring = max(0.0, 1.0 - abs(r - 5.5) / 1.2)
                rune = 1.0 if (x + y + f) % 7 == 0 and r < 7 else 0.0
                glow = min(1.0, 0.25 + 0.45 * swirl + 0.6 * ring + 0.5 * rune)
                image.putpixel((x, y + 16 * f), (int(60 + 170 * glow), int(20 + 60 * glow * glow), int(110 + 145 * glow), 255))
    out = ASSETS / "textures" / "block"
    out.mkdir(parents=True, exist_ok=True)
    image.save(out / "dungeon_seal.png")
    write_json(out / "dungeon_seal.png.mcmeta", {"animation": {"frametime": 3, "interpolate": True}})
    write_json(ASSETS / "models" / "block" / "dungeon_seal.json",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/dungeon_seal"}})
    write_json(ASSETS / "blockstates" / "dungeon_seal.json", {"variants": {"": {"model": f"{MODID}:block/dungeon_seal"}}})


def generate_worldgen():
    ore_feature("ore_mithril", 8, 8, -32, 56, "mithril_ore", "deepslate_mithril_ore")
    ore_feature("ore_arcanite", 5, 5, -64, 16, "arcanite_ore", "deepslate_arcanite_ore")
    ore_feature("ore_adamantite", 4, 3, -64, -24, None, "adamantite_ore", air_discard=0.5)
    generate_caves()


def uniform_height(low, high):
    return {"type": "minecraft:uniform", "min_inclusive": {"absolute": low}, "max_inclusive": {"absolute": high}}


def uniform_float(low, high):
    return {"type": "minecraft:uniform", "min_inclusive": low, "max_exclusive": high}


def generate_caves():
    """Better Caves style: giant caverns, wide winding tunnels, deep ravines, underground lakes and cave regions."""
    carver_base = {"lava_level": {"above_bottom": 8}, "replaceable": "#minecraft:overworld_carver_replaceables"}
    carvers = {
        # One huge flat-floored chamber with very wide tunnels (see CavernCarver, which picks its own floor_level)
        "cavern": {"type": f"{MODID}:cavern", "config": {
            **carver_base, "probability": 0.04, "y": uniform_height(-48, 24), "yScale": uniform_float(0.5, 0.7),
            "horizontal_radius_multiplier": uniform_float(1.0, 1.6), "vertical_radius_multiplier": uniform_float(0.9, 1.3),
            "floor_level": uniform_float(-0.75, -0.45)}},
        # More, wider, winding tunnels between the caverns
        "winding_tunnels": {"type": "minecraft:cave", "config": {
            **carver_base, "probability": 0.08, "y": uniform_height(-56, 48), "yScale": uniform_float(0.3, 0.9),
            "horizontal_radius_multiplier": uniform_float(1.2, 2.2), "vertical_radius_multiplier": uniform_float(1.0, 1.6),
            "floor_level": uniform_float(-1.0, -0.5)}},
        # Deep, wide ravines
        "deep_ravine": {"type": "minecraft:canyon", "config": {
            **carver_base, "probability": 0.012, "y": uniform_height(-24, 40), "yScale": {"type": "minecraft:constant", "value": 3.0},
            "vertical_rotation": uniform_float(-0.125, 0.125),
            "shape": {"distance_factor": uniform_float(0.75, 1.0),
                      "thickness": {"type": "minecraft:trapezoid", "min": 2.0, "max": 9.0, "plateau": 3.0},
                      "width_smoothness": 3, "horizontal_radius_factor": uniform_float(1.0, 1.4),
                      "vertical_radius_default_factor": 1.2, "vertical_radius_center_factor": 0.0}}},
    }
    for name, carver in carvers.items():
        write_json(DATA / MODID / "worldgen" / "configured_carver" / f"{name}.json", carver)
    write_json(DATA / MODID / "forge" / "biome_modifier" / "add_cave_carvers.json", {
        "type": f"{MODID}:add_carvers", "biomes": "#minecraft:is_overworld",
        "carvers": [rl(name) for name in carvers], "step": "air",
    })

    # Underground lakes dug into cave floors (see UndergroundLakeFeature; vanilla water lakes crash underground)
    write_json(DATA / MODID / "worldgen" / "configured_feature" / "underground_lake.json",
               {"type": rl("underground_lake"), "config": {}})
    write_json(DATA / MODID / "worldgen" / "placed_feature" / "underground_lake.json", {
        "feature": rl("underground_lake"),
        "placement": [
            {"type": "minecraft:count", "count": 2},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": uniform_height(-50, 40)},
            {"type": "minecraft:environment_scan", "direction_of_search": "down", "max_steps": 24,
             "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air", "minecraft:cave_air"]},
             "target_condition": {"type": "minecraft:all_of", "predicates": [
                 {"type": "minecraft:solid"},
                 {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air", "minecraft:cave_air"], "offset": [0, 1, 0]}]}},
            {"type": "minecraft:surface_relative_threshold_filter", "heightmap": "OCEAN_FLOOR_WG", "max_inclusive": -12},
            {"type": "minecraft:biome"},
        ],
    })
    # Cave regions (see CaveDecoratorFeature): once per chunk, after the vanilla cave decorations
    write_json(DATA / MODID / "worldgen" / "configured_feature" / "cave_decorator.json",
               {"type": rl("cave_decorator"), "config": {}})
    write_json(DATA / MODID / "worldgen" / "placed_feature" / "cave_decorator.json",
               {"feature": rl("cave_decorator"), "placement": []})
    for name, step in (("underground_lake", "lakes"), ("cave_decorator", "underground_decoration")):
        write_json(DATA / MODID / "forge" / "biome_modifier" / f"add_{name}.json", {
            "type": "forge:add_features", "biomes": "#minecraft:is_overworld", "features": rl(name), "step": step,
        })


# ---------------------------------------------------------------------------
# Weapons, Fusion Forge and Weapon Forge
# ---------------------------------------------------------------------------

WEAPON_TYPES = {  # type -> (ingots needed, handle item, in-hand scale)
    "dagger": (1, "weapon_grip", 0.75),
    "katana": (3, "weapon_grip", 1.15),
    "longsword": (4, "weapon_grip", 1.4),
    "spear": (2, "weapon_shaft", 1.5),
    "scythe": (3, "weapon_shaft", 1.45),
}
WEAPON_MATERIALS = {  # material -> ingredient used in the Weapon Forge
    "iron": "minecraft:iron_ingot",
    "steel": "steel_ingot",
    "diamond": "minecraft:diamond",
    "mithril": "mithril_ingot",
    "orichalcum": "orichalcum_ingot",
    "adamantite": "adamantite_ingot",
    "arcanium": "arcanium_ingot",
    "starsteel": "starsteel_ingot",
}
ALL_WEAPONS = [f"{m}_{t}" for m in WEAPON_MATERIALS for t in WEAPON_TYPES]
ALL_HAMMERS = [f"{m}_mining_hammer" for m in WEAPON_MATERIALS]
HAMMER_COST = 5
ALLOYS = ("steel", "orichalcum", "arcanium", "starsteel")


def weapon_model(name, scale):
    """Handheld model, bigger (or smaller) in hand depending on the weapon."""
    write_json(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/handheld",
        "textures": {"layer0": f"{MODID}:item/{name}"},
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0 * scale, 0.5],
                                      "scale": [0.85 * scale] * 3},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0 * scale, 0.5],
                                     "scale": [0.85 * scale] * 3},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13],
                                      "scale": [0.68 * min(scale, 1.0)] * 3},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13],
                                     "scale": [0.68 * min(scale, 1.0)] * 3},
        },
    })


def fusion(name, first, second, result, count=1, time=200):
    def counted(entry):
        item, amount = entry
        return {"ingredient": ingredient(item), "count": amount}
    write_json(DATA / MODID / "recipe" / "fusion" / f"{name}.json", {
        "type": f"{MODID}:fusion", "first": counted(first), "second": counted(second),
        "result": {"id": rl(result), "count": count}, "time": time,
    })


def generate_forging():
    for name in ALL_WEAPONS:
        weapon_model(name, WEAPON_TYPES[name.split("_", 1)[1]][2])
    for alloy in ALLOYS:
        item_model(f"{alloy}_ingot")
    for name in ALL_HAMMERS:
        weapon_model(name, 1.3)
    for weapon in list(WEAPON_TYPES) + ["hammer"]:
        item_model(f"{weapon}_template")
    for item in ("weapon_grip", "weapon_shaft", "fusion_catalyst"):
        item_model(item)

    # Fusion Forge: rotates with its facing, glows while working
    for suffix, front in (("", "fusion_forge_front"), ("_on", "fusion_forge_front_on")):
        write_json(ASSETS / "models" / "block" / f"fusion_forge{suffix}.json", {
            "parent": "minecraft:block/orientable",
            "textures": {"front": f"{MODID}:block/{front}", "side": f"{MODID}:block/fusion_forge_side",
                         "top": f"{MODID}:block/fusion_forge_top"},
        })
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    write_json(ASSETS / "blockstates" / "fusion_forge.json", {"variants": {
        f"facing={facing},lit={str(lit).lower()}": {"model": f"{MODID}:block/fusion_forge{'_on' if lit else ''}",
                                                     **({"y": rot} if rot else {})}
        for facing, rot in rotations.items() for lit in (False, True)
    }})
    write_json(ASSETS / "models" / "item" / "fusion_forge.json", {"parent": f"{MODID}:block/fusion_forge"})

    write_json(ASSETS / "models" / "block" / "weapon_forge.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{MODID}:block/weapon_forge_top", "side": f"{MODID}:block/weapon_forge_side",
                     "bottom": f"{MODID}:block/weapon_forge_bottom"},
    })
    write_json(ASSETS / "blockstates" / "weapon_forge.json", {"variants": {"": {"model": f"{MODID}:block/weapon_forge"}}})
    write_json(ASSETS / "models" / "item" / "weapon_forge.json", {"parent": f"{MODID}:block/weapon_forge"})
    loot_self("fusion_forge")
    loot_self("weapon_forge")

    # Crafting
    shaped("fusion_forge", "fusion_forge", ["MMM", "AFA", "BBB"],
           {"M": "mithril_ingot", "A": "arcanite", "F": "minecraft:blast_furnace", "B": "minecraft:polished_blackstone_bricks"},
           category="misc")
    shaped("weapon_forge", "weapon_forge", ["III", "PAP", "PPP"],
           {"I": "minecraft:iron_ingot", "A": "minecraft:anvil", "P": "#minecraft:planks"}, category="misc")
    shapeless("fusion_catalyst", "fusion_catalyst", ["arcanite", "minecraft:blaze_powder", "minecraft:redstone"], 2)
    shapeless("weapon_grip", "weapon_grip", ["minecraft:stick", "minecraft:leather", "minecraft:string"], 2)
    shapeless("weapon_shaft", "weapon_shaft", ["minecraft:stick", "minecraft:stick", "minecraft:stick", "minecraft:string"])
    template_extras = {
        "dagger": ["minecraft:iron_nugget"],
        "katana": ["minecraft:iron_ingot", "minecraft:string"],
        "longsword": ["minecraft:iron_ingot", "minecraft:iron_ingot"],
        "spear": ["minecraft:iron_ingot", "minecraft:stick"],
        "scythe": ["minecraft:iron_ingot", "minecraft:wheat"],
    }
    template_extras["hammer"] = ["minecraft:iron_block"]
    for weapon, extras in template_extras.items():
        shapeless(f"{weapon}_template", f"{weapon}_template", ["minecraft:paper"] + extras)

    # Fusion
    fusion("steel_ingot", ("minecraft:iron_ingot", 1), ("#minecraft:coals", 2), "steel_ingot", time=100)
    fusion("orichalcum_ingot", ("mithril_ingot", 1), ("minecraft:gold_ingot", 1), "orichalcum_ingot", time=200)
    fusion("arcanium_ingot", ("arcanite", 2), ("adamantite_ingot", 1), "arcanium_ingot", time=300)
    fusion("starsteel_ingot", ("mithril_ingot", 2), ("adamantite_ingot", 1), "starsteel_ingot", time=400)

    # Weapon Forge
    for material, ingot in WEAPON_MATERIALS.items():
        write_json(DATA / MODID / "recipe" / "weapon_forging" / f"{material}_mining_hammer.json", {
            "type": f"{MODID}:weapon_forging",
            "template": ingredient("hammer_template"),
            "material": {"ingredient": ingredient(ingot), "count": HAMMER_COST},
            "handle": ingredient("weapon_shaft"),
            "result": {"id": rl(f"{material}_mining_hammer"), "count": 1},
        })
        for weapon, (cost, handle, _) in WEAPON_TYPES.items():
            write_json(DATA / MODID / "recipe" / "weapon_forging" / f"{material}_{weapon}.json", {
                "type": f"{MODID}:weapon_forging",
                "template": ingredient(f"{weapon}_template"),
                "material": {"ingredient": ingredient(ingot), "count": cost},
                "handle": ingredient(handle),
                "result": {"id": rl(f"{material}_{weapon}"), "count": 1},
            })


if __name__ == "__main__":
    generate_forging()
    generate_textures_and_models()
    generate_dungeon_seal()
    generate_loot()
    generate_recipes()
    generate_tags()
    generate_worldgen()
    generate_dimension()
    generate_entity_tags()
    print("Resources generated in", ROOT)
