package com.firerideur.arcanequest.jobs;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * What jobs add to block drops: double (and later triple) ores, logs and crops; apples for lumberjacks; farmers
 * replant what they harvest and sometimes make the crops around them grow.
 */
public class JobLootModifier extends LootModifier {
    public static final MapCodec<JobLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, JobLootModifier::new));

    public JobLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (state == null || origin == null || !(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player player)) {
            return loot;
        }
        RandomSource random = context.getRandom();
        if (JobEvents.oreXp(state) > 0) {
            multiply(loot, Job.MINER, JobEvents.level(player, Job.MINER), 2, random);
        } else if (state.is(BlockTags.LOGS)) {
            int level = JobEvents.level(player, Job.LUMBERJACK);
            multiply(loot, Job.LUMBERJACK, level, -1, random);
            if (Job.LUMBERJACK.hasPerk(level, 0) && random.nextFloat() < 0.08F) {
                loot.add(new ItemStack(Items.APPLE));
            }
            if (Job.LUMBERJACK.hasPerk(level, 2) && random.nextFloat() < 0.01F) {
                loot.add(new ItemStack(Items.GOLDEN_APPLE));
            }
        } else if (JobEvents.isRipeCrop(state)) {
            int level = JobEvents.level(player, Job.FARMER);
            BlockPos pos = BlockPos.containing(origin);
            if (Job.FARMER.hasPerk(level, 0)) {
                replant(context.getLevel(), pos, state, loot);
            }
            multiply(loot, Job.FARMER, level, 2, random);
            if (Job.FARMER.hasPerk(level, 1) && random.nextFloat() < 0.1F) {
                growAround(context.getLevel(), pos);
            }
        }
        return loot;
    }

    /**
     * Doubles the drops with the job's chance; past the job's perk {@code triplePerk} (if any), a third of the doubles
     * become triples.
     */
    private static void multiply(ObjectArrayList<ItemStack> loot, Job job, int level, int triplePerk, RandomSource random) {
        if (random.nextFloat() >= job.doubleChance(level)) {
            return;
        }
        int times = triplePerk >= 0 && job.hasPerk(level, triplePerk) && random.nextInt(3) == 0 ? 3 : 2;
        for (ItemStack stack : loot) {
            stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() * times));
        }
    }

    /** Plants the crop again, using one of its own seeds from the harvest. */
    private static void replant(ServerLevel level, BlockPos pos, BlockState harvested, ObjectArrayList<ItemStack> loot) {
        BlockState young;
        if (harvested.getBlock() instanceof CropBlock crop) {
            young = crop.getStateForAge(0);
        } else if (harvested.getBlock() instanceof NetherWartBlock) {
            young = harvested.setValue(NetherWartBlock.AGE, 0);
        } else if (harvested.getBlock() instanceof CocoaBlock) {
            young = harvested.setValue(CocoaBlock.AGE, 0);
        } else {
            return; // melons and pumpkins keep their stem
        }
        Item seed = harvested.getBlock().asItem();
        for (ItemStack stack : loot) {
            if (stack.is(seed)) {
                stack.shrink(1);
                loot.removeIf(ItemStack::isEmpty);
                // After the block is gone
                level.getServer().execute(() -> {
                    if (level.getBlockState(pos).isAir() && young.canSurvive(level, pos)) {
                        level.setBlock(pos, young, 3);
                    }
                });
                return;
            }
        }
    }

    /** The crops around grow by two stages. */
    private static void growAround(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                level.setBlock(pos, crop.getStateForAge(Math.min(crop.getMaxAge(), crop.getAge(state) + 2)), 2);
            }
        }
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
