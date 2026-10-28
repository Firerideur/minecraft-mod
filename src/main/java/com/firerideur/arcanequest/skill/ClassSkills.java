package com.firerideur.arcanequest.skill;

import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.firerideur.arcanequest.skill.Skill.*;

/**
 * Which skills each class has, and when they unlock (see {@link #UNLOCK_LEVELS}):
 * <ul>
 *   <li>a first class has {@link #FIRST_CLASS_SKILLS} skills, at levels 20, 30, 40, 50 and 75;</li>
 *   <li>an awakened class keeps the first {@link #INHERITED} of them, and replaces the others by its own two skills
 *       (levels 50 and 75) and its ultimate (level 100);</li>
 *   <li>a class with a full set of its own (the Shadow Monarch, who can come from two classes) doesn't inherit.</li>
 * </ul>
 */
public final class ClassSkills {
    /** Level at which each skill slot unlocks. */
    public static final int[] UNLOCK_LEVELS = {20, 30, 40, 50, 75, 100};
    public static final int SLOTS = UNLOCK_LEVELS.length;
    public static final int FIRST_CLASS_SKILLS = 5;
    /** Skills an awakened class keeps from its first class. */
    public static final int INHERITED = 3;
    /** Slot of the ultimate of awakened classes. */
    public static final int ULTIMATE_SLOT = SLOTS - 1;

    private static final Map<PlayerClass, List<Skill>> OWN = new EnumMap<>(PlayerClass.class);

