package com.firerideur.arcanequest.jobs;

import com.firerideur.arcanequest.boss.ArcaneBoss;
import com.firerideur.arcanequest.client.ClientStats;
import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.registry.ModBlocks;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatsCapability;
import com.firerideur.arcanequest.stats.StatsEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/** How jobs gain experience, and what their levels give. Double harvests of blocks are in {@link JobLootModifier}. */
public final class JobEvents {
    private JobEvents() {}

    /** The player's stats on either side (the client keeps its own synced copy). */
    static PlayerStats stats(Player player) {
        return player.level().isClientSide() ? ClientStats.get() : StatsCapability.get(player).orElse(null);
    }

    public static int level(Player player, Job job) {
        PlayerStats stats = stats(player);
        return stats == null ? 1 : stats.getJobLevel(job);
    }

    public static boolean hasPerk(Player player, Job job, int perk) {
        return job.hasPerk(level(player, job), perk);
    }

    /** Job experience: a small message each time, a title and the job's new bonus on each level. */
    public static void gain(ServerPlayer player, Job job, long amount) {
        if (amount <= 0) {
            return;
        }
        PlayerStats stats = StatsCapability.get(player).orElse(null);
        if (stats == null || stats.getJobLevel(job) >= Job.MAX_LEVEL) {
            return;
        }
        int before = stats.getJobLevel(job);
        int gained = stats.addJobXp(job, amount);
        int level = stats.getJobLevel(job);
        player.displayClientMessage(Component.literal("+" + amount + " XP ").withStyle(job.color())
                .append(job.displayName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(level >= Job.MAX_LEVEL ? "  ★" : "  " + stats.getJobXp(job) + " / " + Job.xpForNextLevel(level))
                        .withStyle(ChatFormatting.DARK_GRAY)), true);
        if (gained > 0) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.arcanequest.job_level_sub")
                    .withStyle(ChatFormatting.GRAY)));
            player.sendSystemMessage(Component.translatable("message.arcanequest.job_level", job.displayName(), level)
                    .withStyle(job.color()).append(" ").append(job.bonus(level).copy().withStyle(ChatFormatting.GRAY)));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.arcanequest.job_level",
                    job.displayName(), level).withStyle(job.color())));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
            for (int perk = 0; perk < job.perkCount(); perk++) {
                if (before < job.milestone(perk) && level >= job.milestone(perk)) {
                    player.sendSystemMessage(Component.translatable("message.arcanequest.job_perk", job.displayName(), job.perk(perk))
                            .withStyle(ChatFormatting.GOLD));
                }
            }
            // A new job level also counts toward the character's level
            for (int l = before + 1; l <= level; l++) {
                StatsEvents.addXp(player, 20L * l);
            }
        }
        ModNetwork.sync(player);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Miner, lumberjack, farmer: breaking blocks
    // ------------------------------------------------------------------------------------------------------------

    /** Job experience for mining an ore (more for the rare ones), 0 for anything else. */
    static int oreXp(BlockState state) {
        Block block = state.getBlock();
        int xp;
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE || block == Blocks.COPPER_ORE
                || block == Blocks.DEEPSLATE_COPPER_ORE || block == Blocks.NETHER_QUARTZ_ORE || block == Blocks.NETHER_GOLD_ORE) {
            xp = 3;
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            xp = 4;
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE || block == Blocks.LAPIS_ORE
                || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            xp = 5;
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            xp = 7;
        } else if (block == ModBlocks.MITHRIL_ORE.get() || block == ModBlocks.DEEPSLATE_MITHRIL_ORE.get()) {
            xp = 8;
        } else if (block == ModBlocks.ARCANITE_ORE.get() || block == ModBlocks.DEEPSLATE_ARCANITE_ORE.get()) {
            xp = 12;
        } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            xp = 14;
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            xp = 18;
        } else if (block == ModBlocks.ADAMANTITE_ORE.get()) {
            xp = 20;
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            xp = 25;
        } else {
            xp = state.is(Tags.Blocks.ORES) ? 4 : 0;
        }
        return xp;
    }

    static boolean isStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Blocks.END_STONE);
    }

    /** A crop ready to be harvested (the only crops that teach a farmer anything). */
    static boolean isRipeCrop(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CropBlock crop && crop.isMaxAge(state)
                || block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3
                || block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= 2
                || block == Blocks.MELON || block == Blocks.PUMPKIN;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        int ore = oreXp(state);
        if (ore > 0) {
            gain(player, Job.MINER, ore);
            if (hasPerk(player, Job.MINER, 1) && event.getExpToDrop() > 0) {
                event.setExpToDrop(Math.round(event.getExpToDrop() * 1.5F));
            }
        } else if (isStone(state) && player.getRandom().nextFloat() < 0.35F) {
            gain(player, Job.MINER, 1);
        } else if (state.is(BlockTags.LOGS)) {
            gain(player, Job.LUMBERJACK, 2);
            if (hasPerk(player, Job.LUMBERJACK, 1)) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 0, true, false));
            }
        } else if (isRipeCrop(state)) {
            gain(player, Job.FARMER, state.getBlock() instanceof NetherWartBlock || state.getBlock() instanceof CocoaBlock ? 3 : 2);
        }
    }

    /** Miners and lumberjacks work faster. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        Job job = state.is(BlockTags.LOGS) ? Job.LUMBERJACK
                : state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? Job.MINER : null;
        if (job != null) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + job.speedBonus(level(player, job))));
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            gain(player, Job.FARMER, 6);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hunter
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && !(event.getEntity() instanceof Player)) {
            event.setAmount(event.getAmount() * (1.0F + Job.HUNTER.speedBonus(level(player, Job.HUNTER))));
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || victim instanceof Player) {
            return;
        }
        long xp = Math.max(1, Math.round(victim.getMaxHealth() / 3.0 + MobLevels.getLevel(victim) / 2.0));
        if (victim instanceof ArcaneBoss) {
            xp *= 3;
        }
        gain(player, Job.HUNTER, xp);
        if (hasPerk(player, Job.HUNTER, 2)) {
            player.heal(2.0F);
        }
    }

    /** Hunters sometimes get the loot twice, and later emeralds. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() instanceof Player) {
            return;
        }
        LivingEntity victim = event.getEntity();
        int level = level(player, Job.HUNTER);
        if (player.getRandom().nextFloat() < Job.HUNTER.doubleChance(level)) {
            List<ItemEntity> copies = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                copies.add(new ItemEntity(victim.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
            event.getDrops().addAll(copies);
        }
        if (Job.HUNTER.hasPerk(level, 1) && player.getRandom().nextFloat() < 0.05F) {
            event.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.EMERALD)));
        }
    }

    /** Hunter perk: +25% class experience from kills. */
    public static float classXpBonus(Player player) {
        return hasPerk(player, Job.HUNTER, 0) ? 1.25F : 1.0F;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fisherman
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int level = level(player, Job.FISHERMAN);
        long xp = 0;
        for (ItemStack stack : event.getDrops()) {
            xp += stack.is(ItemTags.FISHES) ? 8 : 15;
        }
        List<ItemStack> extra = new ArrayList<>();
        if (player.getRandom().nextFloat() < Job.FISHERMAN.doubleChance(level)) {
            event.getDrops().forEach(stack -> extra.add(stack.copy()));
        }
        if (Job.FISHERMAN.hasPerk(level, 0) && player.getRandom().nextFloat() < 0.05F) {
            extra.add(new ItemStack(switch (player.getRandom().nextInt(4)) {
                case 0 -> Items.NAME_TAG;
                case 1 -> Items.SADDLE;
                case 2 -> Items.NAUTILUS_SHELL;
                default -> Items.EXPERIENCE_BOTTLE;
            }));
        }
        if (Job.FISHERMAN.hasPerk(level, 2) && player.getRandom().nextFloat() < 0.01F) {
            extra.add(new ItemStack(Items.HEART_OF_THE_SEA));
        }
        for (ItemStack stack : extra) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        if (Job.FISHERMAN.hasPerk(level, 1)) {
            event.damageRodBy(0);
        }
        gain(player, Job.FISHERMAN, xp);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Miner perk: seeing in the dark deep down
    // ------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player instanceof ServerPlayer player && player.tickCount % 40 == 0 && player.getY() < 0
                && hasPerk(player, Job.MINER, 0)) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
            }
        }
    }
}
