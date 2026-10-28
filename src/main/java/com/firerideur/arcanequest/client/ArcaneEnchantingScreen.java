package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.enchant.ArcaneEnchanting;
import com.firerideur.arcanequest.enchant.ArcaneEnchantingMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

/** Arcane Enchanting Table: the table's stats on the left, the three choices on the right. */
public class ArcaneEnchantingScreen extends AbstractContainerScreen<ArcaneEnchantingMenu> {
    private static final int OPTION_X = 60, OPTION_Y = 18, OPTION_W = 108, OPTION_H = 20, OPTION_GAP = 22;

    public ArcaneEnchantingScreen(ArcaneEnchantingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        for (int i = 0; i < 3; i++) {
            if (inOption(i, mouseX, mouseY) && menu.costs[i] > 0) {
                g.renderComponentTooltip(font, optionTooltip(i), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        UiKit.panel(g, leftPos, topPos, imageWidth, imageHeight);
        UiKit.title(g, font, title, leftPos + 8, topPos + 5, imageWidth - 16);
        for (Slot slot : menu.slots) {
            UiKit.slot(g, leftPos + slot.x - 1, topPos + slot.y - 1, slot == hoveredSlot);
        }
        UiKit.title(g, font, playerInventoryTitle, leftPos + 8, topPos + ArcaneEnchantingMenu.INVENTORY_Y - 13, imageWidth - 16);
        renderStats(g);
        for (int i = 0; i < 3; i++) {
            renderOption(g, i, mouseX, mouseY);
        }
    }

    /** Power, chaos and arcana as small bars, and the highest level the table reaches. */
    private void renderStats(GuiGraphics g) {
        float power = menu.stats[0] / 10.0F;
        int x = leftPos + 8, y = topPos + 20;
        stat(g, Component.translatable("screen.arcanequest.enchanting.power"), String.format("%.1f", power),
                power / ArcaneEnchanting.MAX_POWER, 0xFF7A4FD0, x, y);
        stat(g, Component.translatable("screen.arcanequest.enchanting.chaos"), menu.stats[1] + "%", menu.stats[1] / 100.0F,
                0xFFD04F4F, x, y + 12);
        stat(g, Component.translatable("screen.arcanequest.enchanting.arcana"), menu.stats[2] + "%", menu.stats[2] / 100.0F,
                0xFF4FA8D0, x, y + 24);
        g.pose().pushPose();
        g.pose().translate(x, topPos + 70, 0);
        g.pose().scale(0.7F, 0.7F, 1);
        g.drawString(font, Component.translatable("screen.arcanequest.enchanting.max_level", Math.round(power * 2)), 0, 0, UiKit.GOLD_LIGHT, false);
        g.pose().popPose();
    }

    private void stat(GuiGraphics g, Component label, String value, float progress, int color, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y - 6, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        g.drawString(font, label.copy().append(" " + value), 0, 0, UiKit.TEXT, false);
        g.pose().popPose();
        UiKit.bar(g, x, y + 1, 40, 2, progress, color);
    }

    private void renderOption(GuiGraphics g, int i, int mouseX, int mouseY) {
        int x = leftPos + OPTION_X, y = topPos + OPTION_Y + i * OPTION_GAP;
        int cost = menu.costs[i];
        boolean active = cost > 0 && canAfford(i);
        UiKit.button(g, font, Component.empty(), x, y, OPTION_W, OPTION_H, inOption(i, mouseX, mouseY), active);
        if (cost <= 0) {
            return;
        }
        // Lapis needed, as small blue dots, then the level cost on the right
        for (int dot = 0; dot <= i; dot++) {
            g.fill(x + 4, y + 4 + dot * 5, x + 7, y + 7 + dot * 5, active ? 0xFF3C6FE0 : 0xFF333A55);
        }
        String level = String.valueOf(cost);
        g.drawString(font, level, x + OPTION_W - 4 - font.width(level), y + 6, active ? 0xFF80FF20 : 0xFF407F10, true);
        Component clue = clue(i);
        if (clue != null) {
            int room = OPTION_W - 16 - font.width(level) - 6;
            g.pose().pushPose();
            g.pose().translate(x + 11, y + 7, 0);
            float scale = Math.min(0.8F, room / (float) Math.max(1, font.width(clue)));
            g.pose().scale(scale, scale, 1);
            g.drawString(font, clue, 0, 0, active ? UiKit.TEXT : UiKit.TEXT_DIM, false);
            g.pose().popPose();
        }
    }

    /** "Sharpness VII…": the first enchantment of the choice; the others stay a surprise. */
    private Component clue(int i) {
        if (menu.clueEnchantment[i] < 0 || minecraft == null || minecraft.level == null) {
            return null;
        }
        Holder<Enchantment> enchantment = minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .asHolderIdMap().byId(menu.clueEnchantment[i]);
        return enchantment == null ? null : Enchantment.getFullname(enchantment, menu.clueLevel[i]).copy().append("…");
    }

    private List<Component> optionTooltip(int i) {
        List<Component> lines = new ArrayList<>();
        Component clue = clue(i);
        if (clue != null) {
            lines.add(clue.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        lines.add(Component.translatable("screen.arcanequest.enchanting.requires", menu.costs[i]).withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("screen.arcanequest.enchanting.consumes", i + 1).withStyle(ChatFormatting.GRAY));
        if (!canAfford(i)) {
            lines.add(Component.translatable("screen.arcanequest.enchanting.cannot").withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    private boolean canAfford(int i) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (minecraft.player.hasInfiniteMaterials()) {
            return true;
        }
        return minecraft.player.experienceLevel >= menu.costs[i] && menu.lapisCount() >= i + 1;
    }

    private boolean inOption(int i, double mouseX, double mouseY) {
        int x = leftPos + OPTION_X, y = topPos + OPTION_Y + i * OPTION_GAP;
        return mouseX >= x && mouseX < x + OPTION_W && mouseY >= y && mouseY < y + OPTION_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 3; i++) {
            if (inOption(i, mouseX, mouseY) && menu.costs[i] > 0 && canAfford(i) && minecraft != null && minecraft.gameMode != null
                    && menu.clickMenuButton(minecraft.player, i)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Titles are drawn in renderBg
    }
}
