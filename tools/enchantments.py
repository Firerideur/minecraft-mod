"""Arcane Quest enchantments (Minecraft 1.21 data-driven enchantments).

The simple ones are complete here (effects on hit, attributes); the others only declare themselves and their
behavior is in EnchantmentEvents.java. Every enchantment also gets a French and English name and description
(the "enchantment.<id>.desc" keys are the ones Enchantment Descriptions-style mods read; the mod shows them on
enchanted books too).
"""

MODID = "arcanequest"


def linear(base, per):
    return {"type": "minecraft:linear", "base": base, "per_level_above_first": per}


def direct_hit():
    return {"condition": "minecraft:damage_source_properties", "predicate": {"is_direct": True}}


def on_hit_effect(effect, duration, amplifier):
    return {"minecraft:post_attack": [{
        "affected": "victim", "enchanted": "attacker", "requirements": direct_hit(),
        "effect": {"type": "minecraft:apply_mob_effect", "to_apply": effect,
                   "min_duration": duration, "max_duration": duration,
                   "min_amplifier": amplifier, "max_amplifier": amplifier}}]}


def attribute(name, attr, amount, operation="add_value"):
    return {"minecraft:attributes": [{"id": f"{MODID}:enchantment.{name}", "attribute": attr,
                                      "amount": amount, "operation": operation}]}


WEAPON = "#minecraft:enchantable/weapon"
SWORD = "#minecraft:enchantable/sword"
MINING = "#minecraft:enchantable/mining"
TOOL_OR_WEAPON = f"#{MODID}:enchantable/tool_or_weapon"
RANGED = f"#{MODID}:enchantable/ranged"
STAFF = f"#{MODID}:enchantable/staff"

