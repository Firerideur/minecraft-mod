package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

    private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(DungeonMob.class, EntityDataSerializers.BOOLEAN);

    /** Dungeon this mob belongs to, -1 if it was summoned elsewhere. */
    private int dungeonId = -1;
    private boolean gateBoss;
    /** Counts toward the dungeon's kill objective (not mimics hidden as chests, nor the boss's guards). */
    private boolean required;
    private ServerBossEvent bossEvent;

    protected DungeonMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DORMANT, false);
    }

    // ---------------------------------------------------------------- dormant (statues, mimics)

    /** A dormant mob doesn't move at all (a statue, a chest...) until a player comes close or hits it. */
    public boolean isDormant() {
        return entityData.get(DATA_DORMANT);
    }

    public void setDormant(boolean dormant) {
        entityData.set(DATA_DORMANT, dormant);
    }

    /** Players closer than this wake the mob up; 0 for mobs that are never dormant. */
    protected double wakeRadius() {
        return 0;
    }

    /** Damage taken while dormant (stone skin...). */
    protected float dormantDamageFactor() {
        return 1.0F;
    }

    /** Effects when waking up. */
    protected void onWake() {
    }

    private void wake() {
        setDormant(false);
        onWake();
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isDormant();
    }

    @Override
    public boolean isPushable() {
        return !isDormant() && super.isPushable();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && isDormant() && tickCount % 5 == 0) {
            Player near = level().getNearestPlayer(this, wakeRadius());
            if (near != null && !near.isSpectator() && !near.isCreative()) {
                wake();
                setTarget(near);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isDormant() && !level().isClientSide() && source.getEntity() != null) {
            float reduced = amount * dormantDamageFactor();
            wake();
            return super.hurt(source, reduced);
        }
        return super.hurt(source, amount);
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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isGateBoss() {
        return gateBoss;
    }

    /** Turns this mob into the boss of a gate: much tougher, bigger, with a boss bar. */
    public void makeGateBoss(int stars) {
        gateBoss = true;
        setDormant(false);
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
            // A gate boss waits behind its seal (see customServerAiStep)
            bossEvent.setVisible(dungeonId < 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (bossEvent != null) {
            bossEvent.setProgress(getHealth() / getMaxHealth());
            if (tickCount % 20 == 0 && level() instanceof ServerLevel level) {
                // Hidden behind its seal: the bar shows once the arena opens
                bossEvent.setVisible(!Gates.isSealed(level, dungeonId));
            }
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
        if (level() instanceof ServerLevel level && dungeonId >= 0) {
            if (gateBoss) {
                Gates.onBossDefeated(level, dungeonId, blockPosition());
            } else if (required) {
                Gates.onMonsterKilled(level, dungeonId);
            }
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
        tag.putBoolean("Required", required);
        tag.putBoolean("Dormant", isDormant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dungeonId = tag.contains("Dungeon") ? tag.getInt("Dungeon") : -1;
        gateBoss = tag.getBoolean("GateBoss");
        required = tag.getBoolean("Required");
        setDormant(tag.getBoolean("Dormant"));
        if (gateBoss) {
            createBossEvent();
            bossEvent.setName(getDisplayName());
        }
    }
}
