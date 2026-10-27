package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.fusion.FusionForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class FusionForgeScreen extends AbstractContainerScreen<FusionForgeMenu> {
    private static final int ARROW_X = 76, ARROW_Y = 35;
    private static final int CHARGE_X = 28, CHARGE_Y = 33, CHARGE_W = 5, CHARGE_H = 20;

    public FusionForgeScreen(FusionForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(CHARGE_X, CHARGE_Y, CHARGE_W, CHARGE_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("container.arcanequest.fusion_forge.charge",
                    menu.charge(), menu.maxCharge()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        UiKit.panel(g, leftPos, topPos, imageWidth, imageHeight);
        UiKit.title(g, font, title, leftPos + 50, topPos + 4, imageWidth - 58);
        UiKit.title(g, font, playerInventoryTitle, leftPos + 8, topPos + 70, imageWidth - 16);
        for (Slot slot : menu.slots) {
            UiKit.slot(g, leftPos + slot.x - 1, topPos + slot.y - 1, slot == hoveredSlot);
        }
        // Bigger frame around the result
        UiKit.frame(g, leftPos + FusionForgeMenu.RESULT_X - 4, topPos + FusionForgeMenu.RESULT_Y - 4, 24, 24, UiKit.GOLD);
        // "+" between the two ingredients
        int px = leftPos + FusionForgeMenu.A_X + 8, py = topPos + 43;
        g.fill(px - 3, py, px + 4, py + 1, UiKit.GOLD);
        g.fill(px, py - 3, px + 1, py + 4, UiKit.GOLD);
        UiKit.progressArrow(g, leftPos + ARROW_X, topPos + ARROW_Y, menu.progress());
        // Catalyst gauge
        int gx = leftPos + CHARGE_X, gy = topPos + CHARGE_Y;
        g.fill(gx, gy, gx + CHARGE_W, gy + CHARGE_H, 0xFF0A0908);
        int filled = Math.round((float) menu.charge() / menu.maxCharge() * (CHARGE_H - 2));
        if (filled > 0) {
            g.fillGradient(gx + 1, gy + CHARGE_H - 1 - filled, gx + CHARGE_W - 1, gy + CHARGE_H - 1, 0xFFD08CFF, 0xFF6A2A9A);
        }
        UiKit.frame(g, gx - 1, gy - 1, CHARGE_W + 2, CHARGE_H + 2, UiKit.GOLD);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Titles are drawn in renderBg
    }
}
