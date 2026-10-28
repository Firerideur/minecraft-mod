package com.firerideur.arcanequest.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * Active skills. Each class unlocks some of them as it levels up (see {@link ClassSkills});
 * the effects are in {@link SkillActions}.
 */
public enum Skill {
    // Physical skills cost at most 20 mana: classes without Intelligence only have 20.
    // Ultimates (slot 6 of awakened classes, level 100): long cooldown, big effect.

    // ---------------------------------------------------------------- warrior skills
    CHARGE("charge", () -> Items.IRON_HORSE_ARMOR, 8, 10, false, SkillActions::charge),
    WHIRLWIND("whirlwind", () -> Items.IRON_SWORD, 10, 15, false, SkillActions::whirlwind),
    GROUND_SLAM("ground_slam", () -> Items.MACE, 12, 15, false, SkillActions::groundSlam),
    EARTHQUAKE("earthquake", () -> Items.CRACKED_STONE_BRICKS, 25, 20, false, SkillActions::earthquake),
    WAR_CRY("war_cry", () -> Items.GOAT_HORN, 30, 20, false, SkillActions::warCry),
    BLOODLUST("bloodlust", () -> Items.NETHER_WART, 25, 15, false, SkillActions::bloodlust),
    BLOOD_FRENZY("blood_frenzy", () -> Items.REDSTONE, 25, 15, false, SkillActions::bloodFrenzy),
    BERSERK("berserk", () -> Items.BLAZE_POWDER, 45, 20, false, SkillActions::berserk),
    EXECUTE("execute", () -> Items.NETHERITE_AXE, 8, 15, false, SkillActions::execute),
    REAPING_CLEAVE("reaping_cleave", () -> Items.NETHERITE_HOE, 12, 15, false, SkillActions::reapingCleave),
    BLADE_DANCE("blade_dance", () -> Items.DIAMOND_SWORD, 25, 20, false, SkillActions::bladeDance),
    TAUNT("taunt", () -> Items.BELL, 20, 10, false, SkillActions::taunt),
    SHIELD_BASH("shield_bash", () -> Items.IRON_DOOR, 8, 10, false, SkillActions::shieldBash),
    SHIELD_WALL("shield_wall", () -> Items.SHIELD, 25, 15, false, SkillActions::shieldWall),
    IRON_SKIN("iron_skin", () -> Items.IRON_CHESTPLATE, 30, 20, false, SkillActions::ironSkin),
    BULWARK("bulwark", () -> Items.IRON_BLOCK, 30, 20, false, SkillActions::bulwark),
    PARRY("parry", () -> Items.GOLDEN_SWORD, 12, 10, false, SkillActions::parry),
    FLURRY("flurry", () -> Items.RABBIT_FOOT, 9, 15, false, SkillActions::flurry),
    TORNADO("tornado", () -> Items.WIND_CHARGE, 15, 20, false, SkillActions::tornado),
    SWORD_DANCE("sword_dance", () -> Items.PINK_PETALS, 14, 15, false, SkillActions::swordDance),
    SWORD_WAVE("sword_wave", () -> Items.QUARTZ, 10, 15, false, SkillActions::swordWave),
    PALM_STRIKE("palm_strike", () -> Items.MAGMA_CREAM, 10, 10, false, SkillActions::palmStrike),
    HOLY_BANNER("holy_banner", () -> Items.WHITE_BANNER, 35, 20, false, SkillActions::holyBanner),

    // ---------------------------------------------------------------- rogue skills
    MUTILATION("mutilation", () -> Items.SHEARS, 8, 10, false, SkillActions::mutilation),
    SHADOW_STEP("shadow_step", () -> Items.ENDER_PEARL, 10, 15, true, SkillActions::shadowStep),
    STEALTH("stealth", () -> Items.PHANTOM_MEMBRANE, 20, 15, false, SkillActions::stealth),
    DAGGER_RUSH("dagger_rush", () -> Items.POINTED_DRIPSTONE, 12, 15, false, SkillActions::daggerRush),
    SMOKE_BOMB("smoke_bomb", () -> Items.GUNPOWDER, 20, 15, false, SkillActions::smokeBomb),
    POISON_BLADE("poison_blade", () -> Items.SPIDER_EYE, 20, 10, false, SkillActions::poisonBlade),
    SHURIKEN_STORM("shuriken_storm", () -> Items.FLINT, 10, 10, false, SkillActions::shurikenStorm),

    // ---------------------------------------------------------------- archer skills
    MULTISHOT("multishot", () -> Items.ARROW, 6, 10, false, SkillActions::multishot),
    ARROW_RAIN("arrow_rain", () -> Items.SPECTRAL_ARROW, 15, 20, false, SkillActions::arrowRain),
    PIERCING_SHOT("piercing_shot", () -> Items.TIPPED_ARROW, 8, 15, false, SkillActions::piercingShot),
    EVASIVE_SHOT("evasive_shot", () -> Items.FEATHER, 8, 10, false, SkillActions::evasiveShot),
    EXPLOSIVE_ARROW("explosive_arrow", () -> Items.TNT, 12, 15, false, SkillActions::explosiveArrow),
    TRAP("trap", () -> Items.COBWEB, 15, 15, false, SkillActions::trap),
    DEADEYE("deadeye", () -> Items.SPYGLASS, 30, 20, false, SkillActions::deadeye),
    HUNTERS_MARK("hunters_mark", () -> Items.TARGET, 15, 10, false, SkillActions::huntersMark),
    SUMMON_WOLVES("summon_wolves", () -> Items.BONE, 60, 20, false, SkillActions::summonWolves),

