package com.firerideur.arcanequest.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashSet;
import java.util.Set;

/**
 * A lake dug into a cave floor: an oval basin filled with water (sometimes lava deep down), with clay and gravel at
 * the bottom. Only placed when the basin is watertight, so nothing floods. Placed on a cave floor (the origin).
 */
public class UndergroundLakeFeature extends Feature<NoneFeatureConfiguration> {
    public UndergroundLakeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos floor = context.origin();
        if (!level.isEmptyBlock(floor.above()) || level.getBlockState(floor).isAir()) {
            return false;
        }
        boolean lava = floor.getY() < -20 && random.nextInt(4) == 0;
        int radiusX = 3 + random.nextInt(4), radiusZ = 3 + random.nextInt(4), maxDepth = 2 + random.nextInt(2);

        Set<BlockPos> basin = new HashSet<>();
        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                double edge = 1.0 - ((double) dx * dx / (radiusX * radiusX) + (double) dz * dz / (radiusZ * radiusZ));
                if (edge <= 0) {
                    continue;
                }
                int depth = 1 + (int) (edge * maxDepth);
                for (int dy = 0; dy < depth; dy++) {
                    basin.add(floor.offset(dx, -dy, dz));
                }
            }
        }
        // Watertight: every water block replaces solid ground and is surrounded by solid ground or water
        for (BlockPos pos : basin) {
            if (!solid(level.getBlockState(pos))) {
                return false;
            }
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (side != Direction.UP && !basin.contains(next) && !solid(level.getBlockState(next))) {
                    return false;
                }
            }
        }
        BlockState fluid = (lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState();
        for (BlockPos pos : basin) {
            level.setBlock(pos, fluid, 2);
            BlockPos below = pos.below();
            if (!basin.contains(below)) {
                BlockState bottom = lava ? (random.nextBoolean() ? Blocks.MAGMA_BLOCK : Blocks.BASALT).defaultBlockState()
                        : (random.nextInt(3) == 0 ? Blocks.GRAVEL : Blocks.CLAY).defaultBlockState();
                level.setBlock(below, bottom, 2);
            }
        }
        return true;
    }

    private static boolean solid(BlockState state) {
        return state.blocksMotion() && state.getFluidState().isEmpty();
    }
}
