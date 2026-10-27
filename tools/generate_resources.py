#!/usr/bin/env python3
"""Generates Arcane Quest textures, models, blockstates, recipes, loot tables, tags and worldgen.

Run from the repository root:  python3 tools/generate_resources.py
Textures are original pixel art drawn by tools/textures.py (no Mojang or resource pack texture is copied).
Translations (lang files) are written by hand and are not touched.
"""
import json
from pathlib import Path

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
