package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.network.SpendPointPacket;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.Stat;
import com.firerideur.arcanequest.stats.StatEffects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/** Character sheet: level, experience and stat allocation. Opened with K by default. */
public class StatsScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 170;
    private static final int ROW_HEIGHT = 24;
    private static final int ROWS_TOP = 62;

    private final Map<Stat, Button> plusButtons = new EnumMap<>(Stat.class);
    private int left;
    private int top;

    public StatsScreen() {
        super(Component.translatable("screen.arcanequest.stats"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        plusButtons.clear();
        for (Stat stat : Stat.values()) {
            Button button = Button.builder(Component.literal("+"),
                            b -> ModNetwork.sendToServer(new SpendPointPacket(stat.ordinal())))
                    .bounds(left + PANEL_WIDTH - 30, top + ROWS_TOP + stat.ordinal() * ROW_HEIGHT - 4, 20, 20)
                    .tooltip(Tooltip.create(stat.description()))
                    .build();
            plusButtons.put(stat, addRenderableWidget(button));
        }
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        boolean canSpend = ClientStats.get().getPoints() > 0;
        plusButtons.values().forEach(button -> button.active = canSpend);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        PlayerStats stats = ClientStats.get();

        graphics.drawCenteredString(font, title, width / 2, top + 8, 0xFFD700);

        Component level = Component.translatable("screen.arcanequest.level", stats.getLevel());
        graphics.drawString(font, level, left + 12, top + 26, 0xFFFFFF);
        Component xp = stats.isMaxLevel()
                ? Component.translatable("screen.arcanequest.max_level")
                : Component.translatable("screen.arcanequest.xp", stats.getXp(), PlayerStats.xpForNextLevel(stats.getLevel()));
        graphics.drawString(font, xp, left + PANEL_WIDTH - 12 - font.width(xp), top + 26, 0xAAAAAA);
        ClientSetup.drawXpBar(graphics, left + 12, top + 38, PANEL_WIDTH - 24, 4, stats);

        Component points = Component.translatable("screen.arcanequest.points", stats.getPoints());
        graphics.drawString(font, points, left + 12, top + 48, stats.getPoints() > 0 ? 0x7CFC00 : 0xAAAAAA);

        for (Stat stat : Stat.values()) {
            int y = top + ROWS_TOP + stat.ordinal() * ROW_HEIGHT + 2;
            graphics.drawString(font, stat.displayName(), left + 12, y, stat.color());
            graphics.drawString(font, String.valueOf(stats.get(stat)), left + 90, y, 0xFFFFFF);
            graphics.drawString(font, bonus(stat, stats.get(stat)), left + 110, y, 0x888888);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF8B6914);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE1A1A2E);
    }

    private static Component bonus(Stat stat, int value) {
        return switch (stat) {
            case STRENGTH -> Component.translatable("screen.arcanequest.bonus.strength",
                    format(value * StatEffects.DAMAGE_PER_STRENGTH));
            case VITALITY -> Component.translatable("screen.arcanequest.bonus.vitality",
                    format(value * StatEffects.HEALTH_PER_VITALITY / 2.0));
            case AGILITY -> Component.translatable("screen.arcanequest.bonus.agility",
                    format(value * StatEffects.SPEED_PER_AGILITY * 100));
            case INTELLIGENCE -> Component.translatable("screen.arcanequest.bonus.intelligence");
        };
    }

    private static String format(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
