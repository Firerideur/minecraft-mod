package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.jobs.Job;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.gametest.GameTestHolder;

/** Jobs: leveling, experience from work, perks. */
@GameTestHolder(ArcaneQuest.MODID)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only mock player helper in 1.21.1
public class JobGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    @GameTest(template = EMPTY)
    public static void jobsLevelUpAndAreSaved(GameTestHelper helper) {
        PlayerStats stats = new PlayerStats();
        helper.assertTrue(stats.getJobLevel(Job.MINER) == 1, "jobs start at level 1");
        int gained = stats.addJobXp(Job.MINER, Job.xpForNextLevel(1) + Job.xpForNextLevel(2) + 5);
        helper.assertTrue(gained == 2 && stats.getJobLevel(Job.MINER) == 3 && stats.getJobXp(Job.MINER) == 5,
                "two levels and 5 xp left, got level " + stats.getJobLevel(Job.MINER));
        stats.addJobXp(Job.FISHERMAN, 100_000_000L);
        helper.assertTrue(stats.getJobLevel(Job.FISHERMAN) == Job.MAX_LEVEL, "capped at " + Job.MAX_LEVEL);
        PlayerStats loaded = new PlayerStats();
        loaded.load(stats.save());
        helper.assertTrue(loaded.getJobLevel(Job.MINER) == 3 && loaded.getJobXp(Job.MINER) == 5
                && loaded.getJobLevel(Job.FISHERMAN) == Job.MAX_LEVEL, "jobs should be saved");
        helper.assertTrue(Job.MINER.speedBonus(50) > Job.MINER.speedBonus(10), "bonuses grow with the level");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void miningOresTeachesTheMiner(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.DIAMOND_ORE);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertTrue(stats.getJobXp(Job.MINER) == 14, "a diamond ore gives 14 miner xp, got " + stats.getJobXp(Job.MINER));
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void farmersReplant(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        StatsCapability.get(player).orElseThrow().setJobLevel(Job.FARMER, Job.FARMER.milestone(0));
        BlockPos crop = new BlockPos(1, 2, 1);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        player.gameMode.destroyBlock(helper.absolutePos(crop));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.WHEAT, crop);
            helper.assertTrue(((CropBlock) Blocks.WHEAT).getAge(helper.getBlockState(crop)) == 0, "replanted as a young crop");
        });
    }
}
