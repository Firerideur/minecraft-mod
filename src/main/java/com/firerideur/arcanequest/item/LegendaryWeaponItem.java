package com.firerideur.arcanequest.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Boss weapons with an on-hit effect. */
public class LegendaryWeaponItem extends SwordItem {
    public enum Effect {
        /** Skeleton King's blade: withers the target. */
        WITHER,
        /** Adamantite Golem's hammer: shockwave damaging enemies around the target. */
        SHOCKWAVE
    }

    private static final float SHOCKWAVE_DAMAGE = 6.0F;
    private static final double SHOCKWAVE_RADIUS = 3.0;

    private final Effect effect;

    public LegendaryWeaponItem(Tier tier, Effect effect, Properties properties) {
        super(tier, properties);
        this.effect = effect;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        switch (effect) {
            case WITHER -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), attacker);
            case SHOCKWAVE -> shockwave(target, attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private static void shockwave(LivingEntity target, LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.5F, 1.4F);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SHOCKWAVE_RADIUS),
                e -> e != attacker && e != target && e.isAlive() && !(e instanceof Player && attacker instanceof Player))) {
            victim.hurt(attacker.damageSources().mobAttack(attacker), SHOCKWAVE_DAMAGE);
            victim.knockback(0.6, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.arcanequest.legendary." + effect.name().toLowerCase())
                .withStyle(ChatFormatting.GOLD));
    }
}
