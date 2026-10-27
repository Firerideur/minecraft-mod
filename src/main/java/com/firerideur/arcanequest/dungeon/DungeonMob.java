package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Monsters that live behind the gates. They don't spawn naturally; the dungeon places them with a level
 * that depends on the gate's stars. One of them guards the last room as the gate boss.
 */
public abstract class DungeonMob extends Monster {
    private static final ResourceLocation BOSS_HEALTH = ArcaneQuest.id("gate_boss_health");
    private static final ResourceLocation BOSS_DAMAGE = ArcaneQuest.id("gate_boss_damage");
    private static final ResourceLocation BOSS_SCALE = ArcaneQuest.id("gate_boss_scale");
    private static final ResourceLocation BOSS_KNOCKBACK = ArcaneQuest.id("gate_boss_knockback");

    /** Dungeon this mob belongs to, -1 if it was summoned elsewhere. */
    private int dungeonId = -1;
    private boolean gateBoss;
    private ServerBossEvent bossEvent;

    protected DungeonMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /** Usual melee behavior; subclasses add their attack goal at priority 2. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setDungeonId(int dungeonId) {
        this.dungeonId = dungeonId;
    }

    public int getDungeonId() {
        return dungeonId;
    }

    public boolean isGateBoss() {
        return gateBoss;
    }

    /** Turns this mob into the boss of a gate: much tougher, bigger, with a boss bar. */
    public void makeGateBoss(int stars) {
        gateBoss = true;
        double health = 8.0 + stars;
        getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(
                new AttributeModifier(BOSS_HEALTH, health, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(
                new AttributeModifier(BOSS_DAMAGE, 0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        getAttribute(Attributes.SCALE).addOrReplacePermanentModifier(
                new AttributeModifier(BOSS_SCALE, 0.7, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).addOrReplacePermanentModifier(
                new AttributeModifier(BOSS_KNOCKBACK, 0.8, AttributeModifier.Operation.ADD_VALUE));
        setHealth(getMaxHealth());
        setCustomName(Component.translatable("entity.arcanequest.gate_boss", getType().getDescription())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        createBossEvent();
    }

    private void createBossEvent() {
        if (bossEvent == null) {
            bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (bossEvent != null) {
            bossEvent.setProgress(getHealth() / getMaxHealth());
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bossEvent != null) {
            bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossEvent != null) {
            bossEvent.removePlayer(player);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (gateBoss && level() instanceof ServerLevel level && dungeonId >= 0) {
            Gates.onBossDefeated(level, dungeonId, blockPosition());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Dungeon", dungeonId);
        tag.putBoolean("GateBoss", gateBoss);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dungeonId = tag.contains("Dungeon") ? tag.getInt("Dungeon") : -1;
        gateBoss = tag.getBoolean("GateBoss");
        if (gateBoss) {
            createBossEvent();
            bossEvent.setName(getDisplayName());
        }
    }
}
