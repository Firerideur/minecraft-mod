package com.firerideur.arcanequest.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.function.Function;

/**
 * Giant caverns: a huge, flat-floored chamber joined by a couple of very wide tunnels, like the caverns of Better Caves.
 * The vanilla tunnel and room shapes are reused, only much bigger and flatter.
 */
public class CavernCarver extends CaveWorldCarver {
    public CavernCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean carve(CarvingContext context, CaveCarverConfiguration config, ChunkAccess chunk,
                         Function<BlockPos, Holder<Biome>> biomes, RandomSource random, Aquifer aquifer, ChunkPos start,
                         CarvingMask mask) {
        int length = SectionPos.sectionToBlockCoord(getRange() * 2 - 1);
        double x = start.getBlockX(random.nextInt(16));
        double y = config.y.sample(random, context);
        double z = start.getBlockZ(random.nextInt(16));
        double horizontal = config.horizontalRadiusMultiplier.sample(random);
        double vertical = config.verticalRadiusMultiplier.sample(random);
        // Flat floors (the config's floor_level isn't visible outside its package)
        double floor = -0.75 + random.nextDouble() * 0.3;
        WorldCarver.CarveSkipChecker skip = (ctx, dx, dy, dz, py) -> dy <= floor || dx * dx + dy * dy + dz * dz >= 1.0;

        // The chamber: 18 to 34 blocks wide, about half as tall
        float radius = 8.0F + random.nextFloat() * 8.0F;
        createRoom(context, config, chunk, biomes, aquifer, x, y, z, radius, 0.45 + random.nextDouble() * 0.2, mask, skip);
        int tunnels = 1 + random.nextInt(3);
        for (int i = 0; i < tunnels; i++) {
            float yaw = random.nextFloat() * (float) (Math.PI * 2);
            float pitch = (random.nextFloat() - 0.5F) / 6.0F;
            createTunnel(context, config, chunk, biomes, random.nextLong(), aquifer, x, y, z, horizontal, vertical,
                    getThickness(random), yaw, pitch, 0, length - random.nextInt(length / 4), getYScale(), mask, skip);
        }
        return true;
    }

    @Override
    protected float getThickness(RandomSource random) {
        return 3.0F + random.nextFloat() * 3.0F;
    }

    @Override
    protected double getYScale() {
        return 0.7;
    }
}