    // ---------------------------------------------------------------- spells
    FIREBALL("fireball", () -> Items.FIRE_CHARGE, 4, 15, true, SkillActions::fireball),
    FLAME_SPEAR("flame_spear", () -> Items.BLAZE_ROD, 10, 25, true, SkillActions::flameSpear),
    FROST_NOVA("frost_nova", () -> Items.BLUE_ICE, 12, 20, true, SkillActions::frostNova),
    GLACIAL_SPIKES("glacial_spikes", () -> Items.PACKED_ICE, 14, 30, true, SkillActions::glacialSpikes),
    CHAIN_LIGHTNING("chain_lightning", () -> Items.LIGHTNING_ROD, 8, 20, true, SkillActions::chainLightning),
    METEOR_STRIKE("meteor_strike", () -> Items.MAGMA_BLOCK, 20, 40, true, SkillActions::meteorStrike),
    ARCANE_BARRAGE("arcane_barrage", () -> Items.AMETHYST_SHARD, 6, 25, true, SkillActions::arcaneBarrage),
    ARCANE_SLASH("arcane_slash", () -> Items.PRISMARINE_SHARD, 8, 15, true, SkillActions::arcaneSlash),
    GRAVITY_WELL("gravity_well", () -> Items.OBSIDIAN, 18, 30, true, SkillActions::gravityWell),
    BLINK("blink", () -> Items.ENDER_EYE, 6, 10, true, SkillActions::blink),
    TIME_STOP("time_stop", () -> Items.CLOCK, 40, 50, true, SkillActions::timeStop),
    REWIND("rewind", () -> Items.RECOVERY_COMPASS, 90, 40, true, SkillActions::rewind),
    MANA_SHIELD("mana_shield", () -> Items.PRISMARINE_CRYSTALS, 30, 20, true, SkillActions::manaShield),
    RUNIC_WARD("runic_ward", () -> Items.ENCHANTING_TABLE, 25, 20, true, SkillActions::runicWard),
    LIFE_DRAIN("life_drain", () -> Items.FERMENTED_SPIDER_EYE, 8, 20, true, SkillActions::lifeDrain),
    CURSE("curse", () -> Items.WITHER_ROSE, 20, 25, true, SkillActions::curse),
    SOUL_HARVEST("soul_harvest", () -> Items.SOUL_LANTERN, 20, 20, true, SkillActions::soulHarvest),
    RUNE_BLADE("rune_blade", () -> Items.ENCHANTED_BOOK, 25, 20, true, SkillActions::runeBlade),

    // ---------------------------------------------------------------- holy skills
    HEAL_AURA("heal_aura", () -> Items.GLISTERING_MELON_SLICE, 15, 25, true, SkillActions::healAura),
    PURIFY("purify", () -> Items.MILK_BUCKET, 20, 25, true, SkillActions::purify),
    SMITE("smite", () -> Items.GOLDEN_AXE, 8, 15, true, SkillActions::smite),
    JUDGMENT("judgment", () -> Items.BEACON, 30, 40, true, SkillActions::judgment),
    DIVINE_SHIELD("divine_shield", () -> Items.TOTEM_OF_UNDYING, 60, 30, true, SkillActions::divineShield),
    SANCTUARY("sanctuary", () -> Items.GOLDEN_APPLE, 45, 40, true, SkillActions::sanctuary),

    // ---------------------------------------------------------------- shadows
    SUMMON_SHADOWS("summon_shadows", () -> Items.SCULK, 30, 30, true, SkillActions::summonShadows),
    CORPSE_EXPLOSION("corpse_explosion", () -> Items.FIREWORK_STAR, 10, 20, true, SkillActions::corpseExplosion),
    ARISE("arise", () -> Items.WITHER_SKELETON_SKULL, 20, 40, true, SkillActions::arise),
    SHADOW_EXCHANGE("shadow_exchange", () -> Items.CHORUS_FRUIT, 10, 15, true, SkillActions::shadowExchange),
    RULERS_AUTHORITY("rulers_authority", () -> Items.SHULKER_SHELL, 12, 20, true, SkillActions::rulersAuthority),
    DRAGONS_FEAR("dragons_fear", () -> Items.DRAGON_HEAD, 40, 30, true, SkillActions::dragonsFear),