# name: (supported, primary, slots, max_level, weight, (min base, min per level), treasure, effects, exclusive,
#        french name, french description, english name, english description)
ENCHANTMENTS = {
    # ---------------------------------------------------------------- weapons
    "lifesteal": (WEAPON, SWORD, ["mainhand"], 3, 2, (15, 10), False, None, None,
                  "Vol de vie", "Soigne de 5 % des dégâts infligés par niveau.",
                  "Lifesteal", "Heals 5% of the damage dealt per level."),
    "executioner": (WEAPON, SWORD, ["mainhand"], 3, 2, (10, 10), False, None, None,
                    "Bourreau", "+12 % de dégâts par niveau contre les ennemis sous 35 % de vie.",
                    "Executioner", "+12% damage per level against enemies under 35% health."),
    "critical": (WEAPON, SWORD, ["mainhand"], 3, 2, (12, 10), False, None, None,
                 "Coup critique", "5 % de chances par niveau d'infliger ×1,6 dégâts.",
                 "Critical Strike", "5% chance per level to deal ×1.6 damage."),
    "giant_slayer": (WEAPON, SWORD, ["mainhand"], 5, 2, (5, 8), False, None, None,
                     "Tueur de géants", "+8 % de dégâts par niveau contre plus costaud que vous, +15 % contre les boss.",
                     "Giant Slayer", "+8% damage per level against tougher foes, +15% against bosses."),
    "bleeding": (WEAPON, SWORD, ["mainhand"], 3, 2, (10, 10), False, None, None,
                 "Hémorragie", "Les coups font saigner : dégâts chaque seconde qui ignorent l'armure.",
                 "Bleeding", "Hits make the target bleed: damage every second that ignores armor."),
    "thunderstrike": (WEAPON, SWORD, ["mainhand"], 3, 1, (15, 12), False, None, None,
                      "Frappe de foudre", "6 % de chances par niveau d'appeler la foudre sur la cible (+3 dégâts par niveau, l'enflamme).",
                      "Thunderstrike", "6% chance per level to call lightning on the target (+3 damage per level, sets it on fire)."),
    "cleave": ("#minecraft:axes", "#minecraft:axes", ["mainhand"], 3, 2, (10, 10), False, None, None,
               "Fendoir", "Chaque coup touche aussi les ennemis proches (20 % des dégâts par niveau).",
               "Cleave", "Each hit also strikes nearby enemies (20% of the damage per level)."),
    "soul_harvest": (WEAPON, SWORD, ["mainhand"], 3, 2, (10, 10), False, None, None,
                     "Moisson d'âmes", "Chaque victime rend 4 de mana par niveau.",
                     "Soul Harvest", "Each kill restores 4 mana per level."),
    "frostbite": (WEAPON, SWORD, ["mainhand"], 2, 2, (10, 15), False,
                  on_hit_effect("minecraft:slowness", linear(2.0, 1.0), linear(0.0, 1.0)), None,
                  "Morsure du givre", "Les coups ralentissent la cible.",
                  "Frostbite", "Hits slow the target down."),
    "venom": (WEAPON, SWORD, ["mainhand"], 2, 2, (10, 15), False,
              on_hit_effect("minecraft:poison", linear(3.0, 2.0), linear(0.0, 1.0)), None,
              "Venin", "Les coups empoisonnent la cible.",
              "Venom", "Hits poison the target."),
    # ---------------------------------------------------------------- tools
    "vein_miner": ("#minecraft:pickaxes", "#minecraft:pickaxes", ["mainhand"], 3, 2, (15, 10), False, None, None,
                   "Filon", "Mine tout le filon d'un coup (16, 32 puis 64 blocs). Accroupi pour miner un seul bloc.",
                   "Vein Miner", "Mines the whole vein at once (16, 32 then 64 blocks). Sneak to mine a single block."),
    "timber": ("#minecraft:axes", "#minecraft:axes", ["mainhand"], 1, 2, (15, 0), False, None, None,
               "Bûcheron", "Abat l'arbre entier d'un coup. Accroupi pour couper une seule bûche.",
               "Timber", "Fells the whole tree at once. Sneak to cut a single log."),
    "autosmelt": (MINING, "#minecraft:pickaxes", ["mainhand"], 1, 1, (20, 0), False, None, "minecraft:silk_touch",
                  "Fonte automatique", "Les blocs minés sortent déjà cuits (minerais en lingots, sable en verre...).",
                  "Autosmelt", "Mined blocks come out already smelted (ores into ingots, sand into glass...)."),
    "telekinesis": (TOOL_OR_WEAPON, MINING, ["mainhand"], 1, 2, (15, 0), False, None, None,
                    "Télékinésie", "Le butin des blocs et des monstres va directement dans l'inventaire.",
                    "Telekinesis", "Loot from blocks and monsters goes straight into your inventory."),
    "wisdom": (TOOL_OR_WEAPON, MINING, ["mainhand"], 3, 2, (10, 10), False, None, None,
               "Sagesse", "+25 % d'expérience par niveau (blocs, monstres et XP de classe).",
               "Wisdom", "+25% experience per level (blocks, monsters and class XP)."),
    # ---------------------------------------------------------------- armor
    "vitality": ("#minecraft:enchantable/armor", "#minecraft:enchantable/chest_armor", ["armor"], 5, 2, (5, 9), False,
                 attribute("vitality", "minecraft:generic.max_health", linear(2.0, 2.0)), None,
                 "Vitalité", "+1 cœur de vie maximale par niveau.",
                 "Vitality", "+1 heart of max health per level."),
    "swiftness": ("#minecraft:enchantable/foot_armor", "#minecraft:enchantable/foot_armor", ["feet"], 3, 2, (10, 10), False,
                  attribute("swiftness", "minecraft:generic.movement_speed", linear(0.06, 0.06), "add_multiplied_base"), None,
                  "Célérité", "+6 % de vitesse de déplacement par niveau.",
                  "Swiftness", "+6% movement speed per level."),
    "leaping": ("#minecraft:enchantable/foot_armor", "#minecraft:enchantable/foot_armor", ["feet"], 2, 2, (10, 15), False,
                {"minecraft:attributes": [
                    {"id": f"{MODID}:enchantment.leaping", "attribute": "minecraft:generic.jump_strength",
                     "amount": linear(0.08, 0.08), "operation": "add_value"},
                    {"id": f"{MODID}:enchantment.leaping_step", "attribute": "minecraft:generic.step_height",
                     "amount": linear(0.5, 0.0), "operation": "add_value"}]}, None,
                "Bond", "Sauts plus hauts et vous montez les marches d'un bloc sans sauter.",
                "Leaping", "Higher jumps, and you walk up one-block steps without jumping."),
    "arcane_mind": ("#minecraft:enchantable/head_armor", "#minecraft:enchantable/head_armor", ["head"], 3, 2, (10, 10), False,
                    None, None,
                    "Esprit arcanique", "+10 de mana maximal et +15 % de régénération de mana par niveau.",
                    "Arcane Mind", "+10 max mana and +15% mana regeneration per level."),
    "rejuvenation": ("#minecraft:enchantable/chest_armor", "#minecraft:enchantable/chest_armor", ["chest"], 3, 2, (15, 10), False,
                     None, None,
                     "Rajeunissement", "Rend ½ cœur par niveau toutes les 5 secondes.",
                     "Rejuvenation", "Restores ½ heart per level every 5 seconds."),
    "evasion": ("#minecraft:enchantable/leg_armor", "#minecraft:enchantable/leg_armor", ["legs"], 3, 2, (10, 10), False,
                None, None,
                "Esquive", "4 % de chances par niveau d'éviter complètement une attaque.",
                "Evasion", "4% chance per level to dodge an attack completely."),
    "night_sight": ("#minecraft:enchantable/head_armor", "#minecraft:enchantable/head_armor", ["head"], 1, 2, (15, 0), False,
                    None, None,
                    "Vision nocturne", "Vision nocturne tant que le casque est porté.",
                    "Night Sight", "Night vision while the helmet is worn."),
    "blazing_thorns": ("#minecraft:enchantable/chest_armor", "#minecraft:enchantable/chest_armor", ["chest"], 2, 2, (10, 15), False,
                       {"minecraft:post_attack": [{
                           "affected": "attacker", "enchanted": "victim", "requirements": direct_hit(),
                           "effect": {"type": "minecraft:ignite", "duration": linear(3.0, 3.0)}}]}, None,
                       "Retour de flamme", "Ceux qui vous frappent au corps à corps prennent feu.",
                       "Blazing Thorns", "Those who hit you in melee catch fire."),
    "second_wind": ("#minecraft:enchantable/chest_armor", "#minecraft:enchantable/chest_armor", ["chest"], 1, 1, (25, 0), True,
                    None, None,
                    "Second souffle", "Une fois toutes les 5 minutes, survivre à un coup mortel (¼ de vie, résistance).",
                    "Second Wind", "Once every 5 minutes, survive a fatal blow (¼ health, resistance)."),
    # ---------------------------------------------------------------- bows
    "explosive_arrows": (RANGED, "#minecraft:enchantable/bow", ["mainhand", "offhand"], 2, 1, (20, 15), False, None, None,
                         "Flèches explosives", "Les flèches explosent à l'impact (sans abîmer le terrain).",
                         "Explosive Arrows", "Arrows explode on impact (without harming the terrain)."),
    "sniper": (RANGED, "#minecraft:enchantable/bow", ["mainhand", "offhand"], 3, 2, (10, 10), False, None, None,
               "Tireur d'élite", "Jusqu'à +25 % de dégâts par niveau selon la distance du tir (maximum à 40 blocs).",
               "Sniper", "Up to +25% damage per level depending on the shot's distance (full at 40 blocks)."),
    # ---------------------------------------------------------------- staffs
    "spell_power": (STAFF, STAFF, ["mainhand"], 5, 5, (1, 10), False, None, None,
                    "Puissance arcanique", "+10 % de puissance des sorts par niveau (sorts du bâton et aptitudes magiques).",
                    "Spell Power", "+10% spell power per level (staff spells and magic skills)."),
    "mana_efficiency": (STAFF, STAFF, ["mainhand"], 3, 3, (5, 10), False, None, None,
                        "Économie de mana", "-10 % de coût en mana par niveau (sorts du bâton et aptitudes magiques).",
                        "Mana Efficiency", "-10% mana cost per level (staff spells and magic skills)."),
    "quick_cast": (STAFF, STAFF, ["mainhand"], 3, 2, (10, 10), False, None, None,
                   "Incantation rapide", "-15 % de recharge du bâton par niveau.",
                   "Quick Cast", "-15% staff cooldown per level."),
    # ---------------------------------------------------------------- anything
    "soulbound": ("#minecraft:enchantable/durability", "#minecraft:enchantable/durability", ["any"], 1, 1, (25, 0), True,
                  None, None,
                  "Lié à l'âme", "L'objet reste avec vous quand vous mourez.",
                  "Soulbound", "The item stays with you when you die."),
}


