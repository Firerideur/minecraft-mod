package com.firerideur.arcanequest.skill;

import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.firerideur.arcanequest.skill.Skill.*;

/**
 * Which skills each class has, and when they unlock: a first class gets three skills at levels 20, 30 and 40,
 * its awakening adds two more at levels 50 and 75.
 */
public final class ClassSkills {
    /** Level at which each skill slot unlocks. */
    public static final int[] UNLOCK_LEVELS = {20, 30, 40, 50, 75};
    public static final int SLOTS = UNLOCK_LEVELS.length;

    private static final Map<PlayerClass, List<Skill>> OWN = new EnumMap<>(PlayerClass.class);

    static {
        own(PlayerClass.BERSERKER, WHIRLWIND, WAR_CRY, BLOODLUST);
        own(PlayerClass.WARLORD, EARTHQUAKE, BERSERK);
        own(PlayerClass.EXECUTIONER, EXECUTE, BLADE_DANCE);
        own(PlayerClass.GUARDIAN, TAUNT, SHIELD_WALL, GROUND_SLAM);
        own(PlayerClass.FORTRESS, IRON_SKIN, EARTHQUAKE);
        own(PlayerClass.RUNE_GUARDIAN, MANA_SHIELD, RUNE_BLADE);
        own(PlayerClass.ASSASSIN, SHADOW_STEP, POISON_BLADE, SMOKE_BOMB);
        own(PlayerClass.NINJA, FLURRY, BLINK);
        own(PlayerClass.SHADOW_ASSASSIN, EXECUTE, BLADE_DANCE);
        own(PlayerClass.MAGE, FIREBALL, FROST_NOVA, CHAIN_LIGHTNING);
        own(PlayerClass.ARCHMAGE, ARCANE_BARRAGE, TIME_STOP);
        own(PlayerClass.ELEMENTALIST, METEOR_STRIKE, EARTHQUAKE);
        own(PlayerClass.PALADIN, SMITE, HEAL_AURA, CHARGE);
        own(PlayerClass.CRUSADER, WAR_CRY, WHIRLWIND);
        own(PlayerClass.HOLY_KNIGHT, DIVINE_SHIELD, JUDGMENT);
        own(PlayerClass.DUELIST, CHARGE, PARRY, FLURRY);
        own(PlayerClass.WEAPON_MASTER, BLADE_DANCE, WHIRLWIND);
        own(PlayerClass.WINDBLADE, BLINK, TORNADO);
        own(PlayerClass.RANGER, MULTISHOT, TRAP, PIERCING_SHOT);
        own(PlayerClass.SNIPER, DEADEYE, ARROW_RAIN);
        own(PlayerClass.HUNTER, ARROW_RAIN, SUMMON_WOLVES);
        own(PlayerClass.SPELLBLADE, RUNE_BLADE, CHARGE, FIREBALL);
        own(PlayerClass.RUNE_KNIGHT, WHIRLWIND, MANA_SHIELD);
        own(PlayerClass.WARLOCK, LIFE_DRAIN, CURSE);
        own(PlayerClass.CLERIC, HEAL_AURA, SMITE, MANA_SHIELD);
        own(PlayerClass.SAINT, SANCTUARY, JUDGMENT);
        own(PlayerClass.DRUID, TRAP, SUMMON_WOLVES);
        own(PlayerClass.ARCANIST, BLINK, ARCANE_BARRAGE, MANA_SHIELD);
        own(PlayerClass.CHRONOMANCER, TIME_STOP, REWIND);
        own(PlayerClass.BATTLEMAGE, RUNE_BLADE, METEOR_STRIKE);
        own(PlayerClass.MONK, FLURRY, CHARGE, IRON_SKIN);
        own(PlayerClass.GRANDMASTER, TORNADO, EARTHQUAKE);
        own(PlayerClass.IRON_MONK, TAUNT, SHIELD_WALL);
        own(PlayerClass.NECROMANCER, SUMMON_SHADOWS, LIFE_DRAIN, CURSE);
        own(PlayerClass.LICH_LORD, FROST_NOVA, ARISE);
        // The Shadow Monarch can come from two classes: it has its own full set
        own(PlayerClass.SHADOW_MONARCH, SUMMON_SHADOWS, SHADOW_STEP, SHADOW_EXCHANGE, ARISE, MONARCH_DOMAIN);
    }

    private ClassSkills() {}

    private static void own(PlayerClass playerClass, Skill... skills) {
        OWN.put(playerClass, List.of(skills));
    }

    /** The skills of a class in slot order (unlocked or not): the first class's three, then the awakening's. */
    public static List<Skill> skillsOf(PlayerClass playerClass) {
        List<Skill> own = OWN.getOrDefault(playerClass, List.of());
        if (!playerClass.isAwakened() || own.size() >= SLOTS) {
            return own;
        }
        List<Skill> all = new ArrayList<>(skillsOf(playerClass.firstClass()));
        all.addAll(own);
        return all;
    }

    /** The skill in {@code slot} if the player has unlocked it, else null. */
    public static Skill unlocked(PlayerStats stats, int slot) {
        List<Skill> skills = skillsOf(stats.getPlayerClass());
        if (slot < 0 || slot >= skills.size() || stats.getLevel() < UNLOCK_LEVELS[slot]) {
            return null;
        }
        return skills.get(slot);
    }
}