    // ---------------------------------------------------------------- spells of the martial classes
    // Every hunter in Solo Leveling wields some mana: each line has one spell among its first three skills,
    // cheap enough (at most the base mana) for a class without Intelligence.
    BLOOD_AURA("blood_aura", () -> Items.CRIMSON_ROOTS, 18, 15, true, SkillActions::bloodAura),
    EARTH_SPIKES("earth_spikes", () -> Items.DRIPSTONE_BLOCK, 10, 12, true, SkillActions::earthSpikes),
    WIND_GUARD("wind_guard", () -> Items.FEATHER, 16, 12, true, SkillActions::windGuard),
    FROST_ARROW("frost_arrow", () -> Items.SNOWBALL, 8, 10, true, SkillActions::frostArrow),
    MEDITATION("meditation", () -> Items.LILY_OF_THE_VALLEY, 25, 10, true, SkillActions::meditation),

    // ---------------------------------------------------------------- ultimates (level 100)
    CATACLYSM("cataclysm", () -> Items.HEAVY_CORE, 90, 20, false, SkillActions::cataclysm),
    DEATH_SENTENCE("death_sentence", () -> Items.SKELETON_SKULL, 100, 20, false, SkillActions::deathSentence),
    GOLIATH("goliath", () -> Items.NETHERITE_CHESTPLATE, 120, 20, false, SkillActions::goliath),
    AEGIS("aegis", () -> Items.END_CRYSTAL, 90, 20, false, SkillActions::aegis),
    THOUSAND_CUTS("thousand_cuts", () -> Items.ECHO_SHARD, 90, 20, false, SkillActions::thousandCuts),
    ASSASSINATION("assassination", () -> Items.INK_SAC, 80, 20, false, SkillActions::assassination),
    INFERNO("inferno", () -> Items.LAVA_BUCKET, 100, 80, true, SkillActions::inferno),
    WHITE_FLAMES("white_flames", () -> Items.TRIDENT, 100, 80, true, SkillActions::whiteFlames),
    CRUSADE("crusade", () -> Items.GOLDEN_HELMET, 100, 20, false, SkillActions::crusade),
    RULERS_LIGHT("rulers_light", () -> Items.GLOWSTONE, 100, 20, false, SkillActions::rulersLight),
    HEAVEN_SPLITTER("heaven_splitter", () -> Items.NETHERITE_SWORD, 90, 20, false, SkillActions::heavenSplitter),
    TEMPEST_DANCE("tempest_dance", () -> Items.BREEZE_ROD, 90, 20, false, SkillActions::tempestDance),
    HEAVENS_ARROW("heavens_arrow", () -> Items.END_ROD, 90, 20, false, SkillActions::heavensArrow),
    RAKANS_HOWL("rakans_howl", () -> Items.WOLF_ARMOR, 100, 20, false, SkillActions::rakansHowl),
    THUNDER_BLADE("thunder_blade", () -> Items.COPPER_INGOT, 90, 60, true, SkillActions::thunderBlade),
    DEMONIC_PACT("demonic_pact", () -> Items.CRYING_OBSIDIAN, 90, 40, true, SkillActions::demonicPact),
    SAVING_GRACE("saving_grace", () -> Items.ENCHANTED_GOLDEN_APPLE, 120, 80, true, SkillActions::savingGrace),
    VERDANT_GROVE("verdant_grove", () -> Items.FLOWERING_AZALEA, 100, 70, true, SkillActions::verdantGrove),
    TEMPORAL_COLLAPSE("temporal_collapse", () -> Items.COMPASS, 120, 90, true, SkillActions::temporalCollapse),
    ARCANE_AVATAR("arcane_avatar", () -> Items.AMETHYST_CLUSTER, 100, 60, true, SkillActions::arcaneAvatar),
    BEAST_FORM("beast_form", () -> Items.RABBIT_HIDE, 120, 20, false, SkillActions::beastForm),
    ADAMANTINE_BODY("adamantine_body", () -> Items.ANVIL, 100, 20, false, SkillActions::adamantineBody),
    DEATH_PLAGUE("death_plague", () -> Items.DRAGON_BREATH, 100, 70, true, SkillActions::deathPlague),
    MONARCH_DOMAIN("monarch_domain", () -> Items.NETHER_STAR, 90, 60, true, SkillActions::monarchDomain);

    private final String key;
    private final Supplier<Item> icon;
    private final int cooldownTicks;
    private final float manaCost;
    private final boolean magic;
    private final Action action;

    Skill(String key, Supplier<Item> icon, int cooldownSeconds, float manaCost, boolean magic, Action action) {
        this.key = key;
        this.icon = icon;
        this.cooldownTicks = cooldownSeconds * 20;
        this.manaCost = manaCost;
        this.magic = magic;
        this.action = action;
    }

    public String key() {
        return key;
    }

    public Item icon() {
        return icon.get();
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public float manaCost() {
        return manaCost;
    }

    /** Magic skills scale with spell power, the others with strength. */
    public boolean isMagic() {
        return magic;
    }

    boolean run(SkillContext context) {
        return action.run(context);
    }

    public Component displayName() {
        return Component.translatable("skill.arcanequest." + key);
    }

    public Component description() {
        return Component.translatable("skill.arcanequest." + key + ".desc");
    }

    public static Skill byId(int id) {
        Skill[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }

    @FunctionalInterface
    interface Action {
        /** @return false if the skill could not be used (no target...): no cooldown or mana is spent then */
        boolean run(SkillContext context);
    }
}
