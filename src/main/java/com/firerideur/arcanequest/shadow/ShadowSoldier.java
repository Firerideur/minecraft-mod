package com.firerideur.arcanequest.shadow;

import com.firerideur.arcanequest.boss.ArcaneBoss;
import com.firerideur.arcanequest.registry.ModEntities;
import com.firerideur.arcanequest.skill.SkillRuntime;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A shadow soldier raised from a defeated monster by a Necromancer, Lich Lord or Shadow Monarch.
 * Follows its master, fights what the master fights (or what attacks them), and fades after a while
 * unless raised by the Shadow Monarch.
 */
public class ShadowSoldier extends PathfinderMob {
    private static final double FOLLOW_DISTANCE = 5.0;
    private static final double TELEPORT_DISTANCE = 24.0;
    private static final double GUARD_RADIUS = 12.0;

    /**
     * Shadow ranks, as in Solo Leveling: the stronger the monster a shadow was extracted from, the higher its rank.
     * {@code power} multiplies its health and damage.
     */
    public enum Rank {
        SOLDIER("soldier", 1.0F, 1.0F), ELITE("elite", 1.15F, 1.0F), KNIGHT("knight", 1.35F, 1.05F),
        ELITE_KNIGHT("elite_knight", 1.6F, 1.1F), COMMANDER("commander", 2.0F, 1.2F), MARSHAL("marshal", 2.5F, 1.3F);

        private final String key;
        private final float power;
        private final float scale;

        Rank(String key, float power, float scale) {
            this.key = key;
            this.power = power;
            this.scale = scale;
        }

        public float power() {
            return power;
        }

        public Component displayName() {
            return Component.translatable("shadow.arcanequest.rank." + key);
        }

        /** Bosses become commanders (marshals when very strong), the others by max health. */
        public static Rank of(float maxHealth, boolean boss) {
            if (boss) {
                return maxHealth >= 300 ? MARSHAL : COMMANDER;
            }
            return maxHealth < 25 ? SOLDIER : maxHealth < 50 ? ELITE : maxHealth < 100 ? KNIGHT : ELITE_KNIGHT;
        }

        public static Rank byId(int id) {
            Rank[] values = values();
            return id >= 0 && id < values.length ? values[id] : SOLDIER;
        }
    }

    @Nullable
    private UUID ownerId;
    private Rank rank = Rank.SOLDIER;
    /** Ticks left before fading, or 0 for a permanent shadow. */
    private int lifetime;
    private int ownerMissingTicks;