    static {
        // Every line has a spell among its first three skills, so its awakenings keep some magic.
        // Fighter line: Bloodlust (fear) as Sung Jinwoo, Go Gunhee's fists, a headsman's axe
        own(PlayerClass.BERSERKER, WHIRLWIND, WAR_CRY, BLOOD_AURA, BLOODLUST, GROUND_SLAM);
        own(PlayerClass.WARLORD, BLOODLUST, BERSERK, CATACLYSM);
        own(PlayerClass.EXECUTIONER, EXECUTE, REAPING_CLEAVE, DEATH_SENTENCE);
        // Tank line: Thomas Andre's Goliath, runic barriers
        own(PlayerClass.GUARDIAN, TAUNT, SHIELD_BASH, EARTH_SPIKES, SHIELD_WALL, IRON_SKIN);
        own(PlayerClass.FORTRESS, BULWARK, EARTHQUAKE, GOLIATH);
        own(PlayerClass.RUNE_GUARDIAN, MANA_SHIELD, RUNIC_WARD, AEGIS);
        // Assassin line: Sung Jinwoo's dagger skills before his Job Change
        own(PlayerClass.ASSASSIN, MUTILATION, SHADOW_STEP, POISON_BLADE, STEALTH, DAGGER_RUSH);
        own(PlayerClass.NINJA, SMOKE_BOMB, SHURIKEN_STORM, THOUSAND_CUTS);
        own(PlayerClass.SHADOW_ASSASSIN, DAGGER_RUSH, BLADE_DANCE, ASSASSINATION);
        // Mage line: Choi Jong-In's Flame Spear, the Frost Monarch and Baran's white flames
        own(PlayerClass.MAGE, FIREBALL, FROST_NOVA, CHAIN_LIGHTNING, FLAME_SPEAR, METEOR_STRIKE);
        own(PlayerClass.ARCHMAGE, FLAME_SPEAR, ARCANE_BARRAGE, INFERNO);
        own(PlayerClass.ELEMENTALIST, METEOR_STRIKE, GLACIAL_SPIKES, WHITE_FLAMES);
        // Paladin line: the light of the Rulers
        own(PlayerClass.PALADIN, SMITE, HEAL_AURA, CHARGE, JUDGMENT, DIVINE_SHIELD);
        own(PlayerClass.CRUSADER, HOLY_BANNER, WHIRLWIND, CRUSADE);
        own(PlayerClass.HOLY_KNIGHT, DIVINE_SHIELD, SANCTUARY, RULERS_LIGHT);
        // Duelist line: Cha Hae-In's Sword Dance, Liu Zhigang's sword
        own(PlayerClass.DUELIST, CHARGE, PARRY, WIND_GUARD, SWORD_DANCE, FLURRY);
        own(PlayerClass.WEAPON_MASTER, BLADE_DANCE, SWORD_WAVE, HEAVEN_SPLITTER);
        own(PlayerClass.WINDBLADE, SWORD_DANCE, TORNADO, TEMPEST_DANCE);
        // Ranger line: Rakan, Monarch of Fangs, for the hunter
        own(PlayerClass.RANGER, MULTISHOT, TRAP, FROST_ARROW, PIERCING_SHOT, ARROW_RAIN);
        own(PlayerClass.SNIPER, DEADEYE, EXPLOSIVE_ARROW, HEAVENS_ARROW);
        own(PlayerClass.HUNTER, SUMMON_WOLVES, HUNTERS_MARK, RAKANS_HOWL);
        // Spellblade line: Baran's lightning daggers, demonic pacts
        own(PlayerClass.SPELLBLADE, RUNE_BLADE, CHARGE, FIREBALL, ARCANE_SLASH, MANA_SHIELD);
        own(PlayerClass.RUNE_KNIGHT, ARCANE_SLASH, CHAIN_LIGHTNING, THUNDER_BLADE);
        own(PlayerClass.WARLOCK, LIFE_DRAIN, CURSE, DEMONIC_PACT);
        // Healer line: Lee Joohee, Min Byung-Gu, the shaman Tusk
        own(PlayerClass.CLERIC, HEAL_AURA, SMITE, MANA_SHIELD, PURIFY, SANCTUARY);
        own(PlayerClass.SAINT, PURIFY, SANCTUARY, SAVING_GRACE);
        own(PlayerClass.DRUID, SUMMON_WOLVES, TRAP, VERDANT_GROVE);
        // Arcanist line: the Architect's time magic, Esil Radiru's battle magic
        own(PlayerClass.ARCANIST, BLINK, ARCANE_BARRAGE, MANA_SHIELD, GRAVITY_WELL, CHAIN_LIGHTNING);
        own(PlayerClass.CHRONOMANCER, TIME_STOP, REWIND, TEMPORAL_COLLAPSE);
        own(PlayerClass.BATTLEMAGE, RUNE_BLADE, METEOR_STRIKE, ARCANE_AVATAR);
        // Monk line: Baek Yoonho's beast form, the shadow Iron
        own(PlayerClass.MONK, FLURRY, CHARGE, MEDITATION, PALM_STRIKE, TORNADO);
        own(PlayerClass.GRANDMASTER, PALM_STRIKE, EARTHQUAKE, BEAST_FORM);
        own(PlayerClass.IRON_MONK, TAUNT, SHIELD_WALL, ADAMANTINE_BODY);
        // Necromancer line: the class Sung Jinwoo was offered at his Job Change, Yogumunt's plague
        own(PlayerClass.NECROMANCER, SUMMON_SHADOWS, LIFE_DRAIN, CURSE, CORPSE_EXPLOSION, SOUL_HARVEST);
        own(PlayerClass.LICH_LORD, FROST_NOVA, ARISE, DEATH_PLAGUE);
        // The Shadow Monarch can come from two classes: it has its own full set, Sung Jinwoo's
        own(PlayerClass.SHADOW_MONARCH, DAGGER_RUSH, SHADOW_EXCHANGE, ARISE, RULERS_AUTHORITY, DRAGONS_FEAR, MONARCH_DOMAIN);
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
        List<Skill> all = new ArrayList<>(skillsOf(playerClass.firstClass()).subList(0, INHERITED));
        all.addAll(own);
        return all;
    }

    /** The ultimate of a class (its level 100 skill), or null for first classes. */
    public static Skill ultimateOf(PlayerClass playerClass) {
        List<Skill> skills = skillsOf(playerClass);
        return skills.size() > ULTIMATE_SLOT ? skills.get(ULTIMATE_SLOT) : null;
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
