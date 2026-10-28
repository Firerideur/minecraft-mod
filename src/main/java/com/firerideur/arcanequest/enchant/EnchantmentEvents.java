package com.firerideur.arcanequest.enchant;

import com.firerideur.arcanequest.boss.ArcaneBoss;
import com.firerideur.arcanequest.dungeon.DungeonMob;
import com.firerideur.arcanequest.item.HammerItem;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** What the enchantments that aren't plain data do. */
public final class EnchantmentEvents {
    /** Second Wind can save its wearer once every 5 minutes. */
    static final int SECOND_WIND_COOLDOWN = 6000;
    private static final String SECOND_WIND_TAG = "arcanequest_second_wind";
    private static final String ARROW_TAG = "arcanequest_bow";

    /** Set while Vein Miner, Timber or Cleave act, so their own breaking and hits don't trigger them again. */
    private static boolean busy;
    /** Soulbound items of players who just died, given back when they respawn. */
    private static final Map<UUID, List<ItemStack>> SOULBOUND = new HashMap<>();

    private EnchantmentEvents() {}

    private static int level(LivingEntity entity, ItemStack stack, ResourceKey<Enchantment> key) {
        return ModEnchantments.level(entity.level(), stack, key);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Weapons
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof AbstractArrow arrow && arrow.getPersistentData().contains(ARROW_TAG)) {
            CompoundTag bow = arrow.getPersistentData().getCompound(ARROW_TAG);
            int sniper = bow.getInt("Sniper");
            if (sniper > 0) {
                // Up to +25% per level for a shot from 40 blocks away
                double distance = new Vec3(bow.getDouble("X"), bow.getDouble("Y"), bow.getDouble("Z")).distanceTo(target.position());
                event.setAmount(event.getAmount() * (1.0F + 0.25F * sniper * (float) Math.min(1.0, distance / 40.0)));
            }
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || direct != attacker || busy) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        float amount = event.getAmount();
        int executioner = level(attacker, weapon, ModEnchantments.EXECUTIONER);
        if (executioner > 0 && target.getHealth() < target.getMaxHealth() * 0.35F) {
            amount *= 1.0F + 0.12F * executioner;
        }
        int giantSlayer = level(attacker, weapon, ModEnchantments.GIANT_SLAYER);
        if (giantSlayer > 0) {
            if (target instanceof ArcaneBoss || target instanceof DungeonMob mob && mob.isGateBoss()) {
                amount *= 1.0F + 0.15F * giantSlayer;
            } else if (target.getMaxHealth() > attacker.getMaxHealth()) {
                amount *= 1.0F + 0.08F * giantSlayer;
            }
        }
        ServerLevel level = (ServerLevel) target.level();
        int critical = level(attacker, weapon, ModEnchantments.CRITICAL);
        if (critical > 0 && attacker.getRandom().nextFloat() < 0.05F * critical) {
            amount *= 1.6F;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.4, 0.3, 0.3);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        int thunder = level(attacker, weapon, ModEnchantments.THUNDERSTRIKE);
        if (thunder > 0 && attacker.getRandom().nextFloat() < 0.06F * thunder) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                // Only the light and the sound: the bolt itself would hit the wielder too
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            amount += 3.0F * thunder;
            target.igniteForSeconds(3);
        }
        int bleeding = level(attacker, weapon, ModEnchantments.BLEEDING);
        if (bleeding > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.getHolder().orElseThrow(), 60 + 20 * bleeding, bleeding - 1), attacker);
        }
        event.setAmount(amount);

        int cleave = level(attacker, weapon, ModEnchantments.CLEAVE);
        if (cleave > 0 && attacker instanceof Player player) {
            busy = true;
            try {
                for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0),
                        e -> e != target && e != attacker && e.isAlive() && !e.isAlliedTo(attacker) && !(e instanceof Player))) {
                    other.hurt(level.damageSources().playerAttack(player), amount * 0.2F * cleave);
                }
            } finally {
                busy = false;
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && event.getSource().getDirectEntity() == attacker
                && !attacker.level().isClientSide()) {
            int lifesteal = level(attacker, attacker.getMainHandItem(), ModEnchantments.LIFESTEAL);
            if (lifesteal > 0 && event.getAmount() > 0) {
                attacker.heal(event.getAmount() * 0.05F * lifesteal);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Armor
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.getSource().getEntity() == null) {
            return;
        }
        int evasion = ModEnchantments.level(target, ModEnchantments.EVASION);
        if (evasion > 0 && target.getRandom().nextFloat() < 0.04F * evasion) {
            event.setCanceled(true);
            ((ServerLevel) target.level()).sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof ServerPlayer player && ModEnchantments.level(player, ModEnchantments.SECOND_WIND) > 0) {
            long now = player.level().getGameTime();
            long last = player.getPersistentData().getLong(SECOND_WIND_TAG);
            if (last == 0 || now - last >= SECOND_WIND_COOLDOWN) {
                event.setCanceled(true);
                player.getPersistentData().putLong(SECOND_WIND_TAG, now);
                player.setHealth(player.getMaxHealth() * 0.25F);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 3));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 30, 0.5, 0.8, 0.5, 0.3);
                player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
                player.sendSystemMessage(Component.translatable("message.arcanequest.second_wind").withStyle(ChatFormatting.GOLD));
                return;
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            int harvest = level(killer, killer.getMainHandItem(), ModEnchantments.SOUL_HARVEST);
            if (harvest > 0) {
                StatsCapability.get(killer).ifPresent(stats -> {
                    stats.setMana(Math.min(Mana.max(killer, stats), stats.getMana() + 4.0F * harvest));
                    ModNetwork.sync(killer);
                });
                killer.serverLevel().sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY(0.5), entity.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        int rejuvenation = ModEnchantments.level(player, ModEnchantments.REJUVENATION);
        if (rejuvenation > 0 && player.tickCount % 100 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(rejuvenation);
        }
        if (ModEnchantments.level(player, ModEnchantments.NIGHT_SIGHT) > 0) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Loot and experience
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Soulbound items stay with their owner
            List<ItemStack> kept = new ArrayList<>();
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemStack stack = it.next().getItem();
                if (ModEnchantments.level(player.level(), stack, ModEnchantments.SOULBOUND) > 0) {
                    kept.add(stack.copy());
                    it.remove();
                }
            }
            if (!kept.isEmpty()) {
                SOULBOUND.put(player.getUUID(), kept);
            }
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer
                && level(killer, killer.getMainHandItem(), ModEnchantments.TELEKINESIS) > 0) {
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemStack stack = it.next().getItem();
                killer.getInventory().add(stack);
                if (stack.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            List<ItemStack> kept = SOULBOUND.remove(event.getOriginal().getUUID());
            if (kept != null) {
                kept.forEach(stack -> event.getEntity().getInventory().add(stack));
            }
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player != null) {
            int wisdom = level(player, player.getMainHandItem(), ModEnchantments.WISDOM);
            if (wisdom > 0) {
                event.setDroppedExperience(Math.round(event.getDroppedExperience() * (1.0F + 0.25F * wisdom)));
            }
        }
    }

    /** Spell power multiplier of the staff in hand (Spell Power), for staff spells and magic skills alike. */
    public static float spellPowerBonus(Player player) {
        return 1.0F + 0.1F * level(player, player.getMainHandItem(), ModEnchantments.SPELL_POWER);
    }

    /** Mana cost multiplier of the staff in hand (Mana Efficiency). */
    public static float manaCostBonus(Player player) {
        return 1.0F - 0.1F * level(player, player.getMainHandItem(), ModEnchantments.MANA_EFFICIENCY);
    }

    /** Multiplier of the class experience of a kill (Wisdom on the weapon). */
    public static float wisdomBonus(Player player) {
        return 1.0F + 0.25F * level(player, player.getMainHandItem(), ModEnchantments.WISDOM);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tools
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || busy) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        int wisdom = level(player, tool, ModEnchantments.WISDOM);
        if (wisdom > 0 && event.getExpToDrop() > 0) {
            event.setExpToDrop(Math.round(event.getExpToDrop() * (1.0F + 0.25F * wisdom)));
        }
        if (player.isShiftKeyDown() || tool.getItem() instanceof HammerItem) {
            return;
        }
        BlockState state = event.getState();
        int vein = level(player, tool, ModEnchantments.VEIN_MINER);
        if (vein > 0 && state.is(Tags.Blocks.ORES) && tool.isCorrectToolForDrops(state)) {
            breakAll(player, connected(player.level(), event.getPos(), s -> s.is(state.getBlock()), 8 << vein), event.getPos());
        } else if (level(player, tool, ModEnchantments.TIMBER) > 0 && state.is(BlockTags.LOGS)) {
            List<BlockPos> logs = connected(player.level(), event.getPos(), s -> s.is(BlockTags.LOGS), 160);
            if (isTree(player.level(), logs)) {
                breakAll(player, logs, event.getPos());
            }
        }
    }

    /** Blocks matching {@code accept} touching {@code start} (diagonals too), at most {@code limit}, closest first. */
    static List<BlockPos> connected(Level level, BlockPos start, Predicate<BlockState> accept, int limit) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockPos immutable = next.immutable();
                if (seen.add(immutable) && accept.test(level.getBlockState(immutable))) {
                    found.add(immutable);
                    queue.add(immutable);
                    if (found.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return found;
    }

    /** Logs with natural leaves next to them: a tree, not a house. */
    private static boolean isTree(Level level, List<BlockPos> logs) {
        for (BlockPos log : logs) {
            for (BlockPos near : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(near);
                if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void breakAll(ServerPlayer player, List<BlockPos> blocks, BlockPos origin) {
        busy = true;
        try {
            for (BlockPos pos : blocks) {
                if (player.getMainHandItem().isEmpty()) {
                    break; // the tool broke
                }
                if (!pos.equals(origin)) {
                    player.gameMode.destroyBlock(pos);
                }
            }
        } finally {
            busy = false;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Bows
    // ------------------------------------------------------------------------------------------------------------

    /** Arrows remember the enchantments of the bow that shot them, and where they were shot from. */
    @SubscribeEvent
    public static void onArrowShot(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof LivingEntity shooter) || arrow.getPersistentData().contains(ARROW_TAG)) {
            return;
        }
        ItemStack bow = shooter.getMainHandItem();
        if (!(bow.getItem() instanceof BowItem || bow.getItem() instanceof CrossbowItem)) {
            bow = shooter.getOffhandItem();
        }
        int explosive = level(shooter, bow, ModEnchantments.EXPLOSIVE_ARROWS);
        int sniper = level(shooter, bow, ModEnchantments.SNIPER);
        if (explosive > 0 || sniper > 0) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Explosive", explosive);
            tag.putInt("Sniper", sniper);
            tag.putDouble("X", shooter.getX());
            tag.putDouble("Y", shooter.getY());
            tag.putDouble("Z", shooter.getZ());
            arrow.getPersistentData().put(ARROW_TAG, tag);
        }
    }

    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide()) {
            return;
        }
        CompoundTag tag = arrow.getPersistentData().getCompound(ARROW_TAG);
        int explosive = tag.getInt("Explosive");
        if (explosive > 0) {
            tag.putInt("Explosive", 0);
            Vec3 at = event.getRayTraceResult().getLocation();
            // Hurts what is around, never the terrain
            arrow.level().explode(arrow, at.x, at.y, at.z, 1.0F + 0.6F * explosive, Level.ExplosionInteraction.NONE);
        }
    }
}
