package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.client.ui.BarTrail;
import com.firerideur.arcanequest.client.ui.UiKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces every boss bar with the Arcane Quest style: ornate, framed bars with a damage trail.
 * Champions (yellow bars) get a compact version so several of them don't fill the screen.
 */
public final class BossBars {
    private static final int BOSS_WIDTH = 182, BOSS_HEIGHT = 6, BOSS_SPACING = 24;
    private static final int CHAMPION_WIDTH = 110, CHAMPION_HEIGHT = 3, CHAMPION_SPACING = 15;

    private static final Map<UUID, BarTrail> TRAILS = new HashMap<>();

    private BossBars() {}

    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        event.setCanceled(true);
        LerpingBossEvent boss = event.getBossEvent();
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int center = event.getX() + 91;
        float progress = boss.getProgress();
        float trail = TRAILS.computeIfAbsent(boss.getId(), id -> new BarTrail()).update(progress);

        if (boss.getColor() == BossEvent.BossBarColor.YELLOW) {
            // Champion: small gold name and thin bar
            int y = event.getY();
            UiKit.barText(g, font, boss.getName().getString(), center, y - 6, 0.75F);
            UiKit.bar(g, center - CHAMPION_WIDTH / 2, y + 2, CHAMPION_WIDTH, CHAMPION_HEIGHT, progress, trail, 0xFFD9A030);
            event.setIncrement(CHAMPION_SPACING);
            return;
        }

        int y = event.getY() + 2;
        int left = center - BOSS_WIDTH / 2;
        // Name plate: dark backing with diamonds on each side
        int nameWidth = font.width(boss.getName());
        g.fill(center - nameWidth / 2 - 14, y - 11, center + nameWidth / 2 + 14, y - 1, 0xB0100E0C);
        g.blit(UiKit.ATLAS, center - nameWidth / 2 - 11, y - 10, 118, 0, 7, 7);
        g.blit(UiKit.ATLAS, center + nameWidth / 2 + 4, y - 10, 118, 0, 7, 7);
        g.drawCenteredString(font, boss.getName(), center, y - 10, UiKit.GOLD_LIGHT);
        UiKit.bar(g, left, y + 1, BOSS_WIDTH, BOSS_HEIGHT, progress, trail, colorOf(boss.getColor()));
        // Ornate caps at both ends
        g.blit(UiKit.ATLAS, left - 6, y + 1, 118, 0, 7, 7);
        UiKit.barText(g, font, Math.round(progress * 100) + "%", center, y + 2, 0.5F);
        event.setIncrement(BOSS_SPACING);
    }

    private static int colorOf(BossEvent.BossBarColor color) {
        return switch (color) {
            case PURPLE -> 0xFF8A3FD0;
            case WHITE -> 0xFFD8D0C0;
            case BLUE -> 0xFF2F7FD8;
            case GREEN -> 0xFF3FA34D;
            case PINK -> 0xFFE070B0;
            default -> 0xFFC0282D;
        };
    }

    public static void clear() {
        TRAILS.clear();
    }
}
