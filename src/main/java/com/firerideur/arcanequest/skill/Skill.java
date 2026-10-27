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
    // ---------------------------------------------------------------- warrior skills
    CHARGE("charge", () -> Items.IRON_HORSE_ARMOR, 8, 10, false, SkillActions::charge),
    WHIRLWIND("whirlwind", () -> Items.IRON_SWORD, 10, 15, false, SkillActions::whirlwind),
    GROUND_SLAM("ground_slam", () -> Items.MACE, 12, 15, false, SkillActions::groundSlam),
    EARTHQUAKE("earthquake", () -> Items.CRACKED_STONE_BRICKS, 25, 30, false, SkillActions::earthquake),
    WAR_CRY("war_cry", () -> Items.GOAT_HORN, 30, 20, false, SkillActions::warCry),
    BLOODLUST("bloodlust", () -> Items.REDSTONE, 25, 15, false, SkillActions::bloodlust),
    BERSERK("berserk", () -> Items.BLAZE_POWDER, 45, 25, false, SkillActions::berserk),
    EXECUTE("execute", () -> Items.NETHERITE_AXE, 8, 15, false, SkillActions::execute),
    BLADE_DANCE("blade_dance", () -> Items.DIAMOND_SWORD, 25, 30, false, SkillActions::bladeDance),
    TAUNT("taunt", () -> Items.BELL, 20, 10, false, SkillActions::taunt),
    SHIELD_WALL("shield_wall", () -> Items.SHIELD, 25, 15, false, SkillActions::shieldWall),
    IRON_SKIN("iron_skin", () -> Items.IRON_CHESTPLATE, 30, 20, false, SkillActions::ironSkin),
    PARRY("parry", () -> Items.GOLDEN_SWORD, 12, 10, false, SkillActions::parry),
    FLURRY("flurry", () -> Items.BLAZE_ROD, 9, 15, false, SkillActions::flurry),
    TORNADO("tornado", () -> Items.WIND_CHARGE, 15, 20, false, SkillActions::tornado),

    // ---------------------------------------------------------------- rogue skills
    SHADOW_STEP("shadow_step", () -> Items.ENDER_PEARL, 10, 15, false, SkillActions::shadowStep),
    SMOKE_BOMB("smoke_bomb", () -> Items.GUNPOWDER, 20, 15, false, SkillActions::smokeBomb),
    POISON_BLADE("poison_blade", () -> Items.SPIDER_EYE, 20, 10, false, SkillActions::poisonBlade),

    // ---------------------------------------------------------------- archer skills
    MULTISHOT("multishot", () -> Items.ARROW, 6, 10, false, SkillActions::multishot),
    ARROW_RAIN("arrow_rain", () -> Items.SPECTRAL_ARROW, 15, 25, false, SkillActions::arrowRain),
    PIERCING_SHOT("piercing_shot", () -> Items.TIPPED_ARROW, 8, 15, false, SkillActions::piercingShot),
    TRAP("trap", () -> Items.COBWEB, 15, 15, false, SkillActions::trap),
    DEADEYE("deadeye", () -> Items.SPYGLASS, 30, 20, false, SkillActions::deadeye),
    SUMMON_WOLVES("summon_wolves", () -> Items.BONE, 60, 30, false, SkillActions::summonWolves),

    // ---------------------------------------------------------------- spells
    FIREBALL("fireball", () -> Items.FIRE_CHARGE, 4, 15, true, SkillActions::fireball),
    FROST_NOVA("frost_nova", () -> Items.BLUE_ICE, 12, 20, true, SkillActions::frostNova),
    CHAIN_LIGHTNING("chain_lightning", () -> Items.LIGHTNING_ROD, 8, 20, true, SkillActions::chainLightning),
    METEOR_STRIKE("meteor_strike", () -> Items.MAGMA_BLOCK, 20, 40, true, SkillActions::meteorStrike),
    ARCANE_BARRAGE("arcane_barrage", () -> Items.AMETHYST_SHARD, 6, 25, true, SkillActions::arcaneBarrage),
    BLINK("blink", () -> Items.ENDER_EYE, 6, 10, true, SkillActions::blink),
    TIME_STOP("time_stop", () -> Items.CLOCK, 40, 50, true, SkillActions::timeStop),
    REWIND("rewind", () -> Items.RECOVERY_COMPASS, 90, 40, true, SkillActions::rewind),
    MANA_SHIELD("mana_shield", () -> Items.PRISMARINE_CRYSTALS, 30, 20, true, SkillActions::manaShield),
    LIFE_DRAIN("life_drain", () -> Items.FERMENTED_SPIDER_EYE, 8, 20, true, SkillActions::lifeDrain),
    CURSE("curse", () -> Items.WITHER_ROSE, 20, 25, true, SkillActions::curse),
    RUNE_BLADE("rune_blade", () -> Items.ENCHANTED_BOOK, 25, 20, true, SkillActions::runeBlade),

    // ---------------------------------------------------------------- holy skills
    HEAL_AURA("heal_aura", () -> Items.GLISTERING_MELON_SLICE, 15, 25, true, SkillActions::healAura),
    SMITE("smite", () -> Items.GOLDEN_AXE, 8, 15, true, SkillActions::smite),
    JUDGMENT("judgment", () -> Items.BEACON, 30, 40, true, SkillActions::judgment),
    DIVINE_SHIELD("divine_shield", () -> Items.TOTEM_OF_UNDYING, 60, 30, true, SkillActions::divineShield),
    SANCTUARY("sanctuary", () -> Items.GOLDEN_APPLE, 45, 40, true, SkillActions::sanctuary),

    // ---------------------------------------------------------------- shadows
    SUMMON_SHADOWS("summon_shadows", () -> Items.SCULK, 30, 30, true, SkillActions::summonShadows),
    ARISE("arise", () -> Items.WITHER_SKELETON_SKULL, 60, 50, true, SkillActions::arise),
    SHADOW_EXCHANGE("shadow_exchange", () -> Items.CHORUS_FRUIT, 10, 15, true, SkillActions::shadowExchange),
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