    public ShadowSoldier(EntityType<? extends ShadowSoldier> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /**
     * Tries to raise a shadow from {@code victim} for {@code owner}, depending on the owner's class.
     *
     * @return the new shadow, or null
     */
    @Nullable
    public static ShadowSoldier tryRaise(ServerPlayer owner, PlayerStats stats, LivingEntity victim) {
        PlayerClass.Bonuses bonuses = stats.getPlayerClass().bonuses();
        // Death Plague: every monster killed meanwhile rises
        float chance = SkillRuntime.buff(owner, SkillRuntime.Buff.PLAGUE) > 0 ? 1.0F : bonuses.shadowChance();
        if (bonuses.shadowChance() <= 0 || !(victim instanceof Enemy) || victim instanceof ArcaneBoss
                || victim instanceof ShadowSoldier || owner.getRandom().nextFloat() >= chance) {
            return null;
        }
        return extract(owner, stats, victim.position(), victim.getDisplayName(), victim.getMaxHealth(),
                Rank.of(victim.getMaxHealth(), false), bonuses.shadowPower(), bonuses.shadowLifetime());
    }

    /**
     * Raises the shadow of a dead monster (Shadow Extraction): its rank and health depend on the monster.
     *
     * @param lifetime ticks before it fades, 0 = permanent
     */
    @Nullable
    public static ShadowSoldier extract(ServerPlayer owner, PlayerStats stats, Vec3 pos, Component name, float maxHealth,
                                        Rank rank, float power, int lifetime) {
        double health = Math.max(10.0, Math.min(80.0 * rank.power, maxHealth * 0.6 * power));
        ShadowSoldier shadow = spawn(owner, stats, pos, health, power, lifetime, rank);
        if (shadow != null) {
            shadow.setCustomName(Component.translatable("entity.arcanequest.shadow_soldier.named", rank.displayName(), name));
        }
        return shadow;
    }

    /**
     * Calls a shadow next to {@code owner} without a body to raise (skills).
     *
     * @param lifetime ticks before it fades, 0 = permanent
     */
    @Nullable
    public static ShadowSoldier summon(ServerPlayer owner, PlayerStats stats, float power, int lifetime) {
        double angle = owner.getRandom().nextDouble() * Math.PI * 2;
        Vec3 pos = owner.position().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
        double health = Math.min(100.0, 20.0 * power * (1.0 + stats.getLevel() * 0.02));
        return spawn(owner, stats, pos, health, power * stats.getPlayerClass().bonuses().shadowPower(), lifetime, Rank.SOLDIER);
    }

    @Nullable
    private static ShadowSoldier spawn(ServerPlayer owner, PlayerStats stats, Vec3 pos, double health, float power, int lifetime,
                                       Rank rank) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        ShadowSoldier shadow = ModEntities.SHADOW_SOLDIER.get().create(level);
        if (shadow == null) {
            return null;
        }
        // Too many shadows: the oldest one fades
        int max = Math.max(1, stats.getPlayerClass().bonuses().shadowMax());
        List<ShadowSoldier> army = armyOf(owner);
        while (army.size() >= max && !army.isEmpty()) {
            army.remove(0).fade();
        }
        shadow.ownerId = owner.getUUID();
        shadow.lifetime = lifetime;
        shadow.rank = rank;
        shadow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        shadow.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((3.0 + stats.getLevel() * 0.1) * power * rank.power);
        shadow.getAttribute(Attributes.SCALE).setBaseValue(rank.scale);
        shadow.setHealth((float) health);
        shadow.moveTo(pos.x, pos.y, pos.z, owner.getYRot(), 0);
        shadow.finalizeSpawn(level, level.getCurrentDifficultyAt(shadow.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(shadow);
        level.sendParticles(ParticleTypes.SOUL, shadow.getX(), shadow.getY() + 0.5, shadow.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        level.sendParticles(ParticleTypes.SQUID_INK, shadow.getX(), shadow.getY() + 0.2, shadow.getZ(), 15, 0.4, 0.1, 0.4, 0.02);
        shadow.playSound(SoundEvents.WITHER_SPAWN, 0.4F, 1.6F);
        return shadow;
    }

    /** The shadows of {@code owner} near them, oldest first. */
    public static List<ShadowSoldier> armyOf(Player owner) {
        return owner.level().getEntitiesOfClass(ShadowSoldier.class, owner.getBoundingBox().inflate(128.0),
                        s -> owner.getUUID().equals(s.ownerId))
                .stream().sorted(Comparator.comparingInt((ShadowSoldier s) -> -s.tickCount)).collect(Collectors.toCollection(ArrayList::new));
    }

    public Rank getRank() {
        return rank;
    }

    @Nullable
    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(2, new FollowOwnerGoal());
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (++ownerMissingTicks > 100) {
                fade();
            }
            return;
        }
        ownerMissingTicks = 0;
        if (lifetime > 0 && --lifetime == 0) {
            fade();
            return;
        }
        if (tickCount % 10 == 0) {
            LivingEntity enemy = pickEnemy(owner);
            if (enemy != null) {
                setTarget(enemy);
            } else if (getTarget() != null && !canFight(getTarget())) {
                setTarget(null);
            }
        }
    }

    /** What the master is fighting, what attacks the master, else the closest monster around them. */
    @Nullable
    private LivingEntity pickEnemy(Player owner) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (canFight(attacker)) {
            return attacker;
        }
        LivingEntity attacked = owner.getLastHurtMob();
        if (canFight(attacked)) {
            return attacked;
        }
        if (canFight(getTarget())) {
            return getTarget();
        }
        return level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(GUARD_RADIUS),
                        e -> e instanceof Enemy && canFight(e))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private boolean canFight(@Nullable LivingEntity entity) {
        return entity != null && entity.isAlive() && !(entity instanceof Player) && !(entity instanceof ShadowSoldier)
                && entity.distanceToSqr(this) < 32 * 32;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof ShadowSoldier shadow && ownerId != null && ownerId.equals(shadow.ownerId)) {
            return true;
        }
        return other.getUUID().equals(ownerId) || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Player) && !(target instanceof ShadowSoldier) && super.canAttack(target);
    }

    /** Vanishes in a puff of shadow. */
    public void fade() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("Lifetime", lifetime);
        tag.putInt("Rank", rank.ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        lifetime = tag.getInt("Lifetime");
        rank = Rank.byId(tag.getInt("Rank"));
    }

    /** Walks back to the master when too far, teleports when very far. */
    private class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player owner = getOwner();
            return owner != null && getTarget() == null && distanceToSqr(owner) > FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }

        @Override
        public void tick() {
            Player owner = getOwner();
            if (owner == null) {
                return;
            }
            getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
            if (distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                moveTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), getXRot());
                getNavigation().stop();
            } else if (tickCount % 10 == 0) {
                getNavigation().moveTo(owner, 1.2);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
