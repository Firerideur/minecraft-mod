package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.stats.PlayerStats;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Top-left medallion with health, mana and XP bars, and the compass at the top-right. */
public final class ArcaneHud {
    private static final int BAR_HEIGHT = 4;
    private static final int COMPASS_WIDTH = 140;
    private static final float COMPASS_FOV = 120.0F;
    private static final String[] DIRECTIONS = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};

    private ArcaneHud() {}

    private static boolean hidden(Minecraft mc) {
        return mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen();
    }

    public static void renderStatus(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (hidden(mc)) {
            return;
        }
        LocalPlayer player = mc.player;
        PlayerStats stats = ClientStats.get();
        int x = 4;
        int y = 4;

        UiKit.medallion(g, x, y);
        PlayerFaceRenderer.draw(g, player.getSkin(), x + 12, y + 12, 16);
        // Level badge under the medallion
        String level = String.valueOf(stats.getLevel());
        int badgeW = mc.font.width(level) + 8;
        int badgeX = x + UiKit.MEDALLION_SIZE / 2 - badgeW / 2;
        g.fill(badgeX, y + 33, badgeX + badgeW, y + 44, 0xFF14110E);
        UiKit.frame(g, badgeX, y + 33, badgeW, 11, UiKit.GOLD);
        g.drawCenteredString(mc.font, level, x + UiKit.MEDALLION_SIZE / 2, y + 35, UiKit.GOLD_LIGHT);
        int barX = x + UiKit.MEDALLION_SIZE + 4;
        float maxMana = Mana.max(player, stats);
        float xp = stats.isMaxLevel() ? 1 : (float) stats.getXp() / PlayerStats.xpForNextLevel(stats.getLevel());
        // Short enough to stay left of the boss bars, even on small screens
        UiKit.bar(g, barX, y + 9, 70, BAR_HEIGHT, player.getHealth() / player.getMaxHealth(), 0xFFC0282D);
        UiKit.bar(g, barX, y + 19, 58, BAR_HEIGHT, stats.getMana() / maxMana, 0xFF2F7FD8);
        UiKit.bar(g, barX, y + 29, 46, BAR_HEIGHT, xp, 0xFF3FA34D);
        if (stats.getPoints() > 0) {
            // Unspent stat points, next to the XP bar
            g.drawString(mc.font, Component.translatable("hud.arcanequest.points", stats.getPoints()),
                    barX + 54, y + 27, 0xFF7CFC00);
        }
    }

    /** Horizontal compass strip: letters slide as the player turns. */
    public static void renderCompass(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (hidden(mc)) {
            return;
        }
        float yaw = Mth.wrapDegrees(mc.player.getViewYRot(deltaTracker.getGameTimeDeltaPartialTick(true)));
        int center = g.guiWidth() - COMPASS_WIDTH / 2 - 8;
        int y = 10;
        UiKit.rule(g, center - COMPASS_WIDTH / 2, y + 4, COMPASS_WIDTH);
        for (int angle = 0; angle < 360; angle += 15) {
            float delta = Mth.wrapDegrees(angle - yaw);
            if (Math.abs(delta) > COMPASS_FOV / 2) {
                continue;
            }
            int px = center + Math.round(delta / (COMPASS_FOV / 2) * (COMPASS_WIDTH / 2.0F));
            int alpha = (int) (255 * (1 - Math.abs(delta) / (COMPASS_FOV / 2)) * 0.8 + 50);
            if (angle % 45 == 0) {
                // Minecraft yaw: 0 = south, 90 = west, 180 = north, 270 = east
                Component letter = Component.translatable("hud.arcanequest.compass." + DIRECTIONS[angle / 45]);
                boolean cardinal = angle % 90 == 0;
                int color = (alpha << 24) | (cardinal ? 0xF0D28C : 0xC9A25A);
                g.drawCenteredString(mc.font, letter, px, y - 6, color);
            } else {
                g.fill(px, y + 2, px + 1, y + 7, (alpha << 24) | 0xC9A25A);
            }
        }
        g.blit(UiKit.ATLAS, center - 3, y + 6, 118, 0, 7, 7);
    }
}
