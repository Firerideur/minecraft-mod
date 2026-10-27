package com.firerideur.arcanequest.dungeon;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * A gate: a swirling portal ranked from 1 to 7 stars. Walking into it leads to its dungeon.
 * Exit gates (inside dungeons) lead back to where the player came from.
 */
public class GateEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_STARS = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EXIT = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BOOLEAN);

    /** Gates stay open for one Minecraft day. */
    public static final int LIFETIME = 24000;
    /** Once its dungeon is cleared, a gate closes after a minute. */
    private static final int CLOSE_AFTER_CLEAR = 1200;

    private int dungeonId = -1;
    /** Ticks before closing, -1 = never (exit gates). */
    private int lifetime = LIFETIME;

    public GateEntity(EntityType<? extends GateEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STARS, 1);
        builder.define(DATA_EXIT, false);
    }

    public int getStars() {
        return entityData.get(DATA_STARS);
    }

    public boolean isExit() {
        return entityData.get(DATA_EXIT);
    }

    public int getDungeonId() {
        return dungeonId;
    }

    void setup(int dungeonId, int stars, boolean exit) {
        this.dungeonId = dungeonId;
        entityData.set(DATA_STARS, stars);
        entityData.set(DATA_EXIT, exit);
        lifetime = exit ? -1 : LIFETIME;
        setCustomName(Gates.title(stars, exit));
        setCustomNameVisible(true);
    }

    /** Color of a gate of this rank: blue for the weakest, purple for the strongest. */
    public static int color(int stars) {
        return switch (stars) {
            case 1 -> 0x6FB7FF;
            case 2 -> 0x50E0C0;
            case 3 -> 0x60E060;
            case 4 -> 0xF0E050;
            case 5 -> 0xFF9930;
            case 6 -> 0xFF4040;
            default -> 0xC040FF;
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            int c = color(getStars());
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(FastColor.ARGB32.red(c) / 255.0F,
                    FastColor.ARGB32.green(c) / 255.0F, FastColor.ARGB32.blue(c) / 255.0F), 1.2F);
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(dust, getX() + Math.cos(a) * 1.2, getY() + 0.2 + random.nextDouble() * 3.0,
                        getZ() + Math.sin(a) * 1.2, 0, 0, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.PORTAL, getRandomX(1.0), getY() + random.nextDouble() * 3.2, getRandomZ(1.0), 0, 0, 0);
            }
            return;
        }
        if (lifetime > 0 && --lifetime == 0) {
            close();
            return;
        }
        if (!isExit() && tickCount % 100 == 0 && level() instanceof ServerLevel level && Gates.isCleared(level, dungeonId)
                && (lifetime < 0 || lifetime > CLOSE_AFTER_CLEAR)) {
            lifetime = CLOSE_AFTER_CLEAR;
        }
        if (tickCount % 80 == 0) {
            playSound(SoundEvents.PORTAL_AMBIENT, 0.4F, 0.6F + getStars() * 0.05F);
        }
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox())) {
            if (!player.isOnPortalCooldown() && !player.isSpectator()) {
                if (isExit()) {
                    Gates.exit(player);
                } else {
                    Gates.enter(this, player);
                }
            }
        }
    }

    /** Closes the gate with a burst of particles. */
    public void close() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 60, 0.6, 1.2, 0.6, 0.1);
            playSound(SoundEvents.BEACON_DEACTIVATE, 1.5F, 0.8F);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        dungeonId = tag.getInt("Dungeon");
        lifetime = tag.getInt("Lifetime");
        entityData.set(DATA_STARS, Math.max(1, tag.getInt("Stars")));
        entityData.set(DATA_EXIT, tag.getBoolean("Exit"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Dungeon", dungeonId);
        tag.putInt("Lifetime", lifetime);
        tag.putInt("Stars", getStars());
        tag.putBoolean("Exit", isExit());
    }
}
