package com.firerideur.arcanequest.weapon;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.registry.ModTiers;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.List;
import java.util.function.Consumer;

/** Dagger, katana, longsword, spear and scythe: each with its own mechanic and animation. */
public class WeaponItem extends SwordItem {
    private static final ResourceLocation REACH_ID = ArcaneQuest.id("weapon_reach");

    public static final double KATANA_DASH_DISTANCE = 6.0;
    public static final int KATANA_DASH_COOLDOWN = 60;
    public static final float CLEAVE_RATIO = 0.6F;
    public static final double CLEAVE_RANGE = 3.5;
    public static final float SCYTHE_RATIO = 0.5F;
    public static final double SCYTHE_RANGE = 3.0;
    public static final float SCYTHE_LIFESTEAL = 0.1F;
    public static final float ARCANIUM_MANA_ON_HIT = 2.0F;

    private final WeaponType type;

    public WeaponItem(Tier tier, WeaponType type, Properties properties) {
        super(tier, properties.attributes(createAttributes(tier, type)));
        this.type = type;
    }

    public static ItemAttributeModifiers createAttributes(Tier tier, WeaponType type) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        type.damage() + tier.getAttackDamageBonus(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        type.speed(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (type.reach() != 0) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_ID,
                    type.reach(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    public WeaponType getType() {
        return type;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        if (action == ToolActions.SWORD_SWEEP) {
            return type == WeaponType.KATANA || type == WeaponType.LONGSWORD;
        }
        return super.canPerformAction(stack, action);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            switch (type) {
                case LONGSWORD -> cleave(player, target, damage * CLEAVE_RATIO);
                case SCYTHE -> {
                    int hits = sweepAround(player, target, damage * SCYTHE_RATIO);
                    player.heal(damage * SCYTHE_LIFESTEAL * (1 + hits));
                }
                default -> {
                }
            }
            if (getTier() == ModTiers.ARCANIUM && player instanceof ServerPlayer serverPlayer) {
                StatsCapability.get(serverPlayer).ifPresent(stats -> {
                    stats.setMana(Math.min(Mana.max(serverPlayer, stats), stats.getMana() + ARCANIUM_MANA_ON_HIT));
                    ModNetwork.sync(serverPlayer);
                });
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** Longsword: hits enemies in a cone in front of the player. */
    private static void cleave(Player player, LivingEntity target, float damage) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        for (LivingEntity other : nearby(player, target, CLEAVE_RANGE)) {
            Vec3 to = other.position().subtract(player.position()).multiply(1, 0, 1).normalize();
            if (look.dot(to) > 0.5) {
                other.hurt(player.damageSources().playerAttack(player), damage);
                other.knockback(0.4, player.getX() - other.getX(), player.getZ() - other.getZ());
            }
        }
        if (player.level() instanceof ServerLevel level) {
            Vec3 pos = player.position().add(look.scale(1.5));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, player.getY(0.5), pos.z, 1, 0, 0, 0, 0);
        }
    }

    /** Scythe: hits every enemy around the player. */
    private static int sweepAround(Player player, LivingEntity target, float damage) {
        List<LivingEntity> victims = nearby(player, target, SCYTHE_RANGE);
        for (LivingEntity other : victims) {
            other.hurt(player.damageSources().playerAttack(player), damage);
        }
        if (player.level() instanceof ServerLevel level) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(angle) * 2,
                        player.getY(0.5), player.getZ() + Math.sin(angle) * 2, 1, 0, 0, 0, 0);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        return victims.size();
    }

    private static List<LivingEntity> nearby(Player player, LivingEntity target, double range) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, 1.0, range),
                e -> e != player && e != target && e.isAlive() && !e.isAlliedTo(player) && !(e instanceof Player)
                        && player.distanceToSqr(e) <= range * range);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (type != WeaponType.KATANA) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide()) {
            dash(player);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, KATANA_DASH_COOLDOWN);
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Katana: dash forward, slashing every enemy on the path. */
    private static void dash(Player player) {
        Level level = player.level();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 start = player.position().add(0, 0.5, 0);
        Vec3 end = level.clip(new ClipContext(start, start.add(look.scale(KATANA_DASH_DISTANCE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        double distance = Math.max(0, start.distanceTo(end) - 0.6);

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB path = player.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, path,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
            victim.hurt(player.damageSources().playerAttack(player), damage);
        }
        player.setDeltaMovement(look.scale(Math.min(distance, KATANA_DASH_DISTANCE) * 0.3).add(0, 0.1, 0));
        player.hurtMarked = true;
        player.resetFallDistance();
        if (level instanceof ServerLevel serverLevel) {
            for (double d = 0; d < distance; d += 1.0) {
                Vec3 p = start.add(look.scale(d));
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(type.description().copy().withStyle(ChatFormatting.GRAY));
        if (getTier() == ModTiers.ARCANIUM) {
            tooltip.add(Component.translatable("tooltip.arcanequest.arcanium").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // Called from the Item constructor, before this.type is set: the extension reads the type later.
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new WeaponClientExtensions(this));
    }
}