def generate(write_json, data, lang_fr, lang_en):
    treasure, normal = [], []
    for name, (supported, primary, slots, max_level, weight, (min_base, min_per), is_treasure, effects, exclusive,
               fr, fr_desc, en, en_desc) in ENCHANTMENTS.items():
        enchantment = {
            "description": {"translate": f"enchantment.{MODID}.{name}"},
            "supported_items": supported,
            "primary_items": primary,
            "weight": weight,
            "max_level": max_level,
            "min_cost": {"base": min_base, "per_level_above_first": min_per},
            "max_cost": {"base": min_base + 50, "per_level_above_first": min_per},
            "anvil_cost": max(1, 8 // weight),
            "slots": slots,
        }
        if effects:
            enchantment["effects"] = effects
        if exclusive:
            enchantment["exclusive_set"] = exclusive
        write_json(data / MODID / "enchantment" / f"{name}.json", enchantment)
        (treasure if is_treasure else normal).append(f"{MODID}:{name}")
        lang_fr[f"enchantment.{MODID}.{name}"] = fr
        lang_fr[f"enchantment.{MODID}.{name}.desc"] = fr_desc
        lang_en[f"enchantment.{MODID}.{name}"] = en
        lang_en[f"enchantment.{MODID}.{name}.desc"] = en_desc
    tags = data / "minecraft" / "tags" / "enchantment"
    write_json(tags / "non_treasure.json", {"replace": False, "values": normal})
    write_json(tags / "treasure.json", {"replace": False, "values": treasure})
    write_json(tags / "on_random_loot.json", {"replace": False, "values": normal + treasure})
    write_json(tags / "tradeable.json", {"replace": False, "values": normal + treasure})
    items = data / MODID / "tags" / "item" / "enchantable"
    write_json(items / "staff.json", {"values": [f"{MODID}:{s}" for s in
                                                ("fire_staff", "frost_staff", "healing_staff", "arcane_staff",
                                                 "lightning_staff", "archmage_staff")]})
    write_json(items / "ranged.json", {"values": ["#minecraft:enchantable/bow", "#minecraft:enchantable/crossbow"]})
    write_json(items / "tool_or_weapon.json", {"values": ["#minecraft:enchantable/mining", "#minecraft:enchantable/weapon"]})
    # Autosmelt and Telekinesis on block drops
    write_json(data / MODID / "loot_modifiers" / "enchanted_loot.json", {"type": f"{MODID}:enchanted_loot", "conditions": []})
    write_json(data / "forge" / "loot_modifiers" / "global_loot_modifiers.json",
               {"replace": False, "entries": [f"{MODID}:enchanted_loot"]})
