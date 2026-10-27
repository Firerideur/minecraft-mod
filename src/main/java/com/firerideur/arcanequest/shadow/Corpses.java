package com.firerideur.arcanequest.shadow;

import com.firerideur.arcanequest.boss.ArcaneBoss;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Monsters recently killed by a player (or their shadows), which Shadow Extraction ("Arise") can still raise,
 * like the corpses Sung Jinwoo extracts shadows from.
 */
public final class Corpses {
    /** How long a corpse can be extracted, in ticks. */
    public static final int LIFETIME = 1200;
    /** A corpse that resisted this many extractions is lost. */
    public static final int MAX_ATTEMPTS = 3;
    private static final int MAX = 64;

    /** A dead monster: what its shadow will be made from. */
    public record Corpse(UUID killer, ResourceKey<Level> dimension, Vec3 position, Component name, float maxHealth,
                         boolean boss, long time, int attempts) {
        public ShadowSoldier.Rank rank() {
            return ShadowSoldier.Rank.of(maxHealth, boss);
        }
    }

    private static final List<Corpse> CORPSES = new ArrayList<>();

    private Corpses() {}

    /** Remembers {@code victim} if it is a monster (shadows can't be raised again). */
    public static void add(ServerPlayer killer, LivingEntity victim) {
        if (!(victim instanceof Enemy) || victim instanceof ShadowSoldier) {
            return;
        }
        CORPSES.add(new Corpse(killer.getUUID(), victim.level().dimension(), victim.position(), victim.getDisplayName(),
                victim.getMaxHealth(), victim instanceof ArcaneBoss, victim.level().getGameTime(), 0));
        if (CORPSES.size() > MAX) {
            CORPSES.remove(0);
        }
    }

    /**
     * Takes the corpses of {@code player}'s kills within {@code radius} blocks, closest first; they can't be taken again.
     */
    public static List<Corpse> take(ServerPlayer player, double radius, int max) {
        long now = player.level().getGameTime();
        List<Corpse> found = new ArrayList<>();
        for (Iterator<Corpse> it = CORPSES.iterator(); it.hasNext(); ) {
            Corpse corpse = it.next();
            if (now - corpse.time > LIFETIME || now < corpse.time) {
                it.remove();
            } else if (corpse.killer.equals(player.getUUID()) && corpse.dimension == player.level().dimension()
                    && corpse.position.distanceTo(player.position()) <= radius) {
                found.add(corpse);
            }
        }
        found.sort(Comparator.comparingDouble(c -> c.position.distanceToSqr(player.position())));
        List<Corpse> taken = found.subList(0, Math.min(max, found.size()));
        CORPSES.removeAll(taken);
        return new ArrayList<>(taken);
    }

    /** Puts back a corpse that resisted the extraction, unless it already resisted {@link #MAX_ATTEMPTS} times. */
    public static void retry(Corpse corpse) {
        if (corpse.attempts + 1 < MAX_ATTEMPTS) {
            CORPSES.add(new Corpse(corpse.killer, corpse.dimension, corpse.position, corpse.name, corpse.maxHealth,
                    corpse.boss, corpse.time, corpse.attempts + 1));
        }
    }

    /** Number of corpses {@code player} could extract right now. */
    public static long count(ServerPlayer player, double radius) {
        long now = player.level().getGameTime();
        return CORPSES.stream().filter(c -> now - c.time <= LIFETIME && c.killer.equals(player.getUUID())
                && c.dimension == player.level().dimension() && c.position.distanceTo(player.position()) <= radius).count();
    }

    public static void clear() {
        CORPSES.clear();
    }
}
