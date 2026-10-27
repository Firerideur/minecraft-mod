package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.client.ui.UiKit;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.tags.FluidTags;

/**
 * Live top-down map around the player, like a vanilla map but always centered on you.
 * Redrawn a few rows per tick; 1 pixel = 1 block, {@link #SIZE} blocks across.
 */
public final class WorldMap {
    public static final int SIZE = 128;
    private static final int ROWS_PER_TICK = 16;
    private static final ResourceLocation LOCATION = ArcaneQuest.id("dynamic/world_map");

    private static DynamicTexture texture;
    /** World coordinates of the map's top-left pixel for the pass being drawn / last finished. */
    private static int drawingX, drawingZ, shownX, shownZ;
    private static int nextRow;
    private static boolean shownSet;
    private static final int[] previousHeights = new int[SIZE];

    private WorldMap() {}

    private static DynamicTexture texture() {
        if (texture == null) {
            texture = new DynamicTexture(SIZE, SIZE, true);
            Minecraft.getInstance().getTextureManager().register(LOCATION, texture);
        }
        return texture;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return;
        }
        NativeImage pixels = texture().getPixels();
        if (pixels == null) {
            return;
        }
        if (nextRow == 0) {
            drawingX = Mth.floor(player.getX()) - SIZE / 2;
            drawingZ = Mth.floor(player.getZ()) - SIZE / 2;
            for (int col = 0; col < SIZE; col++) {
                previousHeights[col] = surfaceHeight(level, player, drawingX + col, drawingZ - 1);
            }
            if (!shownSet) {
                shownX = drawingX;
                shownZ = drawingZ;
                shownSet = true;
            }
        }
        for (int i = 0; i < ROWS_PER_TICK; i++, nextRow++) {
            drawRow(level, player, pixels, nextRow);
        }
        if (nextRow >= SIZE) {
            nextRow = 0;
            shownX = drawingX;
            shownZ = drawingZ;
        }
        texture.upload();
    }

    private static void drawRow(ClientLevel level, LocalPlayer player, NativeImage pixels, int row) {
        int z = drawingZ + row;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int col = 0; col < SIZE; col++) {
            int x = drawingX + col;
            if (!level.hasChunk(x >> 4, z >> 4)) {
                pixels.setPixelRGBA(col, row, 0xFF0E0C0A);
                previousHeights[col] = Integer.MIN_VALUE;
                continue;
            }
            int height = surfaceHeight(level, player, x, z);
            pos.set(x, height, z);
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            int color;
            if (fluid.is(FluidTags.WATER)) {
                // Deeper water is darker
                int depth = 0;
                while (depth < 12 && level.getFluidState(pos.move(0, -1, 0)).is(FluidTags.WATER)) {
                    depth++;
                }
                MapColor.Brightness b = depth < 2 ? MapColor.Brightness.HIGH : depth < 6 ? MapColor.Brightness.NORMAL : MapColor.Brightness.LOW;
                color = MapColor.WATER.calculateRGBColor(b);
            } else {
                MapColor mapColor = state.getMapColor(level, pos);
                int north = previousHeights[col];
                MapColor.Brightness b = north == Integer.MIN_VALUE || height == north ? MapColor.Brightness.NORMAL
                        : height > north ? MapColor.Brightness.HIGH : MapColor.Brightness.LOW;
                color = mapColor == MapColor.NONE ? 0xFF0E0C0A : mapColor.calculateRGBColor(b);
            }
            pixels.setPixelRGBA(col, row, color);
            previousHeights[col] = height;
        }
    }

    /** Top block of the column; under a ceiling (Nether) the first floor below the player. */
    private static int surfaceHeight(ClientLevel level, Player player, int x, int z) {
        if (level.dimensionType().hasCeiling()) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, Mth.floor(player.getY()) + 2, z);
            int min = level.getMinBuildHeight();
            while (pos.getY() > min && level.getBlockState(pos).isAir()) {
                pos.move(0, -1, 0);
            }
            return pos.getY();
        }
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    /**
     * Draws a square view of the map centered on the player, with mobs as dots and the player arrow.
     *
     * @param radius blocks shown from the center to each edge
     */
    public static void draw(GuiGraphics g, int x, int y, int size, int radius, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        texture();
        double px = Mth.lerp(partialTick, player.xOld, player.getX());
        double pz = Mth.lerp(partialTick, player.zOld, player.getZ());
        float u = (float) (px - shownX - radius);
        float v = (float) (pz - shownZ - radius);
        g.fill(x, y, x + size, y + size, 0xFF0E0C0A);
        g.enableScissor(x, y, x + size, y + size);
        g.blit(LOCATION, x, y, size, size, u, v, radius * 2, radius * 2, SIZE, SIZE);

        float scale = size / (radius * 2.0F);
        int cx = x + size / 2, cy = y + size / 2;
        if (mc.level != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == player || !(entity instanceof Enemy || entity instanceof Player)) {
                    continue;
                }
                double dx = entity.getX() - px, dz = entity.getZ() - pz;
                if (Math.abs(dx) > radius || Math.abs(dz) > radius) {
                    continue;
                }
                int ex = cx + (int) Math.round(dx * scale), ez = cy + (int) Math.round(dz * scale);
                int color = entity instanceof Player ? 0xFFFFFFFF : 0xFFE03030;
                g.fill(ex - 1, ez - 1, ex + 1, ez + 1, color);
            }
        }
        g.disableScissor();

        // Player arrow: the sprite points north, rotate it to the player's heading
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(player.getViewYRot(partialTick) - 180.0F));
        g.blit(UiKit.ATLAS, -4, -4, 152, 0, 9, 9);
        g.pose().popPose();
    }
}
