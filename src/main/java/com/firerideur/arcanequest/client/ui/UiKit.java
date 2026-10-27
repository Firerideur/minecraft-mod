package com.firerideur.arcanequest.client.ui;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Dark fantasy UI style shared by every Arcane Quest screen: charcoal panels with thin gold trims,
 * ornate corners, engraved slots and gold titles framed by small diamonds.
 */
public final class UiKit {
    public static final ResourceLocation ATLAS = ArcaneQuest.id("textures/gui/arcane_ui.png");

    public static final int GOLD = 0xFFC9A25A;
    public static final int GOLD_LIGHT = 0xFFF0D28C;
    public static final int GOLD_DARK = 0xFF6E5228;
    public static final int TEXT = 0xFFE8DCC4;
    public static final int TEXT_DIM = 0xFF9A8F7C;

    public static final int SLOT_SIZE = 18;
    public static final int TAB_WIDTH = 96;
    public static final int TAB_HEIGHT = 24;
    public static final int MEDALLION_SIZE = 40;

    private UiKit() {}

    /** Panel with tiled charcoal background, double gold trim and ornate corners. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        for (int ty = 0; ty < h; ty += 64) {
            for (int tx = 0; tx < w; tx += 64) {
                g.blit(ATLAS, x + tx, y + ty, 0, 0, Math.min(64, w - tx), Math.min(64, h - ty));
            }
        }
        g.fill(x, y, x + w, y + 1, 0xFF000000);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
        g.fill(x, y, x + 1, y + h, 0xFF000000);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF000000);
        frame(g, x + 2, y + 2, w - 4, h - 4, GOLD_DARK);
        frame(g, x + 4, y + 4, w - 8, h - 8, 0x40C9A25A);
        g.blit(ATLAS, x + 1, y + 1, 100, 0, 9, 9);
        g.blit(ATLAS, x + w - 10, y + 1, 109, 0, 9, 9);
        g.blit(ATLAS, x + 1, y + h - 10, 100, 9, 9, 9);
        g.blit(ATLAS, x + w - 10, y + h - 10, 109, 9, 9, 9);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** "◆ Title ◆" in gold, with a thin gold rule under it spanning {@code width}. */
    public static void title(GuiGraphics g, Font font, Component text, int x, int y, int width) {
        g.blit(ATLAS, x, y, 118, 0, 7, 7);
        g.drawString(font, text, x + 10, y, GOLD_LIGHT, false);
        int end = x + 10 + font.width(text) + 3;
        g.blit(ATLAS, end, y, 118, 0, 7, 7);
        rule(g, x, y + 11, width);
    }

    /** Thin gold line that fades at both ends, with a diamond in the middle of each end. */
    public static void rule(GuiGraphics g, int x, int y, int width) {
        // fillGradient is vertical only, so fade horizontally in small steps
        int half = Math.max(1, width / 2);
        for (int i = 0; i < width; i += 2) {
            int alpha = 0x20 + (int) (0xDF * (1 - Math.abs(i - half) / (float) half));
            g.fill(x + i, y, Math.min(x + width, x + i + 2), y + 1, (alpha << 24) | 0xC9A25A);
        }
        g.fill(x + 2, y, x + 3, y + 1, GOLD_LIGHT);
        g.fill(x + width - 3, y, x + width - 2, y + 1, GOLD_LIGHT);
    }

    /** Slot frame drawn around an item position (items are drawn at x+1, y+1). */
    public static void slot(GuiGraphics g, int x, int y, boolean highlighted) {
        g.blit(ATLAS, x, y, highlighted ? 82 : 64, 0, SLOT_SIZE, SLOT_SIZE);
    }

    public static void arrow(GuiGraphics g, int x, int y) {
        g.blit(ATLAS, x, y, 128, 0, 24, 15);
    }

    /** Horizontal arrow filled up to {@code progress} (0..1). */
    public static void progressArrow(GuiGraphics g, int x, int y, float progress) {
        g.setColor(0.3F, 0.3F, 0.3F, 1.0F);
        arrow(g, x, y);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int filled = Math.round(24 * Math.max(0, Math.min(1, progress)));
        if (filled > 0) {
            g.blit(ATLAS, x, y, 128, 0, filled, 15);
        }
    }

    public static void medallion(GuiGraphics g, int x, int y) {
        g.blit(ATLAS, x, y, 0, 64, MEDALLION_SIZE, MEDALLION_SIZE);
    }

    public static void selectedTab(GuiGraphics g, int x, int y) {
        g.blit(ATLAS, x, y, 0, 104, TAB_WIDTH, TAB_HEIGHT);
        g.blit(ATLAS, x + TAB_WIDTH - 4, y + TAB_HEIGHT / 2 - 3, 118, 0, 7, 7);
    }

    /**
     * Ornate resource bar: black groove, colored fill with a lighter top line, gold frame and diamond end caps.
     */
    public static void bar(GuiGraphics g, int x, int y, int width, int height, float progress, int color) {
        bar(g, x, y, width, height, progress, progress, color);
    }

    /** Same, with a pale "damage trail" up to {@code trail} behind the fill (see {@link BarTrail}). */
    public static void bar(GuiGraphics g, int x, int y, int width, int height, float progress, float trail, int color) {
        float p = Math.max(0, Math.min(1, progress));
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, GOLD_DARK);
        g.fill(x, y, x + width, y + height, 0xFF0A0908);
        int trailEnd = Math.round(width * Math.max(0, Math.min(1, trail)));
        if (trailEnd > 0) {
            g.fill(x, y, x + trailEnd, y + height, 0xFFE8D8A8);
        }
        int filled = Math.round(width * p);
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + height, brighten(color), darken(color));
            g.fill(x, y, x + filled, y + 1, 0x60FFFFFF);
        }
        frame(g, x - 2, y - 2, width + 4, height + 4, GOLD);
        g.blit(ATLAS, x + width - 1, y + height / 2 - 3, 118, 0, 7, 7);
    }

    /** Small text scaled down to fit inside a bar, centered. */
    public static void barText(GuiGraphics g, Font font, String text, int centerX, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(centerX, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    /** Dark button with gold border and gold text; brighter when hovered, dimmed when inactive. */
    public static void button(GuiGraphics g, Font font, Component text, int x, int y, int w, int h,
                              boolean hovered, boolean active) {
        g.fill(x, y, x + w, y + h, hovered && active ? 0xFF3A2E1C : 0xFF1A1714);
        frame(g, x, y, w, h, !active ? 0xFF4A4036 : hovered ? GOLD_LIGHT : GOLD);
        int color = !active ? TEXT_DIM : hovered ? GOLD_LIGHT : TEXT;
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, color);
    }

    /** Key hint like "[Échap] Fermer" for the footer. */
    public static int keyHint(GuiGraphics g, Font font, String key, Component action, int x, int y) {
        int keyWidth = font.width(key) + 6;
        g.fill(x, y - 2, x + keyWidth, y + 9, 0xFF1A1714);
        frame(g, x, y - 2, keyWidth, 11, GOLD_DARK);
        g.drawString(font, key, x + 3, y, GOLD_LIGHT, false);
        g.drawString(font, action, x + keyWidth + 4, y, TEXT_DIM, false);
        return x + keyWidth + 4 + font.width(action) + 12;
    }

    private static int brighten(int color) {
        return scale(color, 1.25F);
    }

    private static int darken(int color) {
        return scale(color, 0.6F);
    }

    private static int scale(int color, float factor) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int gr = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }
}
