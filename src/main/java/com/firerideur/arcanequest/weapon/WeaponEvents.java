package com.firerideur.arcanequest.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WeaponEvents {
    public static final float BACKSTAB_MULTIPLIER = 1.75F;

    private WeaponEvents() {}

    /** Dagger backstab: more damage when hitting a target from behind. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player) || event.getSource().getEntity() != player) {
            return;
        }
        if (player.getMainHandItem().getItem() instanceof WeaponItem weapon && weapon.getType() == WeaponType.DAGGER
                && isBehind(player, event.getEntity())) {
            event.setAmount(event.getAmount() * BACKSTAB_MULTIPLIER);
            if (player.level() instanceof ServerLevel level) {
                LivingEntity target = event.getEntity();
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            }
        }
    }

    public static boolean isBehind(Player attacker, LivingEntity target) {
        Vec3 facing = Vec3.directionFromRotation(0, target.getYHeadRot());
        Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        return facing.dot(toAttacker) < -0.3;
    }
}
