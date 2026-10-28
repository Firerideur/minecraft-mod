package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.boss.ArcaneBoss;
import com.firerideur.arcanequest.dungeon.DungeonMob;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.tags.FluidTags;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Live top-down map around the player, like a vanilla map but always centered on you.
 * Redrawn a few rows per tick; 1 pixel = 1 block, {@link #SIZE} blocks across.
 * Underground it switches to a cave view: the floor of the caves around the player's height, rock in black.
 */
public final class WorldMap {
    public static final int SIZE = 128;
    private static final int ROWS_PER_TICK = 16;
    private static final ResourceLocation LOCATION = ArcaneQuest.id("dynamic/world_map");
    /** Cave view: how far above and below the player's feet floors are looked for. */
    private static final int CAVE_UP = 2, CAVE_DOWN = 14;
    private static final int ROCK = 0xFF1C1916, UNKNOWN = 0xFF0E0C0A;
    /** Monsters drawn as small models (closest first); the others are dots. */
    private static final int MAX_MOB_ICONS = 24;
    private static final int ITEM_COLOR = 0xFFE02020, PLAYER_COLOR = 0xFFFFFFFF, MOB_COLOR = 0xFFB04040;

    private static DynamicTexture texture;
    /** World coordinates of the map's top-left pixel for the pass being drawn / last finished. */
    private static int drawingX, drawingZ, shownX, shownZ;
    private static int nextRow;
    private static boolean shownSet;
    /** Height of the player's feet for the cave pass being drawn, or {@code Integer.MIN_VALUE} at the surface. */
    private static int drawingCaveY = Integer.MIN_VALUE;
    private static boolean underground;
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
        boolean nowUnderground = isUnderground(level, player);
        if (nowUnderground != underground) {
            // Switch views at once instead of finishing a pass of the old one
            underground = nowUnderground;
            nextRow = 0;
        }
        if (nextRow == 0) {
            drawingCaveY = underground ? Mth.floor(player.getY()) : Integer.MIN_VALUE;
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
                pixels.setPixelRGBA(col, row, UNKNOWN);
                previousHeights[col] = Integer.MIN_VALUE;
                continue;
            }
            int height = surfaceHeight(level, player, x, z);
            if (height == Integer.MIN_VALUE) {
                pixels.setPixelRGBA(col, row, ROCK);
                previousHeights[col] = Integer.MIN_VALUE;
                continue;
            }
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
                color = mapColor == MapColor.NONE ? UNKNOWN : mapColor.calculateRGBColor(b);
                if (drawingCaveY != Integer.MIN_VALUE && height < drawingCaveY - 1) {
                    // Lower cave floors fade into the dark
                    color = darken(color, Math.max(0.35F, 1.0F - (drawingCaveY - 1 - height) * 0.06F));
                }
            }
            pixels.setPixelRGBA(col, row, color);
            previousHeights[col] = height;
        }
    }

    /** Whether the player is in a cave: well below the ground above them (trees and roofs don't count). */
    public static boolean isUnderground() {
        return underground;
    }

    private static boolean isUnderground(ClientLevel level, Player player) {
        if (level.dimensionType().hasCeiling()) {
            return false;
        }
        BlockPos feet = player.blockPosition();
        // WORLD_SURFACE is one of the heightmaps the server sends to clients
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, feet.getX(), feet.getZ());
        // Little sky light: under rock, not under a tree. Margins both ways so the view doesn't flicker at a cave mouth.
        int skyLight = level.getBrightness(LightLayer.SKY, feet.above());
        return feet.getY() < ground - (underground ? 2 : 4) && skyLight < (underground ? 10 : 7);
    }

    /**
     * Top block of the column; under a ceiling (Nether) the first floor below the player; in the cave view the floor
     * of the open space near the player's height, or {@code Integer.MIN_VALUE} for solid rock.
     */
    private static int surfaceHeight(ClientLevel level, Player player, int x, int z) {
        if (drawingCaveY != Integer.MIN_VALUE) {
            return caveFloor(level, x, z, drawingCaveY);
        }
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

    private static int caveFloor(ClientLevel level, int x, int z, int feetY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, feetY + CAVE_UP, z);
        boolean open = false;
        for (int y = feetY + CAVE_UP; y >= feetY - CAVE_DOWN && y >= level.getMinBuildHeight(); y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            // Air, torches, grass... are open space; fluids are shown as the floor (water, lava)
            if (state.getFluidState().isEmpty() && !state.blocksMotion()) {
                open = true;
            } else if (open) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static int darken(int abgr, float factor) {
        int r = (int) ((abgr & 0xFF) * factor), gr = (int) (((abgr >> 8) & 0xFF) * factor), b = (int) (((abgr >> 16) & 0xFF) * factor);
        return (abgr & 0xFF000000) | (b << 16) | (gr << 8) | r;
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
            List<LivingEntity> mobs = new ArrayList<>();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == player) {
                    continue;
                }
                double dx = entity.getX() - px, dz = entity.getZ() - pz;
                if (Math.abs(dx) > radius || Math.abs(dz) > radius) {
                    continue;
                }
                int ex = cx + (int) Math.round(dx * scale), ez = cy + (int) Math.round(dz * scale);
                if (entity instanceof ItemEntity) {
                    g.fill(ex - 1, ez - 1, ex + 1, ez + 1, ITEM_COLOR);
                } else if (entity instanceof Player) {
                    g.fill(ex - 1, ez - 1, ex + 1, ez + 1, PLAYER_COLOR);
                } else if (entity instanceof Enemy && entity instanceof LivingEntity living && living.isAlive()
                        && !(entity instanceof DungeonMob mob && mob.isDormant())) {
                    mobs.add(living);
                }
            }
            // Farthest first so the closest monsters are drawn on top
            mobs.sort(Comparator.comparingDouble(e -> -e.distanceToSqr(px, e.getY(), pz)));
            int icon = Mth.clamp(size / 8, 8, 12);
            for (int i = 0; i < mobs.size(); i++) {
                LivingEntity mob = mobs.get(i);
                int ex = cx + (int) Math.round((mob.getX() - px) * scale), ez = cy + (int) Math.round((mob.getZ() - pz) * scale);
                if (mobs.size() - i > MAX_MOB_ICONS) {
                    g.fill(ex - 1, ez - 1, ex + 1, ez + 1, MOB_COLOR);
                } else {
                    drawMobIcon(g, mob, ex, ez, mob instanceof ArcaneBoss ? icon * 3 / 2 : icon);
                }
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

    /** A tiny model of {@code mob} facing the viewer, centered on (x, y). */
    private static void drawMobIcon(GuiGraphics g, LivingEntity mob, int x, int y, int height) {
        float bodyRot = mob.yBodyRot, bodyRotO = mob.yBodyRotO, yRot = mob.getYRot(), xRot = mob.getXRot();
        float headRot = mob.yHeadRot, headRotO = mob.yHeadRotO;
        mob.yBodyRot = mob.yBodyRotO = 200.0F;
        mob.setYRot(200.0F);
        mob.setXRot(0.0F);
        mob.yHeadRot = mob.yHeadRotO = 200.0F;
        float scale = height / Math.max(mob.getBbHeight(), mob.getBbWidth() * 0.8F);
        g.pose().pushPose();
        g.pose().translate(0, 0, 100);
        InventoryScreen.renderEntityInInventory(g, x, y, scale, new Vector3f(0, mob.getBbHeight() / 2.0F, 0),
                new Quaternionf().rotateZ((float) Math.PI), null, mob);
        g.pose().popPose();
        mob.yBodyRot = bodyRot;
        mob.yBodyRotO = bodyRotO;
        mob.setYRot(yRot);
        mob.setXRot(xRot);
        mob.yHeadRot = headRot;
        mob.yHeadRotO = headRotO;
    }
}
