package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.worldgen.CaveDecoratorFeature;
import com.firerideur.arcanequest.worldgen.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Optional;

/** Cave regions and underground lakes. */
@GameTestHolder(ArcaneQuest.MODID)
public class CaveGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    @GameTest(template = EMPTY)
    public static void caveRegionsAreStableAndMagmaStaysDeep(GameTestHelper helper) {
        RandomSource random = RandomSource.create(7);
        boolean sawMagma = false;
        for (int i = 0; i < 4000; i++) {
            int x = random.nextInt(20000) - 10000, z = random.nextInt(20000) - 10000;
            CaveDecoratorFeature.Theme upper = CaveDecoratorFeature.themeAt(1234L, x, 10, z);
            helper.assertTrue(upper != CaveDecoratorFeature.Theme.MAGMA, "magma caves must stay below y = 0");
            helper.assertTrue(upper == CaveDecoratorFeature.themeAt(1234L, x, 50, z), "a region is the same at every height of its layer");
            sawMagma |= CaveDecoratorFeature.themeAt(1234L, x, -30, z) == CaveDecoratorFeature.Theme.MAGMA;
        }
        helper.assertTrue(sawMagma, "magma caves should exist below y = 0");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void undergroundLakeFillsAWatertightFloor(GameTestHelper helper) {
        // High above the test grid so the stone block doesn't touch other tests
        BlockPos origin = helper.absolutePos(new BlockPos(1, 120, 1));
        ServerLevel level = helper.getLevel();
        fill(level, origin.offset(-9, -6, -9), origin.offset(9, 0, 9), true);
        helper.assertTrue(placeLake(level, origin), "a lake should be dug into a solid floor");
        helper.assertTrue(level.getBlockState(origin).is(Blocks.WATER) || level.getBlockState(origin).is(Blocks.LAVA),
                "the middle of the floor should be water");
        fill(level, origin.offset(-9, -6, -9), origin.offset(9, 0, 9), false);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void undergroundLakeNeverLeaks(GameTestHelper helper) {
        // A pillar too narrow for any basin: the lake must not be placed at all
        BlockPos origin = helper.absolutePos(new BlockPos(1, 150, 1));
        ServerLevel level = helper.getLevel();
        fill(level, origin.offset(-2, -6, -2), origin.offset(2, 0, 2), true);
        helper.assertFalse(placeLake(level, origin), "a lake without solid ground around it would flood the cave");
        helper.assertTrue(level.getBlockState(origin).is(Blocks.STONE), "nothing should have been dug");
        fill(level, origin.offset(-2, -6, -2), origin.offset(2, 0, 2), false);
        helper.succeed();
    }

    private static boolean placeLake(ServerLevel level, BlockPos floor) {
        return ModWorldgen.UNDERGROUND_LAKE.get().place(new FeaturePlaceContext<>(Optional.empty(), level,
                level.getChunkSource().getGenerator(), RandomSource.create(42), floor, NoneFeatureConfiguration.INSTANCE));
    }

    private static void fill(ServerLevel level, BlockPos from, BlockPos to, boolean stone) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            level.setBlock(pos, (stone ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
        }
    }
}
