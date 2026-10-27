package com.firerideur.arcanequest.mobs;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.boss.ArcaneBoss;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * Hostile mobs get a level when they first spawn, based on where they spawn:
 * further from world spawn, deeper underground, and in the Nether or End means stronger.
 * A small fraction become Champions (mini-bosses).
 */
public final class MobLevels {
    public static final int MAX_LEVEL = 100;
    public static final float ELITE_CHANCE = 0.03F;
    public static final int ELITE_MIN_LEVEL = 3;

    private static final String LEVEL_TAG = "arcanequest_level";
    private static final String ELITE_TAG = "arcanequest_elite";

    private static final ResourceLocation LEVEL_HEALTH = ArcaneQuest.id("level_health");
    private static final ResourceLocation LEVEL_DAMAGE = ArcaneQuest.id("level_damage");
    private static final ResourceLocation LEVEL_ARMOR = ArcaneQuest.id("level_armor");
    private static final ResourceLocation ELITE_HEALTH = ArcaneQuest.id("elite_health");
    private static final ResourceLocation ELITE_DAMAGE = ArcaneQuest.id("elite_damage");
    private static final ResourceLocation ELITE_SPEED = ArcaneQuest.id("elite_speed");
    private static final ResourceLocation ELITE_SCALE = ArcaneQuest.id("elite_scale");
    private static final ResourceLocation ELITE_KNOCKBACK = ArcaneQuest.id("elite_knockback");

    private MobLevels() {}

    /** Mobs that get a random level. Our bosses have their own fixed level; vanilla bosses are left alone. */
    public static boolean isLeveled(Entity entity) {
        return entity instanceof Mob && entity instanceof Enemy && !(entity instanceof ArcaneBoss)
                && !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** Level of a mob, or 0 if it has none. */
    public static int getLevel(Entity entity) {
        if (entity instanceof ArcaneBoss boss) {
            return boss.getBossLevel();
        }
        return entity.getPersistentData().getInt(LEVEL_TAG);
    }

    public static boolean isElite(Entity entity) {
        return entity.getPersistentData().getBoolean(ELITE_TAG);
    }

    /** Level for a mob spawning at {@code pos}. */
    public static int computeLevel(Level level, BlockPos pos, RandomSource random) {
        BlockPos spawn = level.getSharedSpawnPos();
        double distance = Math.sqrt(Mth.square(pos.getX() - spawn.getX()) + Mth.square(pos.getZ() - spawn.getZ()));
        double value = 1 + distance / 150.0;
        if (pos.getY() < 0) {
            value += -pos.getY() / 16.0;
        }
        if (level.dimension() == Level.NETHER) {
            value += 20;
        } else if (level.dimension() == Level.END) {
            value += 35;
        }
        value += random.nextInt(5) - 2;
        return Mth.clamp((int) value, 1, MAX_LEVEL);
    }

    /** Gives a level to a freshly spawned mob (does nothing if it already has one). */
    public static void assign(Mob mob, ServerLevel level) {
        CompoundTag data = mob.getPersistentData();
        if (data.contains(LEVEL_TAG)) {
            return;
        }
        if (isChampionName(mob.getCustomName())) {
            // Slimes split from a Champion copy its name, but they are not Champions themselves.
            mob.setCustomName(null);
        }
        int mobLevel = computeLevel(level, mob.blockPosition(), mob.getRandom());
        boolean elite = mobLevel >= ELITE_MIN_LEVEL && mob.getRandom().nextFloat() < ELITE_CHANCE;
        setLevel(mob, mobLevel, elite);
    }

    public static void setLevel(Mob mob, int mobLevel, boolean elite) {
        CompoundTag data = mob.getPersistentData();
        data.putInt(LEVEL_TAG, mobLevel);
        data.putBoolean(ELITE_TAG, elite);

        int bonusLevels = mobLevel - 1;
        set(mob, Attributes.MAX_HEALTH, LEVEL_HEALTH, 0.08 * bonusLevels, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(mob, Attributes.ATTACK_DAMAGE, LEVEL_DAMAGE, 0.05 * bonusLevels, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(mob, Attributes.ARMOR, LEVEL_ARMOR, 0.1 * bonusLevels, AttributeModifier.Operation.ADD_VALUE);

        set(mob, Attributes.MAX_HEALTH, ELITE_HEALTH, elite ? 2.0 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(mob, Attributes.ATTACK_DAMAGE, ELITE_DAMAGE, elite ? 0.5 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(mob, Attributes.MOVEMENT_SPEED, ELITE_SPEED, elite ? 0.15 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(mob, Attributes.SCALE, ELITE_SCALE, elite ? 0.25 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, ELITE_KNOCKBACK, elite ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(mob.getMaxHealth());

        if (elite) {
            // Champions are rare, so keeping them around is fine (and expected for a mini-boss).
            mob.setPersistenceRequired();
            mob.setCustomName(Component.translatable("entity.arcanequest.champion", mob.getType().getDescription())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    private static boolean isChampionName(@Nullable Component name) {
        return name != null && name.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("entity.arcanequest.champion");
    }

    /** Experience given for killing {@code victim}. */
    public static long xpForKill(LivingEntity victim) {
        if (victim instanceof ArcaneBoss boss) {
            return boss.getXpReward();
        }
        long base = Math.max(1, Math.round(victim.getMaxHealth() / 2.0));
        int mobLevel = getLevel(victim);
        if (mobLevel > 1) {
            // Max health already grows with level, so this stays moderate.
            base = Math.round(base * (1.0 + 0.05 * (mobLevel - 1)));
        }
        return isElite(victim) ? base * 3 : base;
    }

    private static void set(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
